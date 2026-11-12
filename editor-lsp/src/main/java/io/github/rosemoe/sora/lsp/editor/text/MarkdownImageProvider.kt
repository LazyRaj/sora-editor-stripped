package io.github.rosemoe.sora.lsp.editor.text

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64

/**
 * Interface to load an image from a given source string.
 */
interface ImageProvider {
    /**
     * Attempts to load an image from the given source string.
     * @param src Source string (e.g., data URI, file path, URL)
     * @return A [Drawable] if successful, or null if the image cannot be loaded.
     */
    fun load(src: String): Drawable?
}

/**
 * Default implementation of [ImageProvider] that decodes Base64 raster images from data URIs.
 *
 * @param resources Android resources used to create the [BitmapDrawable]. Defaults to [Resources.getSystem].
 * @param maxWidth Maximum width for returned Bitmaps. Images wider than this will be scaled down preserving aspect ratio.
 */
open class DefaultImageProvider(
    private val maxWidth: Int = 800,
    private val resources: Resources? = null
) : ImageProvider {

    override fun load(src: String): Drawable? {
        if (!src.startsWith("data:")) return null

        val payload = src.substringAfter("base64,", "")
        if (payload.isEmpty()) return null

        val imageByteArray = try {
            Base64.decode(payload, Base64.DEFAULT)
        } catch (_: Exception) {
            return null
        }
        val bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size) ?: return null
        val scaledBitmap = scaleIfNeeded(bitmap, maxWidth)
        val res = resources ?: runCatching { Resources.getSystem() }.getOrNull()
        return if (res != null) {
            BitmapDrawable(res, scaledBitmap)
        } else {
            null
        }
    }

    /**
     * Scale down a bitmap to maxWidth preserving aspect ratio. If bitmap width
     * is already <= maxWidth, the original bitmap is returned.
     */
    private fun scaleIfNeeded(bmp: Bitmap, maxWidth: Int): Bitmap {
        val currentWidth = bmp.width
        if (currentWidth <= maxWidth) return bmp
        val ratio = maxWidth.toFloat() / currentWidth.toFloat()

        val newHeight = (bmp.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bmp, maxWidth, newHeight, true)
    }
}
