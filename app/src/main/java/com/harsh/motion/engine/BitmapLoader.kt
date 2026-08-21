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
        val decoded = openStream(context, uri).use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("Decoder returned no bitmap for this photo")
        return clampToTextureLimit(decoded)
    }

    /**
     * inSampleSize only halves, and the loop above stops while the *halved* size
     * would still be at or above the target — so a 12MP photo can legitimately
     * decode at its full 4032px. That matters because the wallpaper now renders
     * on a hardware canvas, and a bitmap larger than the GPU's maximum texture
     * size is not drawn at all: Android logs "Bitmap too large to be uploaded
     * into a texture" and silently skips it, which would show as a black
     * wallpaper with only the effect overlays on top.
     *
     * 4096 is the safe ceiling for every GPU on our minSdk (26) and above, and
     * it is well past the screen resolution these are drawn at, so this cannot
     * change how any photo that already worked looks.
     */
    private fun clampToTextureLimit(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_TEXTURE_PX) return bitmap
        val factor = MAX_TEXTURE_PX.toFloat() / longest
        val w = (bitmap.width * factor).toInt().coerceAtLeast(1)
        val h = (bitmap.height * factor).toInt().coerceAtLeast(1)
        return runCatching {
            Bitmap.createScaledBitmap(bitmap, w, h, true).also { scaled ->
                if (scaled !== bitmap) bitmap.recycle()
            }
        }.getOrDefault(bitmap)
    }

    private const val MAX_TEXTURE_PX = 4096

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
