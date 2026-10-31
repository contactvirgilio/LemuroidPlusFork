package com.swordfish.lemuroid.app.mobile.feature.games

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Tile size on Home and Favorites (no list view there). Shared by both screens. */
enum class HomeTileSize(val size: Dp) {
    SMALL(88.dp),
    LARGE(116.dp),
}

/** Remembers the chosen tile size across screens and app restarts. */
object HomeTileSizeStore {
    private const val PREFS_NAME = "home_tile_size"
    private const val KEY_SIZE = "size"

    private var state: MutableStateFlow<HomeTileSize>? = null

    fun get(context: Context): StateFlow<HomeTileSize> {
        state?.let { return it }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved =
            prefs.getString(KEY_SIZE, null)
                ?.let { name -> HomeTileSize.entries.firstOrNull { it.name == name } }
                ?: HomeTileSize.LARGE
        return MutableStateFlow(saved).also { state = it }
    }

    fun set(
        context: Context,
        size: HomeTileSize,
    ) {
        (get(context) as MutableStateFlow).value = size
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SIZE, size.name)
            .apply()
    }
}

@Composable
fun rememberHomeTileSize(): HomeTileSize {
    val context = LocalContext.current
    val size by HomeTileSizeStore.get(context).collectAsState()
    return size
}
