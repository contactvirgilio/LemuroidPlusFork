package com.swordfish.lemuroid.app.mobile.feature.settings.general

import com.swordfish.lemuroid.app.mobile.shared.compose.ui.focusHighlight
import com.swordfish.lemuroid.app.mobile.feature.games.GamesViewMode
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.height
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.GameShellSkin
import com.swordfish.lemuroid.app.shared.tiles.SystemTileColors
import com.swordfish.lemuroid.lib.library.SystemID

/** The six shell colours as tiny shells, under the Shell skins switch; faded while it is off. */
@Composable
fun ShellSkinsPreview(enabled: Boolean) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .alpha(if (enabled) 1f else 0.4f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GameShellSkin.shells.forEach { shell ->
            Canvas(modifier = Modifier.size(width = 30.dp, height = 46.dp)) {
                val r = CornerRadius(5.dp.toPx())
                drawRoundRect(Color(shell.color), cornerRadius = r)
                drawRoundRect(
                    Color(shell.bezel),
                    topLeft = Offset(size.width * 0.15f, size.height * 0.1f),
                    size = Size(size.width * 0.7f, size.height * 0.36f),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }
    }
}

/** Two little pads to pick the touch pad skin; the chosen one has a teal outline. */
@Composable
fun PadSkinPicker(
    outline: Boolean,
    onPick: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.settings_title_pad_skin),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PadCard(selected = !outline, label = stringResource(R.string.settings_pad_skin_1), onClick = { onPick(false) }) {
                drawClassicPad()
            }
            PadCard(selected = outline, label = stringResource(R.string.settings_pad_skin_2), onClick = { onPick(true) }) {
                drawOutlinePad()
            }
        }
    }
}

@Composable
private fun PadCard(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    draw: DrawScope.() -> Unit,
) {
    ChoiceCard(selected, label, onClick) {
        Canvas(modifier = Modifier.fillMaxSize()) { draw() }
    }
}

/** A small tappable preview with a label; the chosen one has a teal outline. */
@Composable
private fun ChoiceCard(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(width = 120.dp, height = 84.dp),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                modifier
                    .clip(shape)
                    .background(Color.Black)
                    .border(if (selected) 2.dp else 0.5.dp, if (selected) TEAL else Color(0xFF444441), shape)
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) TEAL else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * How the library looks, all in one place (2026-11-20): game style (tiles or cartridges), layout
 * of a system's games, and colours (original or pastel), with a live preview on top.
 */
@Composable
fun LibraryLookPicker(
    cartridges: Boolean,
    pastel: Boolean,
    layout: GamesViewMode,
    onCartridges: (Boolean) -> Unit,
    onPastel: (Boolean) -> Unit,
    onLayout: (GamesViewMode) -> Unit,
) {
    val colors = SystemTileColors.previewColors(pastel, PREVIEW_SYSTEMS).map { Color(it) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        LibraryPreview(cartridges, colors, layout)

        PickerRow(
            title = stringResource(R.string.settings_title_game_style),
            options = listOf(stringResource(R.string.settings_game_style_tiles), stringResource(R.string.settings_game_style_media)),
            selected = if (cartridges) 1 else 0,
            onPick = { onCartridges(it == 1) },
        )
        val layouts = listOf(GamesViewMode.LIST, GamesViewMode.GRID_SMALL, GamesViewMode.GRID_LARGE, GamesViewMode.CAROUSEL)
        PickerRow(
            title = stringResource(R.string.settings_title_layout),
            options =
                listOf(
                    stringResource(R.string.games_view_list),
                    stringResource(R.string.games_view_grid_small),
                    stringResource(R.string.games_view_grid_large),
                    stringResource(R.string.games_view_carousel),
                ),
            selected = layouts.indexOf(layout).coerceAtLeast(0),
            onPick = { onLayout(layouts[it]) },
        )
        PickerRow(
            title = stringResource(R.string.settings_title_colours),
            options = listOf(stringResource(R.string.settings_colours_original), stringResource(R.string.settings_colours_pastel)),
            selected = if (pastel) 1 else 0,
            onPick = { onPastel(it == 1) },
        )
    }
}

@Composable
private fun PickerRow(
    title: String,
    options: List<String>,
    selected: Int,
    onPick: (Int) -> Unit,
) {
    Text(
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(Color(0xFF1D2126))
                        .border(if (on) 2.dp else 0.5.dp, if (on) TEAL else Color(0xFF444441))
                        .focusHighlight(shape = androidx.compose.ui.graphics.RectangleShape, zoom = 1f)
                        .clickable { onPick(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (on) TEAL else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A small live picture of the library with the current choices. */
@Composable
private fun LibraryPreview(
    cartridges: Boolean,
    colors: List<Color>,
    layout: GamesViewMode,
) {
    @Composable
    fun Item(
        index: Int,
        size: androidx.compose.ui.unit.Dp,
        alpha: Float = 1f,
    ) {
        if (cartridges) {
            val res = listOf(R.drawable.media_snes, R.drawable.media_gba, R.drawable.media_genesis, R.drawable.media_psx, R.drawable.media_gb, R.drawable.media_n64)
            Image(
                painter = painterResource(res[index % res.size]),
                contentDescription = null,
                modifier = Modifier.size(size).alpha(alpha),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(modifier = Modifier.size(size).alpha(alpha).background(colors[index % colors.size], RoundedCornerShape(4.dp)))
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.Black)
                .border(0.5.dp, Color(0xFF444441)),
        contentAlignment = Alignment.Center,
    ) {
        when (layout) {
            GamesViewMode.LIST ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { i ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Item(i, 30.dp)
                            Box(modifier = Modifier.size(width = 120.dp, height = 8.dp).background(Color(0xFF3A3F45)))
                        }
                    }
                }
            GamesViewMode.GRID_SMALL ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(2) { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(6) { Item(row * 6 + it, 38.dp) } } }
                }
            GamesViewMode.GRID_LARGE ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { repeat(4) { Item(it, 64.dp) } }
            GamesViewMode.CAROUSEL ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Item(0, 52.dp, 0.4f)
                    Item(1, 100.dp)
                    Item(2, 52.dp, 0.4f)
                }
        }
    }
}

private fun DrawScope.drawClassicPad() {
    val fill = Color.White.copy(alpha = 0.35f)
    drawRoundRect(Color(0xFF5F5E5A).copy(alpha = 0.4f), Offset(6.dp.toPx(), 6.dp.toPx()), Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()), CornerRadius(8.dp.toPx()))
    val cx = size.width * 0.27f
    val cy = size.height * 0.52f
    drawCircle(fill, radius = size.height * 0.24f, center = Offset(cx, cy))
    val arm = size.height * 0.2f
    val thick = size.height * 0.1f
    drawRect(Color.White.copy(alpha = 0.6f), Offset(cx - thick / 2, cy - arm), Size(thick, arm * 2))
    drawRect(Color.White.copy(alpha = 0.6f), Offset(cx - arm, cy - thick / 2), Size(arm * 2, thick))
    drawCircle(Color.White.copy(alpha = 0.5f), radius = size.height * 0.12f, center = Offset(size.width * 0.68f, size.height * 0.62f))
    drawCircle(Color.White.copy(alpha = 0.5f), radius = size.height * 0.12f, center = Offset(size.width * 0.84f, size.height * 0.42f))
}

private fun DrawScope.drawOutlinePad() {
    val line = Color(0xFF9A9A96)
    val stroke = Stroke(width = 2.dp.toPx())
    val cx = size.width * 0.27f
    val cy = size.height * 0.52f
    val a = size.height * 0.1f
    val b = size.height * 0.3f
    val plus =
        Path().apply {
            moveTo(cx - a, cy - b)
            lineTo(cx + a, cy - b)
            lineTo(cx + a, cy - a)
            lineTo(cx + b, cy - a)
            lineTo(cx + b, cy + a)
            lineTo(cx + a, cy + a)
            lineTo(cx + a, cy + b)
            lineTo(cx - a, cy + b)
            lineTo(cx - a, cy + a)
            lineTo(cx - b, cy + a)
            lineTo(cx - b, cy - a)
            lineTo(cx - a, cy - a)
            close()
        }
    drawPath(plus, line, style = stroke)
    drawCircle(line, radius = size.height * 0.12f, center = Offset(size.width * 0.68f, size.height * 0.62f), style = stroke)
    drawCircle(line, radius = size.height * 0.12f, center = Offset(size.width * 0.84f, size.height * 0.42f), style = stroke)
}

/** In-game shortcuts: always in a row, or behind one grid button. Each card is a tiny game screen. */
@Composable
fun ShortcutsPicker(
    nested: Boolean,
    onPick: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.settings_title_nested_shortcuts),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val card = Modifier.fillMaxWidth().height(84.dp)
            Box(modifier = Modifier.weight(1f)) {
                ChoiceCard(!nested, stringResource(R.string.settings_shortcuts_row), { onPick(false) }, card) {
                    MiniGameScreen {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            ROW_ICONS.forEach { MiniShortcut(it) }
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                ChoiceCard(nested, stringResource(R.string.settings_shortcuts_nested), { onPick(true) }, card) {
                    MiniGameScreen { MiniShortcut(Icons.Filled.Apps) }
                }
            }
        }
    }
}

private val ROW_ICONS =
    listOf(
        Icons.Outlined.Tv,
        Icons.Outlined.Crop,
        Icons.Outlined.BlurOn,
        Icons.Outlined.Save,
        Icons.Outlined.FastForward,
        Icons.Outlined.VolumeUp,
        Icons.Filled.Settings,
    )

/** A tiny game screen: dark grey picture with the shortcuts at the top right. */
@Composable
private fun MiniGameScreen(shortcuts: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(top = 12.dp)
                    .fillMaxWidth(0.62f)
                    .fillMaxHeight(0.7f)
                    .background(Color(0xFF5F5E5A), RoundedCornerShape(2.dp)),
        )
        Box(modifier = Modifier.align(Alignment.TopEnd)) { shortcuts() }
    }
}

@Composable
private fun MiniShortcut(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier.size(12.dp).background(Color(0xFF2C2C2A), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(8.dp),
        )
    }
}

private val PREVIEW_SYSTEMS = listOf(SystemID.SNES, SystemID.GBA, SystemID.GENESIS, SystemID.PSX, SystemID.GB, SystemID.N64)

private val TEAL = Color(0xFF5DCAA5)
