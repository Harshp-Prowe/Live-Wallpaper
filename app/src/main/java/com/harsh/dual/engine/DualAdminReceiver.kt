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

        // Route the personal profile's DUAL intents into here, so even the very
        // first clone works without depending on timing. FLAG_MANAGED_CAN_ACCESS_PARENT
        // = the managed profile handles intents sent from the parent (personal).
        runCatching {
            val filter = IntentFilter(CloneBridge.ACTION).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addCrossProfileIntentFilter(
                admin, filter, DevicePolicyManager.FLAG_MANAGED_CAN_ACCESS_PARENT,
            )
        }
    }

    companion object {
        fun componentName(context: Context): ComponentName =
            ComponentName(context.applicationContext, DualAdminReceiver::class.java)
    }
}
