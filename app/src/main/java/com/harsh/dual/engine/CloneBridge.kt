package com.harsh.dual.engine

/**
 * Shared constants + result channel for the cross-profile forwarding bridge.
 *
 * No-root managed profiles cannot use bindDeviceAdminServiceAsUser (that needs
 * device-owner affiliation, which returns 0 targets here). The mechanism that
 * DOES work without root — and is what Island/Shelter use — is cross-profile
 * intent forwarding: the personal profile fires a forwarded intent that Android
 * routes into the work profile, carrying the package name and a reply Messenger.
 */
object CloneBridge {

    const val ACTION = "com.harsh.dual.action.DUAL"

    const val EXTRA_OP = "op"
    const val EXTRA_PKG = "pkg"
    const val EXTRA_APKS = "apks"
    const val EXTRA_REPLY = "reply"

    const val OP_CLONE = "clone"
    const val OP_REMOVE = "remove"
    const val OP_WIPE = "wipe"

    const val REPORT_KEY = "report"

    /** Last diagnostic report sent back by the work-profile forwarder. */
    @Volatile
    var lastReport: String? = null
}
