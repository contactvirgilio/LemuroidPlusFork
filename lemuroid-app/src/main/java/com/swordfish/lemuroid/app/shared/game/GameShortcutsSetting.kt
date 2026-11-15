package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/**
 * In-game shortcuts: folded behind one button ("nested") or always in a row. Until the user picks
 * one, 4:3-ish screens (Retroid Pocket Classic, about 1.15) get nested and phones get the row.
 */
object GameShortcutsSetting {
    const val KEY = "pref_key_game_shortcuts_nested"

    fun isNested(context: Context): Boolean =
        SharedPreferencesHelper.getSharedPreferences(context).getBoolean(KEY, defaultNested(context))

    /** Width over height of the screen, long side first, between 1.0 and 1.5 (as in Tetris DX). */
    fun defaultNested(context: Context): Boolean {
        val metrics = context.resources.displayMetrics
        val long = maxOf(metrics.widthPixels, metrics.heightPixels).toFloat()
        val short = minOf(metrics.widthPixels, metrics.heightPixels).toFloat()
        if (short <= 0f) return false
        return long / short < 1.5f
    }
}
