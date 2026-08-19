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

    fun copyToPrivateStorage(context: Context, source: Uri): Uri {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        val input = context.contentResolver.openInputStream(source)
            ?: throw IllegalStateException("Could not read the selected photo")
        input.use { inStream ->
            dest.outputStream().use { outStream -> inStream.copyTo(outStream) }
        }
        return Uri.fromFile(dest)
    }
}
