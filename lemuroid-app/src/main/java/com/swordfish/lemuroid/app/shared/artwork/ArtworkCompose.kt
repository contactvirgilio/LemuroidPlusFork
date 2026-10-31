package com.swordfish.lemuroid.app.shared.artwork

import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Follows the "Show artwork" setting, updating when it changes. */
@Composable
fun rememberShowArtwork(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(ArtworkManager.isEnabled(context)) }

    DisposableEffect(Unit) {
        val preferences = ArtworkManager.preferences(context)
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == ArtworkManager.SHOW_ARTWORK_KEY) enabled = ArtworkManager.isEnabled(context)
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return enabled
}

/** The game's artwork when "Show artwork" is on and an image exists, otherwise null. */
@Composable
fun rememberGameArtwork(game: Game): Bitmap? {
    val context = LocalContext.current
    val enabled = rememberShowArtwork()
    val artwork by produceState<Bitmap?>(initialValue = null, game.id, game.fileName, enabled) {
        value =
            if (enabled) {
                withContext(Dispatchers.IO) { ArtworkManager.loadArtwork(context.applicationContext, game) }
            } else {
                null
            }
    }
    return artwork
}
