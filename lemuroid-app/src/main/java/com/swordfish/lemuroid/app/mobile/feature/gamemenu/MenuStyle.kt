package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.R

/** Colors and shapes of the in-game menu, the same card style as the Tetris DX and DKC menus. */
object MenuColors {
    val Card = Color(0xFF0A0C0E).copy(alpha = 0.82f)
    val Tile = Color(0xFF1E2226)
    val TileBorder = Color(0x26FFFFFF)
    val Gold = Color(0xFFFFC107)
    val Title = Color(0xFF9AA4AD)
    val Label = Color(0xFFC8CDD2)
}

/** True when a gamepad is connected, so the menu shows its button hints and starts with focus. */
val LocalMenuHasGamepad = compositionLocalOf { false }

/**
 * The menu card: light dimming behind it, a dark rounded card in the middle with a small spaced
 * title on top. Touching outside closes it.
 */
@Composable
fun MenuCard(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.27f))
                .pointerInput(Unit) { detectTapGestures { onClose() } }
                .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        val cardWidth = minOf(maxWidth * 0.94f, 640.dp)
        val cardHeight = minOf(maxHeight * 0.94f, 440.dp)
        Column(
            modifier =
                Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MenuColors.Card)
                    // Taps on the card itself must not close the menu.
                    .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = if (canGoBack) onBack else onClose) {
                    Icon(
                        if (canGoBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Close,
                        contentDescription = stringResource(if (canGoBack) R.string.back else R.string.close),
                        tint = MenuColors.Title,
                    )
                }
                Text(
                    text = title.uppercase(),
                    color = MenuColors.Title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.3.sp,
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) { content() }
            if (LocalMenuHasGamepad.current) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
                    text = "A  Choose        B  Back        ◀ ▶ ▲ ▼  Select",
                    color = MenuColors.Title,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

/** A rounded tile; the one with controller focus gets the thick gold outline. */
@Composable
fun MenuTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    content: @Composable (focused: Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(MenuColors.Tile)
                .border(
                    if (focused) BorderStroke(3.dp, MenuColors.Gold) else BorderStroke(1.dp, MenuColors.TileBorder),
                    shape,
                )
                .clickable(enabled = enabled, interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content(focused)
    }
}

/** Icon over a label (and an optional small line), used by the tiles of the menu home. */
@Composable
fun MenuTileLabel(
    icon: @Composable (tint: Color) -> Unit,
    label: String,
    focused: Boolean,
    sublabel: String? = null,
    active: Boolean = false,
) {
    val tint = if (active) MenuColors.Gold else if (focused) Color.White else MenuColors.Label
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        icon(tint)
        Text(
            text = label,
            color = tint,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        if (sublabel != null) {
            Text(
                text = sublabel,
                color = MenuColors.Title,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * Makes the screens inside the menu (the core settings) sit on the dark card: no opaque
 * surfaces, light text and the gold accent.
 */
@Composable
fun MenuTheme(content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    androidx.compose.runtime.CompositionLocalProvider(
        com.swordfish.lemuroid.app.mobile.shared.compose.ui.LocalFocusHighlightColor provides MenuColors.Gold,
    ) {
        MaterialTheme(
            colorScheme =
                base.copy(
                    surface = Color.Transparent,
                    background = Color.Transparent,
                    surfaceVariant = MenuColors.Tile,
                    surfaceContainer = Color.Transparent,
                    surfaceContainerHigh = MenuColors.Tile,
                    surfaceContainerHighest = MenuColors.Tile,
                    onSurface = MenuColors.Label,
                    onBackground = MenuColors.Label,
                    onSurfaceVariant = MenuColors.Title,
                    primary = MenuColors.Gold,
                    onPrimary = Color(0xFF1E1804),
                ),
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}
