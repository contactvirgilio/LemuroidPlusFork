package com.swordfish.lemuroid.app.mobile.feature.settings.general

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
    val shape = RoundedCornerShape(10.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier =
                Modifier
                    .size(width = 120.dp, height = 84.dp)
                    .clip(shape)
                    .border(if (selected) 2.dp else 0.5.dp, if (selected) TEAL else Color(0xFF444441), shape)
                    .clickable(onClick = onClick),
        ) {
            drawRect(Color.Black)
            draw()
        }
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) TEAL else MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

/** Two little tile walls to pick normal or soft tile colors; the chosen one has a teal outline. */
@Composable
fun TileColorsPicker(
    soft: Boolean,
    onPick: (Boolean) -> Unit,
) {
    val normal = SystemTileColors.previewColors(false, PREVIEW_SYSTEMS)
    val pastel = SystemTileColors.previewColors(true, PREVIEW_SYSTEMS)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.settings_title_pastel_tiles),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PadCard(selected = !soft, label = stringResource(R.string.settings_tiles_normal), onClick = { onPick(false) }) {
                drawTileWall(normal)
            }
            PadCard(selected = soft, label = stringResource(R.string.settings_tiles_soft), onClick = { onPick(true) }) {
                drawTileWall(pastel)
            }
        }
    }
}

private val PREVIEW_SYSTEMS = listOf(SystemID.SNES, SystemID.GBA, SystemID.GENESIS, SystemID.PSX, SystemID.GB, SystemID.N64)

private fun DrawScope.drawTileWall(colors: List<Int>) {
    val cols = 3
    val rows = 2
    val gap = 6.dp.toPx()
    val w = (size.width - gap * (cols + 1)) / cols
    val h = (size.height - gap * (rows + 1)) / rows
    colors.forEachIndexed { i, c ->
        val x = gap + (i % cols) * (w + gap)
        val y = gap + (i / cols) * (h + gap)
        drawRoundRect(Color(c), Offset(x, y), Size(w, h), CornerRadius(5.dp.toPx()))
    }
}

private val TEAL = Color(0xFF5DCAA5)
