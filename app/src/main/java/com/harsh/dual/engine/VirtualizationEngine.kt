package com.harsh.dual.engine

import android.content.Intent
import com.harsh.dual.data.SpaceState

/**
 * Abstraction over the mechanism that isolates cloned apps. The app is written
 * against this interface so the underlying engine (currently Android Work
 * Profile) can be replaced without touching the UI.
 *
 * Honest scope: on modern Android the only no-root way to run a real, isolated
 * second instance of an app is the managed Work Profile. In-app container
 * engines (VirtualApp / Parallel Space style) are intentionally excluded — they
 * do not work reliably without root on Android 10+ and this project refuses to
 * ship fake cloning.
 */
interface VirtualizationEngine {

    /** Current state of the private space on this device. */
    fun spaceState(): SpaceState

    /**
     * Intent that provisions the private space. The caller starts it for result;
     * Android shows a single confirmation screen — no manual Settings steps.
     * Returns null if the device cannot host a managed profile.
     */
    fun createSpaceIntent(): Intent?

    /** Create a clone of [packageName] inside the private space. */
    suspend fun cloneApp(packageName: String): EngineResult

    /** Remove a single clone. Never touches the original app. */
    suspend fun removeClone(packageName: String): EngineResult

    /** Launch a cloned app inside the private space. */
    fun launchClone(packageName: String): EngineResult

    /** Remove the entire private space and every clone in it. Reversible cleanup. */
    suspend fun removeSpace(): EngineResult
}

sealed interface EngineResult {
    data object Success : EngineResult
    data class Failure(val reason: String) : EngineResult
}
