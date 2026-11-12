package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.app.shared.game.NeoGeoSets
import com.swordfish.lemuroid.app.shared.tiles.GameTileUtils
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/**
 * "Cartridge view": instead of a colored tile, each game is drawn as a generic piece of media for
 * its system (cartridge, disc, arcade board...) in the system's color, with the game's initials on
 * the label. Artwork is not used in this view.
 */
object MediaViewSetting {
    const val KEY = "pref_key_media_view"

    var enabled by mutableStateOf(false)
        private set

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun ensureWatching(context: Context) {
        if (listener != null) return
        val preferences = SharedPreferencesHelper.getSharedPreferences(context.applicationContext)
        enabled = preferences.getBoolean(KEY, false)
        val created =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY) enabled = preferences.getBoolean(KEY, false)
            }
        // Kept in a field: preferences only hold listeners weakly.
        listener = created
        preferences.registerOnSharedPreferenceChangeListener(created)
    }

    fun set(
        context: Context,
        value: Boolean,
    ) {
        ensureWatching(context)
        enabled = value
        SharedPreferencesHelper.getSharedPreferences(context.applicationContext)
            .edit().putBoolean(KEY, value).apply()
    }
}

@Composable
fun rememberMediaView(): Boolean {
    val context = LocalContext.current
    remember { MediaViewSetting.ensureWatching(context) }
    return MediaViewSetting.enabled
}

private enum class MediaShape {
    HOME_CART,
    TALL_CART,
    ROUND_CART,
    STEP_CART,
    DARK_CART,
    HANDHELD_CART,
    WIDE_CART,
    CARD,
    DISC,
    BOARD,
    BIG_CART,
}

private fun shapeFor(game: Game): MediaShape {
    val system = SystemID.values().firstOrNull { it.dbname == game.systemId } ?: return MediaShape.HOME_CART
    return when (system) {
        SystemID.NES -> MediaShape.TALL_CART
        SystemID.SNES -> MediaShape.ROUND_CART
        SystemID.N64 -> MediaShape.STEP_CART
        SystemID.GENESIS, SystemID.SMS -> MediaShape.DARK_CART
        SystemID.GB, SystemID.GBC, SystemID.GG, SystemID.LYNX,
        SystemID.NGP, SystemID.NGC, SystemID.WS, SystemID.WSC,
        -> MediaShape.HANDHELD_CART
        SystemID.GBA -> MediaShape.WIDE_CART
        SystemID.NDS, SystemID.NINTENDO_3DS, SystemID.PC_ENGINE -> MediaShape.CARD
        SystemID.PSX, SystemID.PSP, SystemID.SEGACD, SystemID.DOS -> MediaShape.DISC
        SystemID.ATARI2600, SystemID.ATARI7800 -> MediaShape.HOME_CART
        SystemID.FBNEO, SystemID.MAME2003PLUS ->
            if (NeoGeoSets.isNeoGeo(game.fileName)) MediaShape.BIG_CART else MediaShape.BOARD
    }
}

private val SHELL = Color(0xFF2A2E33)
private val SHELL_LIGHT = Color(0xFF3A3F45)
private val GOLD = Color(0xFFC9A227)
private val PCB = Color(0xFF2F6B4A)
private val CHIP = Color(0xFF1B1F22)
private val HOLE = Color(0xFF0F1418)

private class MediaColors(val base: Color) {
    val light = lerp(base, Color.White, 0.6f)
    val shade = lerp(base, Color.Black, 0.22f)
    val ink = lerp(base, Color.Black, 0.7f)
}

@Composable
fun GameMediaImage(
    modifier: Modifier = Modifier,
    game: Game,
) {
    val shape = remember(game.systemId, game.fileName) { shapeFor(game) }
    val initials = remember(game) { GameTileUtils.initials(game) }
    val base = GameTileUtils.getColors(game).base
    val colors = remember(base) { MediaColors(Color(base)) }
    val paint =
        remember {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
        }

    Canvas(modifier = modifier) {
        // Everything is drawn in a 100 x 100 box, scaled to the tile and centered.
        val unit = size.minDimension / 100f
        val left = (size.width - 100f * unit) / 2f
        val top = (size.height - 100f * unit) / 2f
        withTransform({
            translate(left, top)
            scale(unit, unit, pivot = Offset.Zero)
        }) {
            when (shape) {
                MediaShape.HOME_CART -> homeCart(colors, initials, paint)
                MediaShape.TALL_CART -> tallCart(colors, initials, paint)
                MediaShape.ROUND_CART -> roundCart(colors, initials, paint)
                MediaShape.STEP_CART -> stepCart(colors, initials, paint)
                MediaShape.DARK_CART -> darkCart(colors, initials, paint)
                MediaShape.HANDHELD_CART -> handheldCart(colors, initials, paint)
                MediaShape.WIDE_CART -> wideCart(colors, initials, paint)
                MediaShape.CARD -> card(colors, initials, paint)
                MediaShape.DISC -> disc(colors, initials, paint)
                MediaShape.BOARD -> board(colors, initials, paint)
                MediaShape.BIG_CART -> bigCart(colors, initials, paint)
            }
        }
    }
}

private fun DrawScope.box(
    color: Color,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    r: Float = 0f,
) {
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r, r))
}

/** Initials centered on ([cx], [cy]), shrunk when they would be wider than [maxWidth]. */
private fun DrawScope.label(
    text: String,
    paint: Paint,
    color: Color,
    cx: Float,
    cy: Float,
    maxWidth: Float,
    textSize: Float = 17f,
) {
    paint.color = color.toArgb()
    paint.textSize = textSize
    val measured = paint.measureText(text)
    if (measured > maxWidth && measured > 0f) paint.textSize = textSize * maxWidth / measured
    val baseline = cy - (paint.descent() + paint.ascent()) / 2f
    drawIntoCanvas { it.nativeCanvas.drawText(text, cx, baseline, paint) }
}

private fun DrawScope.homeCart(
    c: MediaColors,
    text: String,
    paint: Paint,
) {
    val body =
        Path().apply {
            moveTo(18f, 20f); lineTo(82f, 20f); lineTo(82f, 32f); lineTo(88f, 32f)
            lineTo(88f, 82f); lineTo(12f, 82f); lineTo(12f, 32f); lineTo(18f, 32f); close()
        }
    drawPath(body, c.base)
    box(c.light, 24f, 28f, 52f, 38f, 3f)
    label(text, paint, c.ink, 50f, 47f, 44f)
    box(c.shade, 24f, 72f, 52f, 4f)
}

private fun DrawScope.tallCart(
    c: MediaColors,
    text: String,
    paint: Paint,
) {
    box(c.base, 20f, 10f, 60f, 80f, 3f)
    box(c.light, 26f, 16f, 48f, 44f, 2f)
    label(text, paint, c.ink, 50f, 38f, 40f)
    box(c.shade, 26f, 68f, 20f, 16f)
    box(c.shade, 52f, 72f, 22f, 3f)
    box(c.shade, 52f, 79f, 22f, 3f)
}

private fun DrawScope.roundCart(
    c: MediaColors,
    text: String,
    paint: Paint,
) {
    val body =
        Path().apply {
            moveTo(10f, 82f); lineTo(10f, 38f)
            quadraticBezierTo(10f, 30f, 18f, 27f)
            quadraticBezierTo(50f, 14f, 82f, 27f)
            quadraticBezierTo(90f, 30f, 90f, 38f)
            lineTo(90f, 82f); close()
        }
    drawPath(body, c.base)
    box(c.light, 22f, 32f, 56f, 32f, 9f)
    label(text, paint, c.ink, 50f, 48f, 46f)
    box(c.shade, 16f, 70f, 10f, 6f, 2f)
    box(c.shade, 74f, 70f, 10f, 6f, 2f)
    box(c.shade, 32f, 71f, 36f, 4f)
}

private fun DrawScope.stepCart(
    c: MediaColors,
    text: String,
    paint: Paint,
) {
    box(c.base, 14f, 10f, 72f, 54f, 9f)
    box(c.base, 14f, 40f, 72f, 24f)
    box(c.base, 19f, 62f, 62f, 26f)
    box(c.light, 24f, 18f, 52f, 38f, 3f)
    label(text, paint, c.ink, 50f, 37f, 44f)
    box(c.shade, 26f, 70f, 48f, 4f)
    box(c.shade, 26f, 79f, 48f, 4f)
}

private fun DrawScope.darkCart(
    c: MediaColors,
    text: String,
    paint: Paint,
) {
    box(c.base, 14f, 16f, 72f, 60f, 16f)
    box(c.base, 14f, 50f, 72f, 36f)
    box(SHELL, 16f, 18f, 68f, 58f, 14f)
    box(SHELL, 16f, 50f, 68f, 34f)
    box(c.base, 25f, 28f, 50f, 36f, 3f)
    label(text, paint, c.ink, 50f, 46f, 42f)
    box(SHELL_LIGHT, 30f, 72f, 40f, 4f)
}

private fun DrawScope.handheldCart(
    c: MediaColors,
    text: String,
    paint: Paint,
) {
    val body =
        Path().apply {
            moveTo(24f, 8f); lineTo(76f, 8f)
            quadraticBezierTo(82f, 8f, 82f, 14f)
            lineTo(82f, 80f); lineTo(70f, 92f); lineTo(24f, 92f)
            quadraticBezierTo(18f, 92f, 18f, 86f)
            lineTo(18f, 14f)
            quadraticBezierTo(18f, 8f, 24f, 8f); close()
        }
    drawPath(body, c.base)
    box(c.shade, 25f, 15f, 50f, 7f, 2f)
    box(c.light, 26f, 30f, 48f, 44f, 3f)
    label(text, paint, c.ink, 50f, 52f, 40f)
}

private fun DrawScope.wideCart(
    c: MediaColors,
    text: String,
    paint: Paint,
) {
    box(c.base, 8f, 26f, 84f, 50f, 5f)
    box(c.light, 15f, 34f, 70f, 30f, 3f)
    label(text, paint, c.ink, 50f, 49f, 60f)
    box(c.shade, 30f, 68f, 40f, 3f)
}

private fun DrawScope.card(
    c: MediaColors,
    text: String,
    paint: Paint,
) {
    val body =
        Path().apply {
            moveTo(22f, 18f); lineTo(70f, 18f); lineTo(82f, 30f); lineTo(82f, 78f)
            quadraticBezierTo(82f, 82f, 78f, 82f)
            lineTo(22f, 82f)
            quadraticBezierTo(18f, 82f, 18f, 78f)
            lineTo(18f, 22f)
            quadraticBezierTo(18f, 18f, 22f, 18f); close()
        }
    drawPath(body, c.base)
    box(c.light, 24f, 32f, 50f, 32f, 2f)
    label(text, paint, c.ink, 49f, 48f, 42f, 15f)
    box(c.shade, 26f, 70f, 46f, 6f)
}

private fun DrawScope.disc(
    c: MediaColors,
    text: String,
    paint: Paint,
) {
    val center = Offset(50f, 50f)
    drawCircle(c.base, 42f, center)
    drawCircle(c.light, 41f, center, style = Stroke(width = 2f))
    drawCircle(c.light, 13f, center)
    drawCircle(HOLE, 5.5f, center)
    label(text, paint, c.ink, 50f, 24f, 44f, 14f)
}

private fun DrawScope.board(
    c: MediaColors,
    text: String,
    paint: Paint,
) {
    box(PCB, 6f, 20f, 88f, 56f, 3f)
    box(c.base, 12f, 26f, 38f, 24f, 2f)
    label(text, paint, c.ink, 31f, 38f, 32f, 13f)
    box(CHIP, 56f, 26f, 32f, 9f)
    box(CHIP, 56f, 40f, 32f, 9f)
    box(CHIP, 12f, 56f, 76f, 12f)
    box(GOLD, 22f, 76f, 56f, 8f)
}

private fun DrawScope.bigCart(
    c: MediaColors,
    text: String,
    paint: Paint,
) {
    box(c.base, 2f, 18f, 96f, 60f, 6f)
    box(SHELL, 4f, 20f, 92f, 56f, 5f)
    box(SHELL_LIGHT, 8f, 27f, 5f, 42f, 2f)
    box(SHELL_LIGHT, 87f, 27f, 5f, 42f, 2f)
    box(c.base, 19f, 26f, 62f, 36f, 3f)
    label(text, paint, c.ink, 50f, 44f, 52f, 19f)
    box(SHELL_LIGHT, 26f, 67f, 48f, 4f, 2f)
    box(GOLD, 16f, 78f, 68f, 6f)
}
