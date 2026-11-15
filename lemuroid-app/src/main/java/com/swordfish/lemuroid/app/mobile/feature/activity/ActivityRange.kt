package com.swordfish.lemuroid.app.mobile.feature.activity

import android.content.Context
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/** The Activity tab's period (Week / Month / Year / All time). */
enum class ActivityPeriod { WEEK, MONTH, YEAR, ALL }

/** Remembers the Activity tab's period and how many top games it lists (kept with the settings). */
object ActivityRange {
    private const val KEY_PERIOD = "pref_key_activity_period"
    private const val KEY_TOP = "pref_key_activity_top_count"
    val TOP_COUNTS = listOf(5, 10, 25)

    fun period(context: Context): ActivityPeriod {
        val name = SharedPreferencesHelper.getSharedPreferences(context).getString(KEY_PERIOD, null)
        return ActivityPeriod.values().firstOrNull { it.name == name } ?: ActivityPeriod.WEEK
    }

    fun setPeriod(
        context: Context,
        period: ActivityPeriod,
    ) {
        SharedPreferencesHelper.getSharedPreferences(context).edit().putString(KEY_PERIOD, period.name).apply()
    }

    fun topCount(context: Context): Int =
        SharedPreferencesHelper.getSharedPreferences(context).getInt(KEY_TOP, 5).takeIf { it in TOP_COUNTS } ?: 5

    fun setTopCount(
        context: Context,
        count: Int,
    ) {
        SharedPreferencesHelper.getSharedPreferences(context).edit().putInt(KEY_TOP, count).apply()
    }
}
