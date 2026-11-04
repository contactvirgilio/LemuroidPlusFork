package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.swordfish.lemuroid.common.compose.textUnit
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme

@Composable
fun LemuroidButtonForeground(
    modifier: Modifier = Modifier,
    pressed: State<Boolean>,
    pressedColor: Color? = null,
    label: (@Composable BoxWithConstraintsScope.() -> Unit),
    icon: (@Composable BoxWithConstraintsScope.() -> Unit),
) {
    val theme = LocalLemuroidPadTheme.current

    GlassSurface(
        modifier = modifier.fillMaxSize().padding(theme.foregroundPadding),
        fillColor =
            if (pressed.value && pressedColor != null) {
                pressedColor.copy(alpha = 0.9f)
            } else {
                theme.foregroundFill(pressed.value)
            },
        shadowColor = theme.level3Shadow,
        shadowWidth = theme.level3ShadowWidth,
        content = {
            icon()
            label()
        },
    )
}

@Composable
fun LemuroidButtonForeground(
    modifier: Modifier = Modifier,
    pressed: State<Boolean>,
    label: String? = null,
    icon: Int? = null,
    iconScale: Float = 0.5f,
    labelScale: Float = 1.0f,
    pressedColor: Color? = null,
) {
    // Light colors (yellow) get dark lettering, the others white.
    val pressedTint = pressedColor?.let { if (it.luminance() > 0.5f) Color(0xFF1E1804) else Color.White }
    LemuroidButtonForeground(
        modifier = modifier,
        pressed = pressed,
        pressedColor = pressedColor,
        label = { LemuroidButtonForegroundLabel(label, labelScale, pressed, pressedTint) },
        icon = { LemuroidButtonForegroundIcon(icon, iconScale, pressed, pressedTint) },
    )
}

@Composable
private fun BoxWithConstraintsScope.LemuroidButtonForegroundIcon(
    icon: Int?,
    scale: Float,
    pressedState: State<Boolean>,
    pressedTint: Color? = null,
) {
    if (icon == null) return

    Icon(
        modifier = Modifier.size(maxWidth * scale, maxHeight * scale),
        painter = painterResource(icon),
        contentDescription = "",
        tint =
            if (pressedState.value && pressedTint != null) {
                pressedTint
            } else {
                LocalLemuroidPadTheme.current.icons(pressedState.value)
            },
    )
}

@Composable
private fun BoxWithConstraintsScope.LemuroidButtonForegroundLabel(
    label: String?,
    scale: Float,
    pressedState: State<Boolean>,
    pressedTint: Color? = null,
) {
    if (label == null) return
    val fontSize = minOf(maxHeight * 0.5f * scale, maxWidth / label.length * scale)
    Text(
        modifier = Modifier.wrapContentSize(),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        text = label,
        color =
            if (pressedState.value && pressedTint != null) {
                pressedTint
            } else {
                LocalLemuroidPadTheme.current.icons(pressedState.value)
            },
        fontSize = fontSize.textUnit(),
    )
}
