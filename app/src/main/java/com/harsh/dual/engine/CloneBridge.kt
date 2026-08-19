package com.harsh.dual.engine

/**
 * In-process hand-off between the personal-profile UI and the personal-profile
 * [WorkBridgeService]. The work-profile agent binds to that service across the
 * profile boundary and pulls whatever tasks are queued here.
 *
 * Both the UI and WorkBridgeService live in the same personal process, so a
 * simple singleton is enough — no IPC needed on this side.
 */
object CloneBridge {

    const val OP_CLONE = "clone"
    const val OP_REMOVE = "remove"
    const val OP_WIPE = "wipe"

    // Messenger protocol between the two profiles.
    const val MSG_GET_TASKS = 1
    const val MSG_TASKS = 2
    const val MSG_RESULT = 3

    data class Op(val type: String, val pkg: String?)

    @Volatile
    var pending: List<Op> = emptyList()

    /** Last diagnostic report sent back by the work-profile agent. */
    @Volatile
    var lastReport: String? = null
}
