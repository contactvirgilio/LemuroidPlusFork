package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import com.swordfish.libretrodroid.ViewportAlignment
import kotlin.math.roundToInt

/**
 * Turns a screenshot of the whole emulator view into a picture of just the game:
 * 1. crop to the viewport (the part of the screen the game is drawn in);
 * 2. trim the black bars the game is letterboxed with inside that area.
 *
 * Bars are trimmed by the same amount on opposite sides (or only on the free side when the game is
 * aligned to the top or bottom), so dark areas at the edge of the game picture itself are kept.
 */
object GamePreviewCropper {
    private const val BLACK_THRESHOLD = 12
    private const val MIN_SIZE_FRACTION = 0.3f

    fun crop(
        screenshot: Bitmap,
        viewport: RectF,
        alignment: ViewportAlignment,
    ): Bitmap {
        val inViewport = cropToViewport(screenshot, viewport)
        return trimLetterbox(inViewport, alignment) ?: inViewport
    }

    private fun cropToViewport(
        bitmap: Bitmap,
        viewport: RectF,
    ): Bitmap {
        val left = (viewport.left * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (viewport.top * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = (viewport.right * bitmap.width).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = (viewport.bottom * bitmap.height).roundToInt().coerceIn(top + 1, bitmap.height)
        if (left == 0 && top == 0 && right == bitmap.width && bottom == bitmap.height) return bitmap
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    private fun trimLetterbox(
        bitmap: Bitmap,
        alignment: ViewportAlignment,
    ): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        fun isBlack(color: Int) =
            Color.red(color) <= BLACK_THRESHOLD &&
                Color.green(color) <= BLACK_THRESHOLD &&
                Color.blue(color) <= BLACK_THRESHOLD

        fun columnIsBlack(x: Int) = (0 until height).all { y -> isBlack(pixels[y * width + x]) }

        fun rowIsBlack(y: Int) = (0 until width).all { x -> isBlack(pixels[y * width + x]) }

        val blackLeft = (0 until width).takeWhile { columnIsBlack(it) }.count()
        if (blackLeft >= width) return null // Completely black frame: nothing to trim against.
        val blackRight = (width - 1 downTo 0).takeWhile { columnIsBlack(it) }.count()
        val blackTop = (0 until height).takeWhile { rowIsBlack(it) }.count()
        val blackBottom = (height - 1 downTo 0).takeWhile { rowIsBlack(it) }.count()

        // Pillarbox bars are always centred horizontally.
        val horizontal = minOf(blackLeft, blackRight)
        val (top, bottom) =
            when (alignment) {
                ViewportAlignment.TOP -> 0 to blackBottom
                ViewportAlignment.BOTTOM -> blackTop to 0
                else -> minOf(blackTop, blackBottom).let { it to it }
            }

        val newWidth = width - 2 * horizontal
        val newHeight = height - top - bottom
        if (newWidth < width * MIN_SIZE_FRACTION || newHeight < height * MIN_SIZE_FRACTION) return null
        if (newWidth == width && newHeight == height) return bitmap
        return Bitmap.createBitmap(bitmap, horizontal, top, newWidth, newHeight)
    }
}
