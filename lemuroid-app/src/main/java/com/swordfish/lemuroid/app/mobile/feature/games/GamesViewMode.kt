package com.swordfish.lemuroid.app.mobile.feature.games

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** How games are shown inside a system: as a list, or as a grid of small or large tiles. */
enum class GamesViewMode {
    LIST,
    GRID_SMALL,
    GRID_LARGE,
}

/** Remembers the chosen view mode across screens and app restarts. */
object GamesViewModeStore {
    private const val PREFS_NAME = "games_view_mode"
    private const val KEY_MODE = "mode"

    private var state: MutableStateFlow<GamesViewMode>? = null

    fun get(context: Context): StateFlow<GamesViewMode> {
        state?.let { return it }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved =
            prefs.getString(KEY_MODE, null)
                ?.let { name -> GamesViewMode.entries.firstOrNull { it.name == name } }
                ?: GamesViewMode.LIST
        return MutableStateFlow(saved).also { state = it }
    }

    fun set(
        context: Context,
        mode: GamesViewMode,
    ) {
        (get(context) as MutableStateFlow).value = mode
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}

@Composable
fun rememberGamesViewMode(): GamesViewMode {
    val context = LocalContext.current
    val mode by GamesViewModeStore.get(context).collectAsState()
    return mode
}
