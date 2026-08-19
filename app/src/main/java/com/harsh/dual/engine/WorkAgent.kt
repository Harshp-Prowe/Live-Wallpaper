package com.harsh.dual.engine

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Runs inside the *work profile* when our launcher activity is started there via
 * LauncherApps (which works without affiliation). Its only job is to (re)install
 * the cross-profile intent filter so the personal profile's forwarded DUAL
 * intents get routed into this profile, then finish invisibly.
 */
object WorkAgent {

    fun isAgent(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return runCatching { dpm.isProfileOwnerApp(context.packageName) }.getOrDefault(false)
    }

    fun run(activity: Activity) {
        val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = DualAdminReceiver.componentName(activity)
        runCatching {
            val filter = IntentFilter(CloneBridge.ACTION).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            // FLAG_MANAGED_CAN_ACCESS_PARENT = the managed profile handles intents
            // sent from the parent (personal) profile. This is the direction we need.
            dpm.addCrossProfileIntentFilter(
                admin, filter, DevicePolicyManager.FLAG_MANAGED_CAN_ACCESS_PARENT,
            )
        }
        activity.finish()
    }
}
