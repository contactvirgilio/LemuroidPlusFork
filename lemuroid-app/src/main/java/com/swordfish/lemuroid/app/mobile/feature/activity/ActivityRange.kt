package com.swordfish.lemuroid.app.mobile.feature.activity

import android.content.Context
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/** Remembers whether the Activity tab shows this month or all time (kept with the other settings). */
object ActivityRange {
    private const val KEY = "pref_key_activity_all_time"

    fun isAllTime(context: Context): Boolean =
        SharedPreferencesHelper.getSharedPreferences(
            context,
        ).getBoolean(KEY, false)

    fun setAllTime(
        context: Context,
        allTime: Boolean,
    ) {
        SharedPreferencesHelper.getSharedPreferences(context).edit().putBoolean(KEY, allTime).apply()
    }
}
