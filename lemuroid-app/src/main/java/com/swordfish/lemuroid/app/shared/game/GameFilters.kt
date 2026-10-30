package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.libretrodroid.ShaderConfig

/**
 * Screen filters that can be switched from the in-game filter button, and the filter chosen for
 * each game (remembered across launches). "Auto" is not offered: it only ever resolves to one of these.
 */
object GameFilters {
    val FILTERS = listOf("sharp", "smooth", "crt", "lcd")

    private const val PREFS_NAME = "game_filters"

    fun savedFilter(
        context: Context,
        game: Game,
    ): String? =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key(game), null)
            ?.takeIf { it in FILTERS }

    fun saveFilter(
        context: Context,
        game: Game,
        filter: String,
    ) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key(game), filter)
            .apply()
    }

    /** The filter "Auto" picks for a system: LCD for handhelds, CRT for home consoles. */
    fun autoFilterFor(system: GameSystem): String =
        when (ShaderChooser.getDefaultShaderForSystem(system)) {
            ShaderConfig.LCD -> "lcd"
            ShaderConfig.CRT -> "crt"
            ShaderConfig.Default -> "smooth"
            else -> "sharp"
        }

    fun next(current: String?): String {
        val index = FILTERS.indexOf(current)
        return if (index < 0) FILTERS.first() else FILTERS[(index + 1) % FILTERS.size]
    }

    fun shaderFor(filter: String): ShaderConfig =
        when (filter) {
            "crt" -> ShaderConfig.CRT
            "lcd" -> ShaderConfig.LCD
            "smooth" -> ShaderConfig.Default
            else -> ShaderConfig.Sharp
        }

    private fun key(game: Game) = "game_${game.fileUri}"
}
