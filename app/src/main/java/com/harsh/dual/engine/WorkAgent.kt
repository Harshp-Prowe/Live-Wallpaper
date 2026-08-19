package com.harsh.dual.engine

import android.app.Activity
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger

/**
 * Runs inside the *work profile*. When our launcher activity is started in the
 * work profile (by the personal UI via LauncherApps), this takes over instead of
 * showing UI: it binds back to the personal-profile [WorkBridgeService], pulls
 * the queued tasks, and executes them with profile-owner privileges.
 */
object WorkAgent {

    /** True only for the work-profile instance where we are profile owner. */
    fun isAgent(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return runCatching { dpm.isProfileOwnerApp(context.packageName) }.getOrDefault(false)
    }

    fun run(activity: Activity) {
        val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = DualAdminReceiver.componentName(activity)

        val parent = runCatching { dpm.getBindDeviceAdminTargetUsers(admin).firstOrNull() }.getOrNull()
        if (parent == null) { activity.finish(); return }

        var connection: ServiceConnection? = null
        var remote: Messenger? = null

        val incoming = Messenger(Handler(Looper.getMainLooper()) { msg ->
            if (msg.what == CloneBridge.MSG_TASKS) {
                val types = msg.data?.getStringArray("types") ?: emptyArray()
                val pkgs = msg.data?.getStringArray("pkgs") ?: emptyArray()
                val report = StringBuilder("agent ok; tasks=${types.size}")
                types.forEachIndexed { i, type ->
                    val detail = executeOp(activity, dpm, admin, type, pkgs.getOrNull(i))
                    report.append("; ").append(detail)
                }
                // Send diagnostics back to the personal side before leaving.
                runCatching {
                    remote?.send(
                        Message.obtain(null, CloneBridge.MSG_RESULT).apply {
                            data = android.os.Bundle().apply { putString("report", report.toString()) }
                        },
                    )
                }
                connection?.let { runCatching { activity.unbindService(it) } }
                activity.finish()
            }
            true
        })

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val m = Messenger(binder)
                remote = m
                val req = Message.obtain(null, CloneBridge.MSG_GET_TASKS).apply { replyTo = incoming }
                runCatching { m.send(req) }
            }
            override fun onServiceDisconnected(name: ComponentName?) {}
        }
        connection = conn

        val serviceIntent = Intent().setClassName(
            activity.packageName, WorkBridgeService::class.java.name,
        )
        val bound = runCatching {
            dpm.bindDeviceAdminServiceAsUser(
                admin, serviceIntent, conn, Context.BIND_AUTO_CREATE, parent,
            )
        }.getOrDefault(false)

        if (!bound) activity.finish()
        // Safety net: never hang the invisible agent open.
        Handler(Looper.getMainLooper()).postDelayed({
            if (!activity.isFinishing) {
                connection?.let { runCatching { activity.unbindService(it) } }
                activity.finish()
            }
        }, 20_000)
    }

    private fun executeOp(
        activity: Activity,
        dpm: DevicePolicyManager,
        admin: ComponentName,
        type: String,
        pkg: String?,
    ): String = when (type) {
        CloneBridge.OP_CLONE -> if (pkg.isNullOrEmpty()) "clone(no-pkg)" else
            runCatching { "clone($pkg)=${dpm.installExistingPackage(admin, pkg)}" }
                .getOrElse { "clone($pkg) err=${it.javaClass.simpleName}:${it.message}" }

        CloneBridge.OP_REMOVE -> if (pkg.isNullOrEmpty()) "remove(no-pkg)" else
            runCatching {
                val installer = activity.packageManager.packageInstaller
                val mutableFlag =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val sender = PendingIntent.getBroadcast(
                    activity, 0, Intent("com.harsh.dual.UNINSTALL_DONE"),
                    PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag,
                )
                installer.uninstall(pkg, sender.intentSender)
                "remove($pkg)=requested"
            }.getOrElse { "remove($pkg) err=${it.javaClass.simpleName}" }

        CloneBridge.OP_WIPE -> runCatching { dpm.wipeData(0); "wipe=ok" }
            .getOrElse { "wipe err=${it.javaClass.simpleName}" }

        else -> "unknown-op"
    }
}
