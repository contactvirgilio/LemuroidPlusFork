package com.swordfish.lemuroid.app.shared.gamemenu

import android.content.SharedPreferences
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID

/**
 * The Game Boy Color / Game Boy Advance color correction, switched from the in-game menu instead
 * of Settings. Only the cores that expose it are supported; the menu hides the switch otherwise.
 */
object ColorCorrection {
    data class Spec(val key: String, val on: String, val off: String)

    fun specFor(
        systemId: SystemID,
        coreID: CoreID,
    ): Spec? =
        when {
            systemId == SystemID.GBC && coreID == CoreID.GAMBATTE ->
                Spec("gambatte_gbc_color_correction", "always", "disabled")
            systemId == SystemID.GBA && coreID == CoreID.MGBA ->
                Spec("mgba_color_correction", "GBA", "OFF")
            else -> null
        }

    fun specFor(
        systemId: SystemID,
        config: SystemCoreConfig,
    ): Spec? = specFor(systemId, config.coreID)

    fun isEnabled(
        preferences: SharedPreferences,
        systemId: SystemID,
        config: SystemCoreConfig,
    ): Boolean {
        val spec = specFor(systemId, config) ?: return false
        val default = config.defaultSettings.firstOrNull { it.key == spec.key }?.value ?: spec.off
        val stored = preferences.getString(prefKey(spec, systemId), null) ?: default
        return stored == spec.on
    }

    fun save(
        preferences: SharedPreferences,
        systemId: SystemID,
        config: SystemCoreConfig,
        enabled: Boolean,
    ) {
        val spec = specFor(systemId, config) ?: return
        preferences.edit().putString(prefKey(spec, systemId), if (enabled) spec.on else spec.off).apply()
    }

    private fun prefKey(
        spec: Spec,
        systemId: SystemID,
    ) = CoreVariablesManager.computeSharedPreferenceKey(spec.key, systemId.dbname)
}
