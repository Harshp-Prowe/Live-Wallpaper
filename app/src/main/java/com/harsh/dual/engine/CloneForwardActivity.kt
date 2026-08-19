package com.harsh.dual.engine

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import java.io.File
import java.io.FileInputStream

/**
 * Runs INSIDE the work profile. Android forwards the personal profile's DUAL
 * intent here (see [WorkAgent] which sets up the forwarding). As profile owner
 * we can install/remove/wipe, then reply through the supplied Messenger.
 */
class CloneForwardActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = DualAdminReceiver.componentName(this)
        val op = intent.getStringExtra(CloneBridge.EXTRA_OP)
        val pkg = intent.getStringExtra(CloneBridge.EXTRA_PKG)
        @Suppress("DEPRECATION")
        val reply = intent.getParcelableExtra<Messenger>(CloneBridge.EXTRA_REPLY)

        val report = if (!dpm.isProfileOwnerApp(packageName)) {
            "not-profile-owner"
        } else when (op) {
            CloneBridge.OP_CLONE -> {
                val apks = intent.getStringArrayExtra(CloneBridge.EXTRA_APKS)
                if (pkg.isNullOrEmpty() || apks.isNullOrEmpty()) "clone(no-apks)"
                else runCatching { installApks(pkg, apks) }
                    .getOrElse { "clone err=${it.javaClass.simpleName}:${it.message}" }
            }

            CloneBridge.OP_REMOVE -> if (pkg.isNullOrEmpty()) "remove(no-pkg)" else
                runCatching {
                    packageManager.packageInstaller.uninstall(
                        pkg, android.app.PendingIntent.getBroadcast(
                            this, 0, android.content.Intent("com.harsh.dual.UNINSTALL_DONE"),
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
                                    android.app.PendingIntent.FLAG_MUTABLE else 0,
                        ).intentSender,
                    )
                    "remove($pkg)=requested"
                }.getOrElse { "remove err=${it.javaClass.simpleName}" }

            CloneBridge.OP_WIPE -> runCatching { dpm.wipeData(0); "wipe=ok" }
                .getOrElse { "wipe err=${it.javaClass.simpleName}" }

            else -> "unknown-op=$op"
        }

        runCatching {
            reply?.send(Message.obtain().apply {
                data = Bundle().apply { putString(CloneBridge.REPORT_KEY, report) }
            })
        }
        finish()
    }

    /**
     * Installs the app's APK(s) into THIS (work) profile via PackageInstaller.
     * The source APKs live in /data/app and are readable & shared across
     * profiles, so no affiliation/device-owner is needed. As profile owner the
     * commit is silent.
     */
    private fun installApks(pkg: String, apks: Array<String>): String {
        val installer = packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(pkg)
        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)
        try {
            apks.forEach { path ->
                val file = File(path)
                session.openWrite(file.name, 0, file.length()).use { out ->
                    FileInputStream(file).use { input -> input.copyTo(out) }
                    session.fsync(out)
                }
            }
            val sender = android.app.PendingIntent.getBroadcast(
                this, sessionId, android.content.Intent("com.harsh.dual.INSTALL_DONE"),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S)
                        android.app.PendingIntent.FLAG_MUTABLE else 0,
            )
            session.commit(sender.intentSender)
            return "clone($pkg)=committed(${apks.size} apk)"
        } catch (t: Throwable) {
            session.abandon()
            return "clone install err=${t.javaClass.simpleName}:${t.message}"
        } finally {
            session.close()
        }
    }
}
