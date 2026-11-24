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
/**
 * Filter and scaling. Per game since 2026-11-23 (owner): a game you change keeps its own choice;
 * games you haven't changed start from the system's choice saved before that.
 */
class GameDisplayOptions(
    private val sharedPreferences: SharedPreferences,
    private val systemId: SystemID,
    private val gameId: Int? = null,
) {
    enum class Filter(
        @StringRes val labelRes: Int,
        private val baseShader: ShaderConfig,
    ) {
        SHARP(R.string.game_filter_sharp, ShaderConfig.Sharp),
        SMOOTH(R.string.game_filter_smooth, ShaderConfig.Default),

        // Handheld screens (2026-11-23): an even 1 px pixel grid with a look per system, and Sunlit
        // (soft pixel shadows on a paper-like back layer, Game Boy and Game Boy Color only).
        LCD(R.string.game_filter_lcd, ShaderConfig.Handheld(grid = true)),
        // Sunlit (2026-11-24): light pixels turn see-through over a grey-beige backing, with soft shadows
        // (inspired by Pixel Transparency).
        SUNLIT(R.string.game_filter_sunlit, ShaderConfig.Handheld(grid = false, transparent = true)),
        SUNLIT_BRIGHT(R.string.game_filter_sunlit_bright, ShaderConfig.Handheld(grid = false, sunlit = true, bright = true)),
        LCD_SUNLIT_BRIGHT(R.string.game_filter_lcd_sunlit_bright, ShaderConfig.Handheld(grid = true, sunlit = true, bright = true)),
        SCAN_SUNLIT_BRIGHT(R.string.game_filter_scan_sunlit_bright, ShaderConfig.Handheld(grid = false, sunlit = true, bright = true, scan = 0.35f)),

        // CRT-style scanlines (soft beams); most even with integer scaling (Pixel-perfect).
        SCAN_25(R.string.game_filter_scan_25, ShaderConfig.Scanlines(strength = 0.35f, boost = 0.06f)),
        SCAN_50(R.string.game_filter_scan_50, ShaderConfig.Scanlines(strength = 0.6f, boost = 0.15f)),
        SCAN_75(R.string.game_filter_scan_75, ShaderConfig.Scanlines(strength = 0.9f, boost = 0.3f)),
        SCAN_MASK(R.string.game_filter_scan_mask, ShaderConfig.Scanlines(strength = 0.6f, boost = 0.18f, mask = true)),
        ;

        /** The shader for this filter on a system (the handheld ones pick that system's screen look). */
        fun shaderFor(systemId: SystemID): ShaderConfig =
            when (val base = baseShader) {
                is ShaderConfig.Handheld -> base.copy(screen = handheldScreen(systemId))
                else -> base
            }

        companion object {
            /** The button number for the filter chosen in Settings, or null for HD mode shaders. */
            fun fromShader(shader: ShaderConfig): Filter? =
                when (shader) {
                    is ShaderConfig.Handheld ->
                        when {
                            shader.scan > 0f -> SCAN_SUNLIT_BRIGHT
                            shader.grid && shader.sunlit && shader.bright -> LCD_SUNLIT_BRIGHT
                            shader.sunlit && shader.bright -> SUNLIT_BRIGHT
                            shader.transparent -> SUNLIT
                            else -> LCD
                        }
                    ShaderConfig.LCD -> LCD
                    else -> values().firstOrNull { it.baseShader == shader }
                }
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

    /**
     * The filters this system offers: LCD only for Game Boy, Game Boy Color and GBA (owner, 2026-11-22);
     * Sunlit and LCD + Sunlit only for Game Boy and Game Boy Color (owner, 2026-11-23).
     */
    fun filters(): List<Filter> = Filter.values().filter { offers(it) }

    private fun offers(filter: Filter): Boolean =
        when (filter) {
            Filter.LCD -> hasLcd(systemId)
            Filter.SUNLIT, Filter.SUNLIT_BRIGHT, Filter.LCD_SUNLIT_BRIGHT, Filter.SCAN_SUNLIT_BRIGHT -> hasSunlit(systemId)
            else -> true
        }

    fun savedFilter(): Filter? {
        val name = saved(FILTER_KEY) ?: return null
        // The old CRT filter was removed (owner, 2026-11-19): Scanlines 25% replaces it.
        if (name == "CRT") return Filter.SCAN_25
        val filter = Filter.values().firstOrNull { it.name == name }
        // LCD saved earlier on a system that no longer offers it: Sharp instead.
        return if (filter != null && !offers(filter)) Filter.SHARP else filter
    }

    fun saveFilter(filter: Filter) {
        sharedPreferences.edit().putString(saveKey(FILTER_KEY), filter.name).apply()
    }

    fun savedScaling(): Scaling {
        val name = saved(SCALING_KEY)
        // "Integer -1" was removed (owner, 2026-11-18): it becomes Integer.
        if (name == "INTEGER_SMALLER") return Scaling.INTEGER
        return Scaling.values().firstOrNull { it.name == name } ?: Scaling.FIT
    }

    fun saveScaling(scaling: Scaling) {
        sharedPreferences.edit().putString(saveKey(SCALING_KEY), scaling.name).apply()
    }

    private fun key(base: String) = "${base}_${systemId.dbname}"

    private fun gameKey(base: String) = gameId?.let { "${base}_game_$it" }

    // This game's choice, else the system's.
    private fun saved(base: String): String? =
        gameKey(base)?.let { sharedPreferences.getString(it, null) } ?: sharedPreferences.getString(key(base), null)

    private fun saveKey(base: String) = gameKey(base) ?: key(base)

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

/** Systems whose real screen was an LCD with a visible grid: only these get the LCD filter. */
fun hasLcd(systemId: SystemID): Boolean = systemId == SystemID.GB || systemId == SystemID.GBC || systemId == SystemID.GBA

/** Sunlit (soft pixel shadows) is offered for Game Boy and Game Boy Color only (owner, 2026-11-23). */
fun hasSunlit(systemId: SystemID): Boolean = systemId == SystemID.GB || systemId == SystemID.GBC

/** Which handheld screen look the LCD filter copies: 0 Game Boy, 1 Game Boy Color, 2 GBA. */
fun handheldScreen(systemId: SystemID): Int =
    when (systemId) {
        SystemID.GBC -> 1
        SystemID.GBA -> 2
        else -> 0
    }
