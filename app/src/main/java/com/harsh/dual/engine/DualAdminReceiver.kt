package com.harsh.dual.engine

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Device admin / profile-owner receiver. Android instantiates this inside the
 * newly created work profile once provisioning finishes.
 */
class DualAdminReceiver : DeviceAdminReceiver() {

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = componentName(context)

        // Name the profile and switch it on so its apps become usable.
        runCatching { dpm.setProfileName(admin, "Dual by Harsh") }
        runCatching { dpm.setProfileEnabled(admin) }

        // Let the personal profile forward AgentActivity commands into here.
        runCatching {
            val filter = IntentFilter(AgentActivity.ACTION).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addCrossProfileIntentFilter(
                admin,
                filter,
                DevicePolicyManager.FLAG_PARENT_CAN_ACCESS_MANAGED,
            )
        }
    }

    companion object {
        fun componentName(context: Context): ComponentName =
            ComponentName(context.applicationContext, DualAdminReceiver::class.java)
    }
}
