package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/**
 * The look around the picture, cycled by the immersive shortcut and remembered per game:
 * Off, Glow+ (immersive mode; the glow favours the game's colourful pixels), then flat shell
 * colours with a dark bezel round the picture. The original Glow was removed (owner, 2026-11-23).
 */
object GameShellSkin {
    const val OFF = 0
    const val GLOW_PLUS = 1
    private const val FIRST_SHELL = 2

    fun isGlow(look: Int) = look == GLOW_PLUS

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

    /** Off, Glow+ and every shell. */
    val count = FIRST_SHELL + shells.size

    /** Looks without shells: Off, Glow+. */
    const val BASIC_COUNT = FIRST_SHELL

    /** The shell for a look, or null for Off and Glow+. */
    fun shellFor(look: Int): Shell? = if (look >= FIRST_SHELL) shells.getOrNull(look - FIRST_SHELL) else null

    fun nameRes(look: Int): Int =
        when (look) {
            OFF -> R.string.game_skin_off
            GLOW_PLUS -> R.string.game_skin_glow_plus
            else -> shells[look - FIRST_SHELL].nameRes
        }

    private fun key(game: Game) = "pref_key_game_shell_skin_${game.id}"

    /** Settings switch: off (default) the immersive shortcut only toggles OLED and Glow+. */
    const val SETTING_KEY = "pref_key_shell_skins"

    fun isEnabled(context: Context): Boolean =
        SharedPreferencesHelper.getSharedPreferences(context).getBoolean(SETTING_KEY, false)

    /** The saved look; without a shell it follows the immersive setting (Glow+ or Off). */
    fun load(
        context: Context,
        game: Game,
    ): Int {
        val shell = SharedPreferencesHelper.getSharedPreferences(context).getInt(key(game), -1)
        if (shell in shells.indices) return FIRST_SHELL + shell
        return if (GameImmersiveMode.isEnabled(context, game)) GLOW_PLUS else OFF
    }

    fun save(
        context: Context,
        game: Game,
        look: Int,
    ) {
        GameImmersiveMode.set(context, game, isGlow(look))
        SharedPreferencesHelper.getSharedPreferences(context).edit()
            .putInt(key(game), look - FIRST_SHELL)
            .apply()
    }

    private const val BEZEL = 0xFF2C2C2A
}
