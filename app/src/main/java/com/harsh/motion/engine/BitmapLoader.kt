package com.harsh.motion.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Decodes a photo scaled down to at most [maxDimension] on its longest side.
 * Loading a multi-megapixel photo at full resolution wastes memory and battery
 * for no visual benefit on a phone screen, so every consumer (wallpaper engine
 * and in-app preview) goes through this.
 *
 * `file://` URIs (our own private copies, see [PhotoStore]) are opened directly
 * via [FileInputStream] rather than through [android.content.ContentResolver] —
 * some OEM Android builds handle ContentResolver's "file" scheme path
 * inconsistently, and a plain File read is both simpler and fully sufficient
 * for a file the app already owns.
 *
 * Throws (rather than silently returning null) so callers can surface the real
 * reason a photo failed to load instead of guessing at a blank/black result.
 */
object BitmapLoader {

    fun decodeScaled(context: Context, uri: Uri, maxDimension: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(context, uri).use { BitmapFactory.decodeStream(it, null, bounds) }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("Unsupported or unreadable image format")
        }

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDimension || bounds.outHeight / (sample * 2) >= maxDimension) {
            sample *= 2
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return openStream(context, uri).use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("Decoder returned no bitmap for this photo")
    }

    private fun openStream(context: Context, uri: Uri): InputStream {
        if (uri.scheme == "file") {
            val path = uri.path ?: throw IllegalStateException("Invalid file path in $uri")
            val file = File(path)
            if (!file.exists()) throw IllegalStateException("File does not exist: $path")
            return FileInputStream(file)
        }
        return context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open the photo (no input stream for $uri)")
    }
}
