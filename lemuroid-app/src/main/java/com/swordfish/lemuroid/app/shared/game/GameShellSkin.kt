package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/**
 * The look around the picture, cycled by the immersive shortcut and remembered per game:
 * Off, Glow (immersive mode), then flat shell colours with a dark bezel round the picture.
 */
object GameShellSkin {
    const val OFF = 0
    const val GLOW = 1

    class Shell(val nameRes: Int, val color: Long, val bezel: Long)

    val shells =
        listOf(
            Shell(R.string.game_skin_teal, 0xFF1D9E75, BEZEL),
            Shell(R.string.game_skin_purple, 0xFF7F77DD, BEZEL),
            Shell(R.string.game_skin_yellow, 0xFFEF9F27, BEZEL),
            Shell(R.string.game_skin_berry, 0xFFD4537E, BEZEL),
            Shell(R.string.game_skin_light_grey, 0xFFD3D1C7, BEZEL),
            Shell(R.string.game_skin_dark_grey, 0xFF444441, 0xFF1A1A1A),
        )

    /** Off, Glow and every shell. */
    val count = 2 + shells.size

    /** The shell for a look, or null for Off and Glow. */
    fun shellFor(look: Int): Shell? = shells.getOrNull(look - 2)

    fun nameRes(look: Int): Int =
        when (look) {
            OFF -> R.string.game_skin_off
            GLOW -> R.string.game_skin_glow
            else -> shells[look - 2].nameRes
        }

    private fun key(game: Game) = "pref_key_game_shell_skin_${game.id}"

    /** The saved look; without a shell it follows the immersive setting (Glow or Off). */
    fun load(
        context: Context,
        game: Game,
    ): Int {
        val shell = SharedPreferencesHelper.getSharedPreferences(context).getInt(key(game), -1)
        if (shell in shells.indices) return 2 + shell
        return if (GameImmersiveMode.isEnabled(context, game)) GLOW else OFF
    }

    fun save(
        context: Context,
        game: Game,
        look: Int,
    ) {
        GameImmersiveMode.set(context, game, look == GLOW)
        SharedPreferencesHelper.getSharedPreferences(context).edit().putInt(key(game), look - 2).apply()
    }

    private const val BEZEL = 0xFF2C2C2A
}
