package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.NeoGeoSets
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/**
 * "Cartridge view": instead of a colored tile, each game is drawn as a picture of its system's
 * media (cartridge, disc, arcade board...). The game's name stays under it, like on any tile.
 * Artwork is not used in this view.
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

private class Media(
    @DrawableRes val res: Int,
    @DrawableRes val label: Int? = null,
    /** Cartridges with a plastic shell can be painted; discs, boards and see-through shells can't. */
    val recolor: Boolean = false,
)

private val DEFAULT_MEDIA = Media(R.drawable.media_a2600, R.drawable.media_a2600_label, true)

private fun mediaFor(game: Game): Media {
    val system =
        SystemID.values().firstOrNull { it.dbname == game.systemId }
            ?: return DEFAULT_MEDIA
    return when (system) {
        SystemID.NES -> Media(R.drawable.media_nes, R.drawable.media_nes_label, true)
        SystemID.SNES -> Media(R.drawable.media_snes, R.drawable.media_snes_label, true)
        SystemID.GENESIS -> Media(R.drawable.media_genesis, R.drawable.media_genesis_label, true)
        SystemID.GB -> Media(R.drawable.media_gb, R.drawable.media_gb_label, true)
        SystemID.GBC -> Media(R.drawable.media_gbc, R.drawable.media_gbc_label, true)
        SystemID.GBA -> Media(R.drawable.media_gba, R.drawable.media_gba_label, true)
        SystemID.N64 -> Media(R.drawable.media_n64, R.drawable.media_n64_label, true)
        SystemID.SMS -> Media(R.drawable.media_sms, null, true)
        SystemID.PSP -> Media(R.drawable.media_psp)
        SystemID.NDS -> Media(R.drawable.media_nds, R.drawable.media_nds_label, true)
        SystemID.GG -> Media(R.drawable.media_gg, R.drawable.media_gg_label, true)
        SystemID.ATARI2600 -> DEFAULT_MEDIA
        SystemID.PSX -> Media(R.drawable.media_psx)
        SystemID.PC_ENGINE -> Media(R.drawable.media_pce, R.drawable.media_pce_label, true)
        SystemID.LYNX -> Media(R.drawable.media_lynx, R.drawable.media_lynx_label, true)
        SystemID.ATARI7800 -> Media(R.drawable.media_a7800, R.drawable.media_a7800_label, true)
        SystemID.SEGACD -> Media(R.drawable.media_segacd)
        SystemID.NGP -> Media(R.drawable.media_ngp, R.drawable.media_ngp_label, true)
        SystemID.NGC -> Media(R.drawable.media_ngc, R.drawable.media_ngc_label, true)
        SystemID.WS -> Media(R.drawable.media_ws, R.drawable.media_ws_label, true)
        SystemID.WSC -> Media(R.drawable.media_wsc)
        SystemID.DOS -> Media(R.drawable.media_dos, R.drawable.media_dos_label, true)
        SystemID.NINTENDO_3DS -> Media(R.drawable.media_3ds)
        SystemID.FBNEO, SystemID.MAME2003PLUS ->
            if (NeoGeoSets.isNeoGeo(game.fileName)) {
                Media(R.drawable.media_neogeo, R.drawable.media_neogeo_label, true)
            } else {
                Media(R.drawable.media_arcade)
            }
    }
}

/** Whether this game's cartridge can be given a color. */
fun isCartridgeColorable(game: Game): Boolean = mediaFor(game).recolor

@Composable
fun GameMediaImage(
    modifier: Modifier = Modifier,
    game: Game,
) {
    GameMediaImage(modifier = modifier, game = game, style = rememberCartridgeStyle(game))
}

/** The game's media drawn in [style] (null = the original picture). */
@Composable
fun GameMediaImage(
    modifier: Modifier = Modifier,
    game: Game,
    style: CartridgeStyle?,
) {
    val media = remember(game.systemId, game.fileName) { mediaFor(game) }
    if (style == null || !media.recolor) {
        Image(
            painter = painterResource(media.res),
            contentDescription = game.displayName(),
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
        return
    }

    val context = LocalContext.current
    val shell = remember(media.res, style) { CartridgeRecolor.shell(context, media.res, media.label, style) }
    val label = remember(media.label) { media.label?.let { CartridgeRecolor.decode(context, it) } }
    // Sparkly cartridges: a soft band of light drifts slowly across the shell.
    val shimmer: State<Float>? =
        if (style.sparkly) {
            rememberInfiniteTransition(label = "shimmer").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(durationMillis = 7000, easing = LinearEasing), RepeatMode.Restart),
                label = "shimmerPhase",
            )
        } else {
            null
        }

    Canvas(modifier = modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val scale = minOf(size.width / shell.width, size.height / shell.height)
        val dw = (shell.width * scale).toInt()
        val dh = (shell.height * scale).toInt()
        val dx = ((size.width - dw) / 2f).toInt()
        val dy = ((size.height - dh) / 2f).toInt()
        val offset = IntOffset(dx, dy)
        val dst = IntSize(dw, dh)

        drawImage(shell, dstOffset = offset, dstSize = dst, filterQuality = FilterQuality.High)
        if (shimmer != null) {
            val band = dw * 0.55f
            val x = dx + (-band + (dw + band * 2f) * shimmer.value)
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                        start = Offset(x, dy.toFloat()),
                        end = Offset(x + band, dy + band * 0.6f),
                    ),
                topLeft = Offset(dx.toFloat(), dy.toFloat()),
                size = Size(dw.toFloat(), dh.toFloat()),
                blendMode = BlendMode.SrcAtop,
            )
        }
        // The label keeps its own pixels, whatever the shell color or shimmer.
        if (label != null) {
            drawImage(label, dstOffset = offset, dstSize = dst, filterQuality = FilterQuality.High)
        }
    }
}
