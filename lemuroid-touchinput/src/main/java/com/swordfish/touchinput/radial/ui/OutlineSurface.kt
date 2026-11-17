package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/** Outline skin: a filled shape (may be transparent) with a thin ring drawn on top of it. */
@Composable
fun OutlineSurface(
    modifier: Modifier = Modifier,
    rectangle: Boolean = false,
    fillColor: Color = Color.Transparent,
    strokeColor: Color,
    strokeWidth: Dp,
    content: @Composable BoxWithConstraintsScope.() -> Unit = { },
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier =
            modifier.drawBehind {
                val sw = strokeWidth.toPx()
                if (rectangle) {
                    drawRect(color = fillColor)
                    drawRect(
                        color = strokeColor,
                        topLeft = Offset(sw / 2f, sw / 2f),
                        size = Size(size.width - sw, size.height - sw),
                        style = Stroke(width = sw),
                    )
                } else {
                    val radius = size.minDimension / 2f
                    drawCircle(color = fillColor, radius = radius)
                    drawCircle(color = strokeColor, radius = radius - sw / 2f, style = Stroke(width = sw))
                }
            },
    ) {
        content()
    }
}

/** The plus-shaped outline around the whole d-pad. */
fun DrawScope.drawCrossOutline(
    color: Color,
    strokeWidth: Dp,
    fillColor: Color = Color.Transparent,
) {
    val sw = strokeWidth.toPx()
    val half = sw / 2f
    val w = size.width - sw
    val h = size.height - sw
    val a = half + w / 3f
    val b = half + 2f * w / 3f
    val c = half + h / 3f
    val d = half + 2f * h / 3f
    val right = half + w
    val bottom = half + h
    val path =
        Path().apply {
            moveTo(a, half)
            lineTo(b, half)
            lineTo(b, c)
            lineTo(right, c)
            lineTo(right, d)
            lineTo(b, d)
            lineTo(b, bottom)
            lineTo(a, bottom)
            lineTo(a, d)
            lineTo(half, d)
            lineTo(half, c)
            lineTo(a, c)
            close()
        }
    drawPath(path = path, color = fillColor)
    drawPath(path = path, color = color, style = Stroke(width = sw))
}
