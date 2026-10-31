package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.tiles.SystemTileColors
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Outline for the selected item (system cards, tabs, search, settings, apps). */
val FocusHighlightColor = Color(0xFF2DD4BF)

/**
 * Outline for a selected game: a lighter shade of its system's colour, so it still shows on the
 * tile, which is filled with that same colour.
 */
fun gameFocusColor(game: Game): Color {
    val systemColor = Color(SystemTileColors.forSystemId(game.systemId).base)
    return lerp(systemColor, Color.White, 0.45f)
}

/**
 * Makes the item that has controller/D-pad focus stand out: a thick bright outline and, for tiles,
 * a slight zoom. Put it on the outer container; it reacts when anything inside it has focus.
 * Touch use is unchanged, since touching doesn't move focus.
 */
fun Modifier.focusHighlight(
    shape: Shape = RoundedCornerShape(12.dp),
    color: Color = FocusHighlightColor,
    zoom: Float = 1.06f,
    width: Dp = 3.dp,
): Modifier =
    composed {
        var focused by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (focused) zoom else 1f,
            label = "focusZoom",
        )
        this
            .onFocusChanged { focused = it.hasFocus }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (focused) Modifier.border(width, color, shape) else Modifier)
    }
