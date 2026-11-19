package com.swordfish.lemuroid.app.shared.gamemenu

import android.content.SharedPreferences
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID

/**
 * Motion blur like the real handheld screen (the cores' "LCD ghosting"), switched from the in-game
 * menu next to Color correction instead of the core options. Only where the core offers it.
 */
object MotionBlur {
    data class Spec(val key: String, val on: String, val off: String)

    fun specFor(config: SystemCoreConfig): Spec? {
        val spec =
            when (config.coreID) {
                CoreID.GAMBATTE -> Spec("gambatte_mix_frames", "lcd_ghosting", "disabled")
                CoreID.MGBA -> Spec("mgba_interframe_blending", "lcd_ghosting", "OFF")
                CoreID.GENESIS_PLUS_GX -> Spec("genesis_plus_gx_lcd_filter", "enabled", "disabled")
                else -> null
            } ?: return null
        // Only for systems where the core option is offered (e.g. Game Gear, not Genesis TV games).
        return spec.takeIf { s -> config.exposedSettings.any { it.key == s.key } }
    }

    fun isEnabled(
        preferences: SharedPreferences,
        systemId: SystemID,
        config: SystemCoreConfig,
    ): Boolean {
        val spec = specFor(config) ?: return false
        val default = config.defaultSettings.firstOrNull { it.key == spec.key }?.value ?: spec.off
        val stored = preferences.getString(prefKey(spec, systemId), null) ?: default
        return stored != spec.off
    }

    fun save(
        preferences: SharedPreferences,
        systemId: SystemID,
        config: SystemCoreConfig,
        enabled: Boolean,
    ) {
        val spec = specFor(config) ?: return
        preferences.edit().putString(prefKey(spec, systemId), if (enabled) spec.on else spec.off).apply()
    }

    private fun prefKey(
        spec: Spec,
        systemId: SystemID,
    ) = CoreVariablesManager.computeSharedPreferenceKey(spec.key, systemId.dbname)
}
