package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/**
 * Immersive mode (the bars around the picture take a soft glow of the game's colors), remembered
 * for each game from the game menu tile. On unless switched off for that game.
 */
object GameImmersiveMode {
    private fun key(game: Game) = "pref_key_game_immersive_${game.id}"

    fun isEnabled(
        context: Context,
        game: Game,
    ): Boolean = SharedPreferencesHelper.getSharedPreferences(context).getBoolean(key(game), true)

    fun set(
        context: Context,
        game: Game,
        enabled: Boolean,
    ) {
        SharedPreferencesHelper.getSharedPreferences(context).edit().putBoolean(key(game), enabled).apply()
    }
}
