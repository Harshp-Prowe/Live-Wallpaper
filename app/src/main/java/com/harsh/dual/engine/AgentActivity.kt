package com.harsh.dual.engine

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Invisible worker that runs *inside* the work profile. The personal-profile UI
 * cannot call profile-owner APIs directly, so it fires a forwarded cross-profile
 * intent (see [DualAdminReceiver.onProfileProvisioningComplete]); Android routes
 * it here, where we hold profile-owner privileges and can act.
 *
 * Extras cross the profile boundary with the forwarded intent. Results are not
 * returned across the boundary — the personal side confirms the outcome by
 * re-scanning the work profile via LauncherApps.
 */
class AgentActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = DualAdminReceiver.componentName(this)

        // Only act if we really are the profile owner here.
        if (dpm.isProfileOwnerApp(packageName)) {
            when (intent.getStringExtra(EXTRA_OP)) {
                OP_CLONE -> intent.getStringExtra(EXTRA_PKG)?.let { pkg ->
                    runCatching { dpm.installExistingPackage(admin, pkg) }
                }
                OP_REMOVE -> intent.getStringExtra(EXTRA_PKG)?.let { pkg ->
                    runCatching { uninstall(pkg) }
                }
                OP_WIPE -> runCatching { dpm.wipeData(0) }
            }
        }
        finish()
    }

    private fun uninstall(pkg: String) {
        val installer = packageManager.packageInstaller
        val intent = Intent(this, AgentActivity::class.java)
        val sender = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE,
        )
        installer.uninstall(pkg, sender.intentSender)
    }

    companion object {
        const val ACTION = "com.harsh.dual.action.AGENT"
        const val EXTRA_OP = "op"
        const val EXTRA_PKG = "pkg"
        const val OP_CLONE = "clone"
        const val OP_REMOVE = "remove"
        const val OP_WIPE = "wipe"

        fun opIntent(op: String, pkg: String? = null): Intent =
            Intent(ACTION).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_OP, op)
                pkg?.let { putExtra(EXTRA_PKG, it) }
            }
    }
}
