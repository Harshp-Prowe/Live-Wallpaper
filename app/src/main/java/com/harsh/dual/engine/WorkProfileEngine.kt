package com.harsh.dual.engine

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import com.harsh.dual.data.SpaceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Work Profile implementation of [VirtualizationEngine]. Real, no-root isolation
 * via Android's managed profile (same mechanism as Island / Shelter).
 *
 * Privileged work (installExistingPackage / uninstall / wipe) must run in the
 * work profile. The personal UI queues tasks in [CloneBridge], launches the
 * work-profile instance of our app (which runs [WorkAgent]), and the agent binds
 * back to pull and execute them. Outcomes are confirmed by re-scanning the work
 * profile through LauncherApps.
 */
class WorkProfileEngine(context: Context) : VirtualizationEngine {

    private val app = context.applicationContext
    private val dpm = app.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val launcher = app.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

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
        return runCatching { launcher.getActivityList(pkg, user).isNotEmpty() }.getOrDefault(false)
    }

    /** Launch our app inside the work profile so [WorkAgent] runs there. */
    private fun launchWorkAgent(): Boolean {
        val user = workUser() ?: return false
        val component = runCatching {
            launcher.getActivityList(app.packageName, user).firstOrNull()?.componentName
        }.getOrNull() ?: ComponentName(app.packageName, "com.harsh.dual.MainActivity")
        return runCatching {
            launcher.startMainActivity(component, user, null, null)
            true
        }.getOrDefault(false)
    }

    override suspend fun cloneApp(packageName: String): EngineResult = withContext(Dispatchers.IO) {
        if (workUser() == null) return@withContext EngineResult.Failure("Private space is not set up yet.")
        if (isInWorkProfile(packageName)) return@withContext EngineResult.Success

        CloneBridge.lastReport = null
        CloneBridge.pending = listOf(CloneBridge.Op(CloneBridge.OP_CLONE, packageName))
        if (!launchWorkAgent()) {
            return@withContext EngineResult.Failure("Could not open the private space to run the clone.")
        }
        if (awaitPresence(packageName, present = true)) EngineResult.Success
        else {
            val report = CloneBridge.lastReport
            val base = "The clone did not appear."
            EngineResult.Failure(if (report != null) "$base [$report]" else "$base The work-profile agent did not respond — reopen after the space finishes setting up.")
        }
    }

    override suspend fun removeClone(packageName: String): EngineResult = withContext(Dispatchers.IO) {
        if (!isInWorkProfile(packageName)) return@withContext EngineResult.Success
        CloneBridge.pending = listOf(CloneBridge.Op(CloneBridge.OP_REMOVE, packageName))
        if (!launchWorkAgent()) return@withContext EngineResult.Failure("Could not open the private space.")
        if (awaitPresence(packageName, present = false)) EngineResult.Success
        else EngineResult.Failure("Confirm the removal prompt on your phone to finish removing this clone.")
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
        CloneBridge.pending = listOf(CloneBridge.Op(CloneBridge.OP_WIPE, null))
        launchWorkAgent()
        if (awaitNoWorkProfile()) EngineResult.Success
        else EngineResult.Failure("The space is being removed. It may take a few seconds to disappear.")
    }

    private suspend fun awaitPresence(pkg: String, present: Boolean): Boolean {
        repeat(30) {
            if (isInWorkProfile(pkg) == present) return true
            delay(500)
        }
        return isInWorkProfile(pkg) == present
    }

    private suspend fun awaitNoWorkProfile(): Boolean {
        repeat(30) {
            if (workUser() == null) return true
            delay(500)
        }
        return workUser() == null
    }
}
