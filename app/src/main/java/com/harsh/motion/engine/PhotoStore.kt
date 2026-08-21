package com.harsh.motion.engine

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Copies a picked photo into the app's own private storage immediately after
 * selection. Photo Picker URIs are only reliably readable right at pick time;
 * relying on them later (after recomposition, app restart, or from the
 * wallpaper service's own process context) is fragile and was the root cause
 * of "no input stream" failures. Owning a private copy removes that class of
 * bug entirely — no URI permission handling needed anywhere else in the app.
 */
object PhotoStore {

    private const val DIR = "photos"

    fun copyToPrivateStorage(context: Context, source: Uri): Uri {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        val input = context.contentResolver.openInputStream(source)
            ?: throw IllegalStateException("Could not read the selected photo")
        input.use { inStream ->
            dest.outputStream().use { outStream -> inStream.copyTo(outStream) }
        }
        return Uri.fromFile(dest)
    }

    /**
     * Deletes every private photo copy that no saved wallpaper points at.
     *
     * Each pick writes a new multi-megabyte copy, so abandoned ones (picked,
     * then replaced or never saved) used to accumulate forever — filling up
     * internal storage, which on a device that is already low stops the *system*
     * from writing a new wallpaper at all, from this or any other app. Clearing
     * the app's data or uninstalling it appeared to "fix" wallpaper changing
     * precisely because both freed these files.
     */
    fun pruneUnreferenced(context: Context, referencedUris: Collection<String>) {
        val dir = File(context.filesDir, DIR)
        if (!dir.isDirectory) return
        val keep = referencedUris.mapNotNullTo(HashSet()) {
            runCatching { Uri.parse(it).path }.getOrNull()
        }
        dir.listFiles()?.forEach { file ->
            if (file.absolutePath !in keep) file.delete()
        }
    }
}
