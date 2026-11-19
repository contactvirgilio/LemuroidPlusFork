package com.swordfish.lemuroid.app.shared.game

import android.content.SharedPreferences
import android.graphics.RectF
import androidx.annotation.StringRes
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.viewmodel.GamePreviewCropper
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.libretrodroid.ShaderConfig
import com.swordfish.libretrodroid.ViewportAlignment
import kotlin.math.floor
import kotlin.math.min

/**
 * Filter and scaling picked with the in-game buttons. Both are remembered per system, so a
 * filter chosen for SNES is used again the next time any SNES game starts.
 */
class GameDisplayOptions(
    private val sharedPreferences: SharedPreferences,
    private val systemId: SystemID,
) {
    enum class Filter(
        @StringRes val labelRes: Int,
        val shader: ShaderConfig,
    ) {
        SHARP(R.string.game_filter_sharp, ShaderConfig.Sharp),
        SMOOTH(R.string.game_filter_smooth, ShaderConfig.Default),
        CRT(R.string.game_filter_crt, ShaderConfig.CRT),
        LCD(R.string.game_filter_lcd, ShaderConfig.LCD),

        // MiSTer-style scanlines: darkest with integer scaling (Fine integer / Pixel-perfect).
        SCAN_25(R.string.game_filter_scan_25, ShaderConfig.Scanlines(strength = 0.25f, boost = 0.08f)),
        SCAN_50(R.string.game_filter_scan_50, ShaderConfig.Scanlines(strength = 0.5f, boost = 0.18f)),
        SCAN_75(R.string.game_filter_scan_75, ShaderConfig.Scanlines(strength = 0.75f, boost = 0.3f)),
        SCAN_MASK(R.string.game_filter_scan_mask, ShaderConfig.Scanlines(strength = 0.5f, boost = 0.2f, mask = true)),
        ;

        companion object {
            /** The button number for the filter chosen in Settings, or null for HD mode shaders. */
            fun fromShader(shader: ShaderConfig): Filter? = values().firstOrNull { it.shader == shader }
        }
    }

    enum class Scaling(
        @StringRes val labelRes: Int,
    ) {
        /** As large as fits, keeping the game's shape (Lemuroid's normal behaviour). */
        FIT(R.string.game_scaling_fit),

        /** The largest whole multiple of the game's height, so every pixel is the same size. */
        INTEGER(R.string.game_scaling_integer),

        /**
         * Whole multiples of the game's pixels both ways (MiSTer style): the width is the whole
         * multiple closest to the right shape, so every pixel is identical but the shape can be off.
         */
        PIXEL_PERFECT(R.string.game_scaling_pixel_perfect),

        /** Like [INTEGER] but in quarter steps (4, 4.25, 4.5...): bigger, with some rows 1 px taller. */
        FINE_INTEGER(R.string.game_scaling_fine_integer),

        /** Stretch to fill: the picture covers the whole game area, ignoring its shape. */
        STRETCH(R.string.game_scaling_stretch),
    }

    fun savedFilter(): Filter? {
        val name = sharedPreferences.getString(key(FILTER_KEY), null) ?: return null
        return Filter.values().firstOrNull { it.name == name }
    }

    fun saveFilter(filter: Filter) {
        sharedPreferences.edit().putString(key(FILTER_KEY), filter.name).apply()
    }

    fun savedScaling(): Scaling {
        val name = sharedPreferences.getString(key(SCALING_KEY), null)
        // "Integer -1" was removed (owner, 2026-11-18): it becomes Integer.
        if (name == "INTEGER_SMALLER") return Scaling.INTEGER
        return Scaling.values().firstOrNull { it.name == name } ?: Scaling.FIT
    }

    fun saveScaling(scaling: Scaling) {
        sharedPreferences.edit().putString(key(SCALING_KEY), scaling.name).apply()
    }

    private fun key(base: String) = "${base}_${systemId.dbname}"

    companion object {
        private const val FILTER_KEY = "pref_key_game_button_filter"
        private const val SCALING_KEY = "pref_key_game_button_scaling"

        /**
         * Height in pixels of the picture each system outputs with the default core settings.
         * Null when it varies too much (arcade, DOS) for integer scaling to make sense.
         */
        fun baseHeight(systemId: SystemID): Int? =
            when (systemId) {
                SystemID.NES -> 224
                SystemID.SNES -> 224
                SystemID.GENESIS, SystemID.SEGACD -> 224
                SystemID.SMS -> 192
                SystemID.GG -> 144
                SystemID.GB, SystemID.GBC -> 144
                SystemID.GBA -> 160
                SystemID.N64 -> 240
                SystemID.PSX -> 240
                SystemID.PSP -> 272
                SystemID.NDS -> if (DsSingleScreen.active) 192 else 384
                SystemID.NINTENDO_3DS -> 480
                SystemID.ATARI2600 -> 210
                SystemID.ATARI7800 -> 223
                SystemID.LYNX -> 102
                SystemID.PC_ENGINE -> 232
                SystemID.NGP, SystemID.NGC -> 152
                SystemID.WS, SystemID.WSC -> 144
                SystemID.FBNEO, SystemID.MAME2003PLUS, SystemID.DOS -> null
            }

        /** Width in pixels of the picture each system outputs (for pixel-perfect scaling). */
        fun baseWidth(systemId: SystemID): Int? =
            when (systemId) {
                SystemID.NES, SystemID.SNES, SystemID.SMS, SystemID.PC_ENGINE, SystemID.NDS -> 256
                SystemID.GENESIS, SystemID.SEGACD, SystemID.N64, SystemID.PSX, SystemID.ATARI7800 -> 320
                SystemID.GG, SystemID.GB, SystemID.GBC, SystemID.ATARI2600, SystemID.LYNX -> 160
                SystemID.NGP, SystemID.NGC -> 160
                SystemID.GBA -> 240
                SystemID.WS, SystemID.WSC -> 224
                SystemID.PSP -> 480
                SystemID.NINTENDO_3DS -> 400
                SystemID.FBNEO, SystemID.MAME2003PLUS, SystemID.DOS -> null
            }

        /** Size of the picture and its shape, used to work out whole-number sizes. */
        data class Profile(val baseHeight: Int, val aspectRatio: Float, val baseWidth: Int? = null)

        /**
         * Null when the game has no fixed picture size (most of Arcade), so there is nothing to
         * scale. Neo Geo games inside Arcade are the exception: they all draw 320 x 224.
         */
        fun profileFor(
            systemId: SystemID,
            romFileName: String,
        ): Profile? {
            val isArcade = systemId == SystemID.FBNEO || systemId == SystemID.MAME2003PLUS
            // Neo Geo (320 x 224) and Capcom CPS1/CPS2 (384 x 224) games, all shown at 4:3.
            if (isArcade && NeoGeoSets.isNeoGeo(romFileName)) return Profile(224, 4f / 3f, 320)
            if (isArcade && CpsSets.isCps(romFileName)) return Profile(224, 4f / 3f, 384)
            val height = baseHeight(systemId) ?: return null
            val aspectRatio = GamePreviewCropper.aspectRatioFor(systemId) ?: return null
            return Profile(height, aspectRatio, baseWidth(systemId))
        }

        /**
         * Pixel-perfect: a whole multiple of the height, then the whole multiple of the width
         * closest to the right shape. Tries the biggest height first. Width to height in pixels.
         */
        private fun pixelPerfectSize(
            profile: Profile,
            areaWidth: Float,
            areaHeight: Float,
        ): Pair<Float, Float>? {
            val w = profile.baseWidth ?: return null
            val h = profile.baseHeight
            var v = floor(areaHeight / h).toInt()
            while (v >= 1) {
                val target = v * h * profile.aspectRatio / w
                val choices = listOf(floor(target).toInt(), floor(target).toInt() + 1).filter { it >= 1 && it * w <= areaWidth }
                val k = choices.minByOrNull { kotlin.math.abs(it - target) }
                if (k != null) return (k * w).toFloat() to (v * h).toFloat()
                v--
            }
            return null
        }

        /** Modes where the picture fills its viewport exactly (the shape is set by the viewport). */
        fun fillsViewport(scaling: Scaling) = scaling == Scaling.STRETCH || scaling == Scaling.PIXEL_PERFECT

        /**
         * Viewport (fractions of the whole view) for the scaling mode. [areaLeft]..[areaBottom] is
         * the space available to the game in pixels, inside a view of [viewWidth] x [viewHeight].
         */
        fun computeViewport(
            scaling: Scaling,
            profile: Profile?,
            alignment: ViewportAlignment,
            viewWidth: Float,
            viewHeight: Float,
            areaLeft: Float,
            areaTop: Float,
            areaRight: Float,
            areaBottom: Float,
        ): RectF {
            val fit =
                RectF(
                    areaLeft / viewWidth,
                    areaTop / viewHeight,
                    areaRight / viewWidth,
                    areaBottom / viewHeight,
                )
            if (scaling == Scaling.FIT || scaling == Scaling.STRETCH) return fit

            val baseHeight = profile?.baseHeight ?: return fit
            val aspectRatio = profile.aspectRatio

            val areaWidth = areaRight - areaLeft
            val areaHeight = areaBottom - areaTop
            val fitHeight = min(areaHeight, areaWidth / aspectRatio)

            val size =
                when (scaling) {
                    Scaling.PIXEL_PERFECT -> pixelPerfectSize(profile, areaWidth, areaHeight)
                    Scaling.FINE_INTEGER -> {
                        // Quarter steps of the height.
                        val step = floor(fitHeight * 4f / baseHeight) / 4f
                        if (step < 1f) null else (step * baseHeight).let { h -> h * aspectRatio to h }
                    }
                    else -> {
                        val multiple = floor(fitHeight / baseHeight).toInt()
                        if (multiple < 1) null else (multiple * baseHeight).toFloat().let { h -> h * aspectRatio to h }
                    }
                }
            // Screens too small for a whole multiple keep the normal size.
            val (width, height) = size ?: return fit
            val left = areaLeft + (areaWidth - width) / 2f
            val top =
                when (alignment) {
                    ViewportAlignment.TOP -> areaTop
                    ViewportAlignment.BOTTOM -> areaBottom - height
                    else -> areaTop + (areaHeight - height) / 2f
                }

            return RectF(
                left / viewWidth,
                top / viewHeight,
                (left + width) / viewWidth,
                (top + height) / viewHeight,
            )
        }
    }
}
