package com.harsh.dual.engine

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import com.harsh.dual.data.SpaceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Work Profile implementation of [VirtualizationEngine]. Uses Android's managed
 * profile — the same no-root mechanism as Island / Shelter. The private space is
 * a real, OS-managed profile; clones are real installs isolated inside it.
 */
class WorkProfileEngine(context: Context) : VirtualizationEngine {

    private val app = context.applicationContext
    private val dpm = app.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val launcher = app.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    /** The work profile's UserHandle, or null when no space exists. */
    private fun workUser(): UserHandle? {
        val me = Process.myUserHandle()
        return launcher.profiles.firstOrNull { it != me }
    }

    override fun spaceState(): SpaceState {
        if (workUser() != null) return SpaceState.READY
        val allowed = runCatching {
            dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
        }.getOrDefault(false)
        return if (allowed) SpaceState.NOT_CREATED else SpaceState.UNAVAILABLE
    }

    override fun createSpaceIntent(): Intent? {
        if (spaceState() != SpaceState.NOT_CREATED) return null
        return Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                DualAdminReceiver.componentName(app),
            )
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
        }
    }

    private fun isInWorkProfile(pkg: String): Boolean {
        val user = workUser() ?: return false
        return runCatching { launcher.getActivityList(pkg, user).isNotEmpty() }
            .getOrDefault(false)
    }

    override suspend fun cloneApp(packageName: String): EngineResult = withContext(Dispatchers.IO) {
        if (workUser() == null) return@withContext EngineResult.Failure("Private space is not set up yet.")
        if (isInWorkProfile(packageName)) return@withContext EngineResult.Success

        runCatching {
            app.startActivity(AgentActivity.opIntent(AgentActivity.OP_CLONE, packageName))
        }.onFailure {
            return@withContext EngineResult.Failure("Could not reach the private space. Reopen the app after the space finishes setting up.")
        }

        // The clone happens in the work profile; confirm by re-scanning.
        if (awaitPresence(packageName, present = true)) EngineResult.Success
        else EngineResult.Failure(
            "The clone did not appear. This app may not allow being installed into a second space.",
        )
    }

    override suspend fun removeClone(packageName: String): EngineResult = withContext(Dispatchers.IO) {
        if (!isInWorkProfile(packageName)) return@withContext EngineResult.Success
        runCatching { app.startActivity(AgentActivity.opIntent(AgentActivity.OP_REMOVE, packageName)) }
            .onFailure { return@withContext EngineResult.Failure("Could not reach the private space.") }
        if (awaitPresence(packageName, present = false)) EngineResult.Success
        else EngineResult.Failure("The clone could not be removed automatically. Confirm the removal prompt on your phone.")
    }

    override fun launchClone(packageName: String): EngineResult {
        val user = workUser() ?: return EngineResult.Failure("Private space is not available.")
        val activity = runCatching { launcher.getActivityList(packageName, user).firstOrNull() }.getOrNull()
            ?: return EngineResult.Failure("This clone has no launchable screen.")
        return runCatching {
            launcher.startMainActivity(activity.componentName, user, null, null)
            EngineResult.Success
        }.getOrElse { EngineResult.Failure("Could not launch the clone.") }
    }

    override suspend fun removeSpace(): EngineResult = withContext(Dispatchers.IO) {
        if (workUser() == null) return@withContext EngineResult.Success
        runCatching { app.startActivity(AgentActivity.opIntent(AgentActivity.OP_WIPE)) }
            .onFailure { return@withContext EngineResult.Failure("Could not remove the space automatically.") }
        // wipeData tears the profile down; profiles list will empty out.
        if (awaitNoWorkProfile()) EngineResult.Success
        else EngineResult.Failure("The space is being removed. It may take a few seconds to disappear.")
    }

    private suspend fun awaitPresence(pkg: String, present: Boolean): Boolean {
        repeat(20) {
            if (isInWorkProfile(pkg) == present) return true
            delay(500)
        }
        return isInWorkProfile(pkg) == present
    }

    private suspend fun awaitNoWorkProfile(): Boolean {
        repeat(20) {
            if (workUser() == null) return true
            delay(500)
        }
        return workUser() == null
    }
}
