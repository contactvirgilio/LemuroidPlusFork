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

        /** One whole multiple smaller than [INTEGER]. */
        INTEGER_SMALLER(R.string.game_scaling_integer_smaller),
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
                SystemID.NDS -> 384
                SystemID.NINTENDO_3DS -> 480
                SystemID.ATARI2600 -> 210
                SystemID.ATARI7800 -> 223
                SystemID.LYNX -> 102
                SystemID.PC_ENGINE -> 232
                SystemID.NGP, SystemID.NGC -> 152
                SystemID.WS, SystemID.WSC -> 144
                SystemID.FBNEO, SystemID.MAME2003PLUS, SystemID.DOS -> null
            }

        fun isScalingSupported(systemId: SystemID): Boolean {
            return baseHeight(systemId) != null && GamePreviewCropper.aspectRatioFor(systemId) != null
        }

        /**
         * Viewport (fractions of the whole view) for the scaling mode. [areaLeft]..[areaBottom] is
         * the space available to the game in pixels, inside a view of [viewWidth] x [viewHeight].
         */
        fun computeViewport(
            scaling: Scaling,
            systemId: SystemID,
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
            if (scaling == Scaling.FIT) return fit

            val baseHeight = baseHeight(systemId) ?: return fit
            val aspectRatio = GamePreviewCropper.aspectRatioFor(systemId) ?: return fit

            val areaWidth = areaRight - areaLeft
            val areaHeight = areaBottom - areaTop
            val fitHeight = min(areaHeight, areaWidth / aspectRatio)

            var multiple = floor(fitHeight / baseHeight).toInt()
            if (scaling == Scaling.INTEGER_SMALLER) multiple -= 1
            // Screens too small for a whole multiple keep the normal size.
            if (multiple < 1) return fit

            val height = (multiple * baseHeight).toFloat()
            val width = height * aspectRatio
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
