package com.swordfish.lemuroid.app.shared.tiles

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

        fun forSystemId(systemId: String): SystemTileColors {
            val id = SystemID.values().firstOrNull { it.dbname == systemId } ?: return DEFAULT
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
