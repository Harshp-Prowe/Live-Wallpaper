package com.harsh.motion.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Decodes a photo scaled down to at most [maxDimension] on its longest side.
 * Loading a multi-megapixel photo at full resolution wastes memory and battery
 * for no visual benefit on a phone screen, so every consumer (wallpaper engine
 * and in-app preview) goes through this.
 *
 * Throws (rather than silently returning null) so callers can surface the real
 * reason a photo failed to load instead of guessing at a blank/black result.
 */
object BitmapLoader {

    fun decodeScaled(context: Context, uri: Uri, maxDimension: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IllegalStateException("Could not open the photo (no input stream for $uri)")

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalStateException("Unsupported or unreadable image format")
        }

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDimension || bounds.outHeight / (sample * 2) >= maxDimension) {
            sample *= 2
        }

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val stream = resolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not reopen the photo for decoding")
        return stream.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("Decoder returned no bitmap for this photo")
    }
}
