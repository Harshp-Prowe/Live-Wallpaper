package com.harsh.dual.engine

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.os.UserHandle
import com.harsh.dual.data.SpaceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Work Profile engine using cross-profile intent forwarding (no root, no
 * device-owner affiliation). The personal profile fires a forwarded DUAL intent
 * that Android routes into the work profile's [CloneForwardActivity], which runs
 * the privileged operation and replies via a Messenger. Outcomes are also
 * confirmed by re-scanning the work profile through LauncherApps.
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

    /** Launch our app in the work profile so [WorkAgent] refreshes the forwarding. */
    private fun refreshRouting(): Boolean {
        val user = workUser() ?: return false
        val component = runCatching {
            launcher.getActivityList(app.packageName, user).firstOrNull()?.componentName
        }.getOrNull() ?: ComponentName(app.packageName, "com.harsh.dual.MainActivity")
        return runCatching { launcher.startMainActivity(component, user, null, null); true }
            .getOrDefault(false)
    }

    /** Ensure the personal copy of the forwarder is disabled so the DUAL intent
     *  is routed across to the work profile instead of resolving locally. */
    private fun disableLocalForwarder() {
        runCatching {
            app.packageManager.setComponentEnabledSetting(
                ComponentName(app, CloneForwardActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    /** All APK files (base + splits) for an installed package. */
    private fun apkPathsFor(pkg: String): Array<String> = runCatching {
        val ai = app.packageManager.getApplicationInfo(pkg, 0)
        (listOfNotNull(ai.sourceDir) + (ai.splitSourceDirs?.toList() ?: emptyList())).toTypedArray()
    }.getOrDefault(emptyArray())

    /** Fire one forwarded operation into the work profile. */
    private fun fireOp(op: String, pkg: String?) {
        CloneBridge.lastReport = null
        val reply = Messenger(Handler(Looper.getMainLooper()) { msg ->
            CloneBridge.lastReport = msg.data?.getString(CloneBridge.REPORT_KEY)
            true
        })
        val intent = Intent(CloneBridge.ACTION).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(CloneBridge.EXTRA_OP, op)
            pkg?.let {
                putExtra(CloneBridge.EXTRA_PKG, it)
                if (op == CloneBridge.OP_CLONE) putExtra(CloneBridge.EXTRA_APKS, apkPathsFor(it))
            }
            putExtra(CloneBridge.EXTRA_REPLY, reply)
        }
        app.startActivity(intent)
    }

    override suspend fun cloneApp(packageName: String): EngineResult = withContext(Dispatchers.IO) {
        if (workUser() == null) return@withContext EngineResult.Failure("Private space is not set up yet.")
        if (isInWorkProfile(packageName)) return@withContext EngineResult.Success

        refreshRouting()
        delay(1500)
        disableLocalForwarder()

        val fired = runCatching { fireOp(CloneBridge.OP_CLONE, packageName) }.isSuccess
        if (!fired) return@withContext EngineResult.Failure(
            "Could not route the clone into the private space (forwarding unavailable).",
        )

        if (awaitPresence(packageName, present = true)) EngineResult.Success
        else {
            val report = CloneBridge.lastReport
            EngineResult.Failure(
                "The clone did not appear." + if (report != null) " [$report]"
                else " No response from the private space. Delete the work profile in Settings and Create Space again.",
            )
        }
    }

    override suspend fun removeClone(packageName: String): EngineResult = withContext(Dispatchers.IO) {
        if (!isInWorkProfile(packageName)) return@withContext EngineResult.Success
        refreshRouting(); delay(1200); disableLocalForwarder()
        runCatching { fireOp(CloneBridge.OP_REMOVE, packageName) }
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
        refreshRouting(); delay(1200); disableLocalForwarder()
        runCatching { fireOp(CloneBridge.OP_WIPE, null) }
        if (awaitNoWorkProfile()) EngineResult.Success
        else EngineResult.Failure("The space is being removed. It may take a few seconds to disappear.")
    }

    private suspend fun awaitPresence(pkg: String, present: Boolean): Boolean {
        // Installing large APKs can take a while; allow up to ~45s.
        repeat(90) {
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
