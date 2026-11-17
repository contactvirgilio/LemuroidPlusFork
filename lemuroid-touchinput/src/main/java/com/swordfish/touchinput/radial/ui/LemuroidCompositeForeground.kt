package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme

@Composable
fun LemuroidCompositeForeground(pressed: State<Boolean>) {
    val theme = LocalLemuroidPadTheme.current
    if (theme.outline) {
        val fill = if (pressed.value) theme.outlinePressedFill else Color.Transparent
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCrossOutline(color = theme.outlineColor, strokeWidth = theme.outlineStroke, fillColor = fill)
        }
        return
    }
    GlassSurface(
        modifier = Modifier.fillMaxSize(),
        fillColor = theme.compositeFill(pressed.value),
        shadowColor = theme.level2Shadow,
        shadowWidth = theme.level2ShadowWidth,
    )
}
