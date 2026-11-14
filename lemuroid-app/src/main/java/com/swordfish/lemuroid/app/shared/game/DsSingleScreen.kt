package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import android.view.InputDevice
import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/**
 * Nintendo DS "Single screen" layout: one screen at a time, full size, and R2 flips between the
 * top and the bottom screen. It is the default when a gamepad is present (built-in controls count);
 * without one there is no R2 to flip with, so the default stays two stacked screens.
 */
object DsSingleScreen {
    private class Values(val key: String, val top: String, val bottom: String, val stacked: String)

    private fun values(coreId: CoreID): Values? =
        when (coreId) {
            CoreID.MELONDS -> Values("melonds_screen_layout1", "top-only", "bottom-only", "top-bottom")
            CoreID.DESMUME -> Values("desmume_screens_layout", "top only", "bottom only", "top/bottom")
            else -> null
        }

    /** Whether the running DS game is in single-screen mode (read by scaling and previews). */
    @Volatile
    var active: Boolean = false
        private set

    /** In single-screen mode, whether the bottom screen is the one showing. */
    @Volatile
    var showingBottom: Boolean = false
        private set

    /** Call when a game starts: back to the top screen, and work out whether the mode is on. */
    fun start(
        context: Context,
        systemId: SystemID,
        config: SystemCoreConfig,
    ) {
        showingBottom = false
        active = systemId == SystemID.NDS && isSingle(effectiveLayout(context, config))
    }

    /** The core variables to send, with the layout adjusted for this mode and the screen showing. */
    fun adjust(
        context: Context,
        systemId: SystemID,
        config: SystemCoreConfig,
        variables: List<CoreVariable>,
    ): List<CoreVariable> {
        if (systemId != SystemID.NDS) return variables
        val v = values(config.coreID) ?: return variables
        val layout = effectiveLayout(context, config)
        active = isSingle(layout)
        val value = if (active) (if (showingBottom) v.bottom else v.top) else (layout ?: return variables)
        return variables.filter { it.key != v.key } + CoreVariable(v.key, value)
    }

    /** R2 pressed: flip the screen. Returns the variable to send, or null when the mode is off. */
    fun flip(
        context: Context,
        systemId: SystemID,
        config: SystemCoreConfig,
    ): CoreVariable? {
        if (systemId != SystemID.NDS) return null
        val v = values(config.coreID) ?: return null
        if (!isSingle(effectiveLayout(context, config))) return null
        active = true
        showingBottom = !showingBottom
        return CoreVariable(v.key, if (showingBottom) v.bottom else v.top)
    }

    private fun isSingle(layout: String?): Boolean = layout == "top-only" || layout == "top only"

    private fun effectiveLayout(
        context: Context,
        config: SystemCoreConfig,
    ): String? {
        val v = values(config.coreID) ?: return null
        val prefKey = CoreVariablesManager.computeSharedPreferenceKey(v.key, SystemID.NDS.dbname)
        val saved = SharedPreferencesHelper.getSharedPreferences(context).getString(prefKey, null)
        if (saved != null) return saved
        return if (hasGamePad()) v.top else v.stacked
    }

    private fun hasGamePad(): Boolean =
        InputDevice.getDeviceIds().any { id ->
            val device = InputDevice.getDevice(id) ?: return@any false
            !device.isVirtual &&
                (device.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        }
}
