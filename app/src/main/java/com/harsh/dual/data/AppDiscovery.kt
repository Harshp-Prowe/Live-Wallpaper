package com.harsh.dual.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads the installed, launchable apps from the personal profile and derives a
 * realistic compatibility rating for cloning. All heavy work runs off the main
 * thread; nothing here modifies any installed package.
 */
class AppDiscovery(private val context: Context) {

    suspend fun loadLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        val own = context.packageName

        resolved.asSequence()
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != own }
            .mapNotNull { pkg -> runCatching { inspect(pm, pkg) }.getOrNull() }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun inspect(pm: PackageManager, pkg: String): InstalledApp {
        val ai = pm.getApplicationInfo(pkg, 0)
        val pi = pm.getPackageInfo(pkg, 0)

        val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) ai.minSdkVersion else 0
        val targetSdk = ai.targetSdkVersion
        val isSplit = ai.splitSourceDirs?.isNotEmpty() == true
        val hasNative = hasNativeLibraries(ai)
        val google = usesGoogleServices(pm, pkg)

        val report = rate(minSdk, targetSdk, google, pm, pkg)

        return InstalledApp(
            packageName = pkg,
            label = pm.getApplicationLabel(ai).toString(),
            icon = runCatching { pm.getApplicationIcon(ai) }.getOrNull(),
            versionName = pi.versionName ?: "",
            minSdk = minSdk,
            targetSdk = targetSdk,
            isSplitApk = isSplit,
            hasNativeLibs = hasNative,
            usesGoogleServices = google,
            compatibility = report.level,
        )
    }

    private fun hasNativeLibraries(ai: ApplicationInfo): Boolean {
        val dir = ai.nativeLibraryDir ?: return false
        return runCatching { File(dir).listFiles()?.any { it.name.endsWith(".so") } == true }
            .getOrDefault(false)
    }

    private fun usesGoogleServices(pm: PackageManager, pkg: String): Boolean {
        // Heuristic: apps that declare a dependency on Play Services metadata.
        return runCatching {
            val ai = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
            ai.metaData?.containsKey("com.google.android.gms.version") == true
        }.getOrDefault(false)
    }

    /** Compatibility heuristic — deliberately conservative and honest. */
    fun rate(
        minSdk: Int,
        targetSdk: Int,
        google: Boolean,
        pm: PackageManager,
        pkg: String,
    ): CompatibilityReport {
        // Known device-bound app families that resist any non-root isolation.
        val hardBlocked = listOf("bank", "upi", "netflix", "gpay", "wallet", "authenticator")
        if (hardBlocked.any { pkg.contains(it, ignoreCase = true) }) {
            return CompatibilityReport(
                Compatibility.UNSUPPORTED,
                "This app category relies on hardware-backed keys or Play Integrity, which a cloned instance cannot satisfy. Bypassing it is not supported.",
            )
        }
        if (google) {
            return CompatibilityReport(
                Compatibility.PARTIAL,
                "Uses Google Play Services. Push notifications and Google Sign-In may be limited inside the second space.",
            )
        }
        return CompatibilityReport(Compatibility.HIGH, "Expected to work in the second space.")
    }
}
