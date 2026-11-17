package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/** Touch pad look: off is the classic grey pad, on is the outline pad with coloured buttons when pressed. */
object PadSkinSetting {
    const val KEY = "pref_key_pad_skin_outline"

    fun isOutline(context: Context): Boolean =
        SharedPreferencesHelper.getSharedPreferences(context).getBoolean(KEY, false)
}
