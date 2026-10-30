package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.graphics.Bitmap
import android.graphics.RectF
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.libretrodroid.ViewportAlignment
import kotlin.math.roundToInt

/**
 * Cuts the game picture out of a screenshot of the whole emulator view.
 *
 * LibretroDroid draws the game inside the viewport, fitted to the game's aspect ratio, centred
 * horizontally and aligned vertically (top/center/bottom), and fills the rest with a blurred
 * background. The same layout is recomputed here from the viewport, the alignment and the
 * system's display aspect ratio, so the result contains only the game.
 */
object GamePreviewCropper {
    /** Display aspect ratio of each system with the default core settings. Null = varies per game. */
    fun aspectRatioFor(systemId: SystemID): Float? =
        when (systemId) {
            SystemID.GB, SystemID.GBC, SystemID.GG -> 10f / 9f
            SystemID.GBA -> 3f / 2f
            SystemID.NGP, SystemID.NGC -> 20f / 19f
            SystemID.WS, SystemID.WSC -> 14f / 9f
            SystemID.LYNX -> 160f / 102f
            SystemID.NDS -> 2f / 3f // Two screens stacked (default layout)
            SystemID.NINTENDO_3DS -> 5f / 6f // Two screens stacked (default layout)
            SystemID.PSP -> 480f / 272f
            SystemID.FBNEO, SystemID.MAME2003PLUS -> null // Horizontal and vertical games
            else -> 4f / 3f
        }

    fun crop(
        screenshot: Bitmap,
        viewport: RectF,
        alignment: ViewportAlignment,
        aspectRatio: Float?,
    ): Bitmap {
        val width = screenshot.width.toFloat()
        val height = screenshot.height.toFloat()

        val viewportLeft = viewport.left * width
        val viewportTop = viewport.top * height
        val viewportWidth = viewport.width() * width
        val viewportHeight = viewport.height() * height
        if (viewportWidth <= 1f || viewportHeight <= 1f) return screenshot

        var gameLeft = viewportLeft
        var gameTop = viewportTop
        var gameWidth = viewportWidth
        var gameHeight = viewportHeight

        if (aspectRatio != null && aspectRatio > 0f) {
            if (aspectRatio > viewportWidth / viewportHeight) {
                // Limited by width: empty space above/below, placed by the alignment.
                gameHeight = viewportWidth / aspectRatio
                val emptySpace = viewportHeight - gameHeight
                gameTop =
                    when (alignment) {
                        ViewportAlignment.TOP -> viewportTop
                        ViewportAlignment.BOTTOM -> viewportTop + emptySpace
                        else -> viewportTop + emptySpace / 2f
                    }
            } else {
                // Limited by height: empty space left/right, always centred.
                gameWidth = viewportHeight * aspectRatio
                gameLeft = viewportLeft + (viewportWidth - gameWidth) / 2f
            }
        }

        val left = gameLeft.roundToInt().coerceIn(0, screenshot.width - 1)
        val top = gameTop.roundToInt().coerceIn(0, screenshot.height - 1)
        val right = (gameLeft + gameWidth).roundToInt().coerceIn(left + 1, screenshot.width)
        val bottom = (gameTop + gameHeight).roundToInt().coerceIn(top + 1, screenshot.height)
        if (left == 0 && top == 0 && right == screenshot.width && bottom == screenshot.height) return screenshot
        return Bitmap.createBitmap(screenshot, left, top, right - left, bottom - top)
    }
}
