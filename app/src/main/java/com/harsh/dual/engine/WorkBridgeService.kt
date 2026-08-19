package com.harsh.dual.engine

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger

/**
 * Personal-profile service. The work-profile agent binds to this across the
 * profile boundary (via DevicePolicyManager.bindDeviceAdminServiceAsUser) and
 * asks for the queued tasks. We reply with the task list; the agent executes it
 * inside the work profile where it holds profile-owner power.
 */
class WorkBridgeService : Service() {

    private val messenger = Messenger(Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            CloneBridge.MSG_GET_TASKS -> {
                val ops = CloneBridge.pending
                val reply = Message.obtain(null, CloneBridge.MSG_TASKS).apply {
                    data = Bundle().apply {
                        putStringArray("types", ops.map { it.type }.toTypedArray())
                        putStringArray("pkgs", ops.map { it.pkg ?: "" }.toTypedArray())
                    }
                }
                runCatching { msg.replyTo?.send(reply) }
            }
            CloneBridge.MSG_RESULT -> {
                CloneBridge.lastReport = msg.data?.getString("report")
            }
        }
        true
    })

    override fun onBind(intent: Intent?): IBinder = messenger.binder
}
