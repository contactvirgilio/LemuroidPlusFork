package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

@DrawableRes
private fun mediaFor(game: Game): Int {
    val system =
        SystemID.values().firstOrNull { it.dbname == game.systemId }
            ?: return R.drawable.media_a2600
    return when (system) {
        SystemID.NES -> R.drawable.media_nes
        SystemID.SNES -> R.drawable.media_snes
        SystemID.GENESIS -> R.drawable.media_genesis
        SystemID.GB -> R.drawable.media_gb
        SystemID.GBC -> R.drawable.media_gbc
        SystemID.GBA -> R.drawable.media_gba
        SystemID.N64 -> R.drawable.media_n64
        SystemID.SMS -> R.drawable.media_sms
        SystemID.PSP -> R.drawable.media_psp
        SystemID.NDS -> R.drawable.media_nds
        SystemID.GG -> R.drawable.media_gg
        SystemID.ATARI2600 -> R.drawable.media_a2600
        SystemID.PSX -> R.drawable.media_psx
        SystemID.PC_ENGINE -> R.drawable.media_pce
        SystemID.LYNX -> R.drawable.media_lynx
        SystemID.ATARI7800 -> R.drawable.media_a7800
        SystemID.SEGACD -> R.drawable.media_segacd
        SystemID.NGP -> R.drawable.media_ngp
        SystemID.NGC -> R.drawable.media_ngc
        SystemID.WS -> R.drawable.media_ws
        SystemID.WSC -> R.drawable.media_wsc
        SystemID.DOS -> R.drawable.media_dos
        SystemID.NINTENDO_3DS -> R.drawable.media_3ds
        SystemID.FBNEO, SystemID.MAME2003PLUS ->
            if (NeoGeoSets.isNeoGeo(game.fileName)) R.drawable.media_neogeo else R.drawable.media_arcade
    }
}

@Composable
fun GameMediaImage(
    modifier: Modifier = Modifier,
    game: Game,
) {
    val media = remember(game.systemId, game.fileName) { mediaFor(game) }
    Image(
        painter = painterResource(media),
        contentDescription = game.displayName(),
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
