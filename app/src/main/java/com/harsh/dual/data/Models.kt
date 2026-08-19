package com.harsh.dual.data

import android.graphics.drawable.Drawable

/** An app installed in the personal profile that the user may clone. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val versionName: String,
    val minSdk: Int,
    val targetSdk: Int,
    val isSplitApk: Boolean,
    val hasNativeLibs: Boolean,
    val usesGoogleServices: Boolean,
    val compatibility: Compatibility,
)

/** A clone that lives inside the work profile. */
data class Clone(
    val packageName: String,
    val label: String,
    val createdAt: Long,
    val enabled: Boolean = true,
)

enum class Compatibility(val label: String) {
    HIGH("High compatibility"),
    PARTIAL("Partial — some features may not work"),
    UNSUPPORTED("Not supported on this device");
}

/** Reason attached to a partial/unsupported rating, shown to the user. */
data class CompatibilityReport(
    val level: Compatibility,
    val reason: String,
)

/** State of the private work space managed by the app. */
enum class SpaceState {
    UNAVAILABLE, // Device cannot host a managed profile
    NOT_CREATED, // Supported but the space has not been provisioned yet
    READY,       // Work profile exists and this app is its profile owner
}
