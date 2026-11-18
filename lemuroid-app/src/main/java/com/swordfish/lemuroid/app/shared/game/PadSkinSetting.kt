package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/** Touch pad look: on (default) is the outline pad, off the classic grey pad with coloured buttons when pressed. */
object PadSkinSetting {
    const val KEY = "pref_key_pad_skin_outline"

    fun isOutline(context: Context): Boolean =
        SharedPreferencesHelper.getSharedPreferences(context).getBoolean(KEY, true)
}
