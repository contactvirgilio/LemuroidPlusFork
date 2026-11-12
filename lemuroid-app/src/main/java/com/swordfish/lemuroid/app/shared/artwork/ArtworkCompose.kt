package com.swordfish.lemuroid.app.shared.artwork

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The "Show artwork" setting, watched once for the whole app. Every game tile reads it, so they
 * share this instead of each one registering its own listener.
 */
private object ShowArtworkSetting {
    var enabled by mutableStateOf(false)
        private set

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun ensureWatching(context: Context) {
        if (listener != null) return
        val appContext = context.applicationContext
        enabled = ArtworkManager.isEnabled(appContext)
        val created =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == ArtworkManager.SHOW_ARTWORK_KEY) enabled = ArtworkManager.isEnabled(appContext)
            }
        // Kept in a field: preferences only hold listeners weakly.
        listener = created
        ArtworkManager.preferences(appContext).registerOnSharedPreferenceChangeListener(created)
    }
}

/** Follows the "Show artwork" setting, updating when it changes. */
@Composable
fun rememberShowArtwork(): Boolean {
    val context = LocalContext.current
    remember { ShowArtworkSetting.ensureWatching(context) }
    return ShowArtworkSetting.enabled
}

/** The game's artwork when "Show artwork" is on and an image exists, otherwise null. */
@Composable
fun rememberGameArtwork(game: Game): Bitmap? {
    val context = LocalContext.current
    val enabled = rememberShowArtwork()
    // An image that is already loaded shows at once (no colour flash while scrolling); the
    // background check below still runs and corrects it if the file changed.
    val initial =
        remember(game.id, game.fileName, enabled) {
            if (enabled) ArtworkManager.peekArtwork(game) else null
        }
    val artwork by produceState(initialValue = initial, game.id, game.fileName, enabled) {
        value =
            if (enabled) {
                withContext(Dispatchers.IO) { ArtworkManager.loadArtwork(context.applicationContext, game) }
            } else {
                null
            }
    }
    return artwork
}
