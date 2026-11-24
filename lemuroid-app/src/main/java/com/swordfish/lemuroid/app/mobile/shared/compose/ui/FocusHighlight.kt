package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.Outline
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
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
val FocusHighlightColor: Color get() = SelectionStyle.color

/**
 * The colour of the selected item everywhere (pulsing fill, menu accents, page dots). Chosen in
 * Settings; Pink by default (owner, 2026-11-24).
 */
object SelectionStyle {
    const val KEY = "selection_color"
    const val DEFAULT = "Pink"

    /** Name to colour. Names stay English in every language, like the filters and skins. */
    val options: List<Pair<String, Color>> =
        listOf(
            "Pink" to Color(0xFFED93B1),
            "Peach" to Color(0xFFFFB796),
            "Mint" to Color(0xFF98E1BE),
            "Sky" to Color(0xFF8CC8F5),
            "Lavender" to Color(0xFFBEA5F0),
            "Lemon" to Color(0xFFFAE68C),
            "Teal" to Color(0xFF2DD4BF),
            "Gold" to Color(0xFFFFC107),
            "Red" to Color(0xFFF05A5A),
            "Orange" to Color(0xFFFF9632),
            "White" to Color(0xFFEBEBF0),
            "Violet" to Color(0xFF965AE6),
        )

    var name: String by mutableStateOf(DEFAULT)
        private set

    val color: Color get() = options.firstOrNull { it.first == name }?.second ?: options[0].second

    fun load(context: android.content.Context) {
        name =
            androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(KEY, DEFAULT) ?: DEFAULT
    }

    fun set(context: android.content.Context, newName: String) {
        name = newName
        androidx.preference.PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putString(KEY, newName)
            .apply()
    }
}

/** The outline color of the screen being shown; the game menu uses gold like the DKC / Tetris menus. */
val LocalFocusHighlightColor = androidx.compose.runtime.compositionLocalOf { Color.Unspecified }

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
            .then(if (focused) Modifier.pulsingSelection(shape) else Modifier)
    }

/**
 * A soft pulsing fill of the selection colour over the item (no outline): it fades between a faint
 * and a clearer tint about once every 1.2 seconds. Only composed while the item is selected, so
 * nothing animates otherwise.
 */
fun Modifier.pulsingSelection(shape: Shape = RoundedCornerShape(12.dp)): Modifier =
    composed {
        val pulse by rememberInfiniteTransition(label = "selectionPulse").animateFloat(
            initialValue = 0.16f,
            targetValue = 0.42f,
            animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "selectionPulseAlpha",
        )
        val tint = SelectionStyle.color
        drawWithContent {
            drawContent()
            when (val outline = shape.createOutline(size, layoutDirection, this)) {
                is Outline.Rectangle -> drawRect(tint, outline.rect.topLeft, outline.rect.size, alpha = pulse)
                is Outline.Rounded ->
                    drawRoundRect(
                        tint,
                        Offset(outline.roundRect.left, outline.roundRect.top),
                        androidx.compose.ui.geometry.Size(outline.roundRect.width, outline.roundRect.height),
                        outline.roundRect.topLeftCornerRadius,
                        alpha = pulse,
                    )
                is Outline.Generic -> drawPath(outline.path, tint, alpha = pulse)
            }
        }
    }
