package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/** In-game shortcuts: folded behind one button ("nested", default) or always in a row. */
object GameShortcutsSetting {
    const val KEY = "pref_key_game_shortcuts_nested"

    fun isNested(context: Context): Boolean =
        SharedPreferencesHelper.getSharedPreferences(context).getBoolean(KEY, true)
}
