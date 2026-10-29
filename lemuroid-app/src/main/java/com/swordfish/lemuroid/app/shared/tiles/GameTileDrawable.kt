package com.swordfish.lemuroid.app.shared.tiles

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint

/** Default game tile: a solid block in the system's color with the game's initials in the middle. */
class GameTileDrawable(
    private val text: String,
    private val colors: SystemTileColors,
) : Drawable() {
    companion object {
        private const val TEXT_SIZE_RATIO = 0.3f
        private const val MAX_TEXT_WIDTH_RATIO = 0.8f
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentAlpha = 255

    private val textPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        fillPaint.color = colors.base
        fillPaint.alpha = currentAlpha
        canvas.drawRect(bounds, fillPaint)

        textPaint.color = colors.text
        textPaint.alpha = currentAlpha
        textPaint.textSize = height * TEXT_SIZE_RATIO
        val measured = textPaint.measureText(text)
        val maxWidth = width * MAX_TEXT_WIDTH_RATIO
        if (measured > maxWidth && measured > 0f) {
            textPaint.textSize *= maxWidth / measured
        }

        val centerX = bounds.left + width / 2
        val centerY = bounds.top + height / 2
        val baseline = centerY - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, centerX, baseline, textPaint)
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    override fun getIntrinsicWidth(): Int = -1

    override fun getIntrinsicHeight(): Int = -1

    override fun setAlpha(alpha: Int) {
        currentAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        textPaint.colorFilter = colorFilter
        invalidateSelf()
    }
}
