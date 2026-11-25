package com.swordfish.lemuroid.app.shared.screenshots

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
 * The Screenshots style setting, watched once for the whole app. Every game tile reads it, so they
 * share this instead of each one registering its own listener.
 */
private object ScreenshotStyleSetting {
    var enabled by mutableStateOf(false)
        private set

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun ensureWatching(context: Context) {
        if (listener != null) return
        val appContext = context.applicationContext
        enabled = ScreenshotManager.isEnabled(appContext)
        val created =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == ScreenshotManager.STYLE_KEY) enabled = ScreenshotManager.isEnabled(appContext)
            }
        // Kept in a field: preferences only hold listeners weakly.
        listener = created
        ScreenshotManager.preferences(appContext).registerOnSharedPreferenceChangeListener(created)
    }
}

/** Follows the Screenshots style setting, updating when it changes. */
@Composable
fun rememberScreenshotStyle(): Boolean {
    val context = LocalContext.current
    remember { ScreenshotStyleSetting.ensureWatching(context) }
    return ScreenshotStyleSetting.enabled
}

/** The game's screenshot in the Screenshots style when one exists, otherwise null. */
@Composable
fun rememberGameScreenshot(game: Game): Bitmap? {
    val context = LocalContext.current
    val enabled = rememberScreenshotStyle()
    // An image that is already loaded shows at once (no colour flash while scrolling); the
    // background check below still runs and corrects it if the file changed.
    val initial =
        remember(game.id, game.fileName, enabled, ScreenshotManager.version) {
            if (enabled) ScreenshotManager.peekScreenshot(game) else null
        }
    val artwork by produceState(initialValue = initial, game.id, game.fileName, enabled, ScreenshotManager.version) {
        value =
            if (enabled) {
                withContext(Dispatchers.IO) { ScreenshotManager.loadScreenshot(context.applicationContext, game) }
            } else {
                null
            }
    }
    return artwork
}

/** Turns the Screenshots style on or off (it excludes the Cartridges style). */
fun setScreenshotStyle(
    context: Context,
    on: Boolean,
) {
    ScreenshotManager.preferences(context.applicationContext).edit().putBoolean(ScreenshotManager.STYLE_KEY, on).apply()
}
