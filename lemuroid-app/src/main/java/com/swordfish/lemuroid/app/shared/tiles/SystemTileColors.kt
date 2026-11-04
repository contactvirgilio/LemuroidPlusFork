package com.swordfish.lemuroid.app.shared.tiles

import android.content.SharedPreferences
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.swordfish.lemuroid.lib.library.MetaSystemID
import com.swordfish.lemuroid.lib.library.SystemID

/**
 * Solid color used to draw the default game tile. It is the same color the Systems screen uses
 * for the system's card, so a game's tile always matches its system.
 *
 * Some Systems cards group two systems (Genesis + Sega CD, Neo Geo Pocket + Color,
 * WonderSwan + Color, FBNeo + MAME). The second system in each pair gets a slightly darker
 * shade of the same color so the two can still be told apart.
 */
data class SystemTileColors(
    val base: Int,
) {
    /** White or near-black, whichever reads better on the base color. */
    val text: Int
        get() {
            val whiteContrast = ColorUtils.calculateContrast(Color.WHITE, base)
            val darkContrast = ColorUtils.calculateContrast(DARK_TEXT, base)
            return if (darkContrast > whiteContrast) DARK_TEXT else Color.WHITE
        }

    companion object {
        private val DARK_TEXT = Color.parseColor("#1C1C1C")
        private val DEFAULT = SystemTileColors(Color.parseColor("#5A5A66"))

        private const val SECONDARY_SHADE_DARKEN = 0.18f

        private val SECONDARY_SYSTEMS =
            setOf(
                SystemID.SEGACD,
                SystemID.NGC,
                SystemID.WSC,
                SystemID.MAME2003PLUS,
            )

        /** Soft colors picked in Settings; each system has its own, so none is darkened. */
        @Volatile
        var pastelEnabled: Boolean = false

        const val PASTEL_PREF_KEY = "pref_key_pastel_tiles"

        fun refresh(preferences: SharedPreferences) {
            pastelEnabled = preferences.getBoolean(PASTEL_PREF_KEY, false)
        }

        private val PASTEL =
            mapOf(
                SystemID.FBNEO to "#FFADAD",
                SystemID.MAME2003PLUS to "#FFADAD",
                SystemID.GB to "#A8C5A0",
                SystemID.GBC to "#F5E6A0",
                SystemID.GBA to "#B7A6E8",
                SystemID.GENESIS to "#7FD3C3",
                SystemID.NES to "#D4D4D4",
                SystemID.PSX to "#8EC5F0",
                SystemID.SNES to "#E0B0D0",
                SystemID.ATARI2600 to "#E8D5B5",
                SystemID.ATARI7800 to "#B8C4CE",
                SystemID.LYNX to "#F5C19A",
                SystemID.GG to "#B8E0D2",
                SystemID.SMS to "#94A7DB",
                SystemID.N64 to "#C5E384",
                SystemID.NINTENDO_3DS to "#E0A98F",
                SystemID.NDS to "#D3E0F5",
                SystemID.NGP to "#C3CCD6",
                SystemID.NGC to "#F5D0B8",
                SystemID.PC_ENGINE to "#E88E9E",
                SystemID.PSP to "#9EA3A8",
                SystemID.SEGACD to "#A58FCF",
                SystemID.WS to "#F1EFE6",
                SystemID.WSC to "#F0C8D8",
                SystemID.DOS to "#B9B29A",
            )

        /** The color of a Systems card, which can stand for two systems (the first one is used). */
        fun forMetaSystem(meta: MetaSystemID): SystemTileColors = forSystemId(meta.systemIDs.first().dbname)

        fun forSystemId(systemId: String): SystemTileColors {
            val id = SystemID.values().firstOrNull { it.dbname == systemId } ?: return DEFAULT
            if (pastelEnabled) {
                PASTEL[id]?.let { return SystemTileColors(Color.parseColor(it)) }
            }
            val systemColor = MetaSystemID.fromSystemID(id).color()
            val base =
                if (id in SECONDARY_SYSTEMS) {
                    ColorUtils.blendARGB(systemColor, Color.BLACK, SECONDARY_SHADE_DARKEN)
                } else {
                    systemColor
                }
            return SystemTileColors(base)
        }
    }
}
