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
 * top and the bottom screen. It is the default on every device (owner); the game menu's Screens tile
 * switches to Both (stacked) or Side by side.
 */
object DsSingleScreen {
    private class Values(val key: String, val top: String, val bottom: String, val stacked: String, val side: String)

    private fun values(coreId: CoreID): Values? =
        when (coreId) {
            // melonDS DS core
            CoreID.MELONDS -> Values("melonds_screen_layout1", "top", "bottom", "top-bottom", "left-right")
            CoreID.DESMUME -> Values("desmume_screens_layout", "top only", "bottom only", "top/bottom", "left/right")
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

    private fun isSingle(layout: String?): Boolean = layout == "top" || layout == "top only"

    /** The core option key this mode drives (hidden from the game menu's option tiles). */
    fun layoutKey(coreId: CoreID): String? = values(coreId)?.key

    /** The game menu's Screens choices, in tile order. */
    enum class Layout { BOTH, SINGLE, SIDE_BY_SIDE }

    /** Game menu "Screens" tile: the chosen layout (or the default for this device). */
    fun chosenLayout(
        context: Context,
        config: SystemCoreConfig,
    ): Layout {
        val v = values(config.coreID) ?: return Layout.BOTH
        val layout = effectiveLayout(context, config)
        return when {
            isSingle(layout) -> Layout.SINGLE
            layout == v.side -> Layout.SIDE_BY_SIDE
            else -> Layout.BOTH
        }
    }

    /** Side by side: the picture is twice as wide, so its size comes from the core (see scaling). */
    fun isSideBySide(
        context: Context,
        config: SystemCoreConfig,
    ): Boolean = chosenLayout(context, config) == Layout.SIDE_BY_SIDE

    /** Game menu "Screens" tile: Both, Single (R2 flips) or Side by side. Applies when the menu closes. */
    fun choose(
        context: Context,
        config: SystemCoreConfig,
        layout: Layout,
    ) {
        val v = values(config.coreID) ?: return
        val prefKey = CoreVariablesManager.computeSharedPreferenceKey(v.key, SystemID.NDS.dbname)
        val value =
            when (layout) {
                Layout.BOTH -> v.stacked
                Layout.SINGLE -> v.top
                Layout.SIDE_BY_SIDE -> v.side
            }
        SharedPreferencesHelper.getSharedPreferences(context).edit().putString(prefKey, value).apply()
        showingBottom = false
    }

    private fun effectiveLayout(
        context: Context,
        config: SystemCoreConfig,
    ): String? {
        val v = values(config.coreID) ?: return null
        val prefKey = CoreVariablesManager.computeSharedPreferenceKey(v.key, SystemID.NDS.dbname)
        val saved = SharedPreferencesHelper.getSharedPreferences(context).getString(prefKey, null)
        if (saved != null) return saved
        // Single screen is the default everywhere (owner, 2026-11-19).
        return v.top
    }

}
