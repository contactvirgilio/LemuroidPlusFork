package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
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

/** The outline color of the screen being shown; the game menu uses gold like the DKC / Tetris menus. */
val LocalFocusHighlightColor = androidx.compose.runtime.compositionLocalOf { FocusHighlightColor }

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
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.focusHighlight(
    shape: Shape = RoundedCornerShape(12.dp),
    color: Color? = null,
    zoom: Float = 1.06f,
    width: Dp = 3.dp,
): Modifier =
    composed {
        val outline = color ?: LocalFocusHighlightColor.current
        var focused by remember { mutableStateOf(false) }
        // Scrolling stops as soon as the tile is on screen, which would cut off the outline and the
        // zoom at the edge. Ask for a bit of room around the tile too.
        val requester = remember { BringIntoViewRequester() }
        val scope = rememberCoroutineScope()
        var itemSize by remember { mutableStateOf(IntSize.Zero) }
        val marginPx = with(LocalDensity.current) { (width + 10.dp).toPx() }
        val scale by animateFloatAsState(
            targetValue = if (focused) zoom else 1f,
            label = "focusZoom",
        )
        this
            .bringIntoViewRequester(requester)
            .onSizeChanged { itemSize = it }
            .onFocusChanged {
                focused = it.hasFocus
                if (it.hasFocus) {
                    scope.launch {
                        requester.bringIntoView(
                            Rect(-marginPx, -marginPx, itemSize.width + marginPx, itemSize.height + marginPx),
                        )
                    }
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (focused) Modifier.border(width, outline, shape) else Modifier)
    }
