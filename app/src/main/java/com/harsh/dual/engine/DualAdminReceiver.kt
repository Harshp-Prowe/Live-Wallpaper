package com.harsh.dual.engine

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

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
    }

    companion object {
        fun componentName(context: Context): ComponentName =
            ComponentName(context.applicationContext, DualAdminReceiver::class.java)
    }
}
