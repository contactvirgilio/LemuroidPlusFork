package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlin.random.Random

/** Predefined cartridge colors. */
object CartridgePalette {
    val colors: List<Color> =
        listOf(
            Color(0xFFD63C3C),
            Color(0xFFEC8428),
            Color(0xFFE6BE32),
            Color(0xFF3CAA5A),
            Color(0xFF28AAAA),
            Color(0xFF3C6EDC),
            Color(0xFF8C50D2),
            Color(0xFFE864AA),
        )
}

/** A cartridge's chosen color ([color] indexes [CartridgePalette]) and finish. */
data class CartridgeStyle(val color: Int, val sparkly: Boolean) {
    fun encode(): String = "$color${if (sparkly) "s" else "m"}"

    companion object {
        fun parse(text: String?): CartridgeStyle? {
            if (text == null || text.length < 2) return null
            val color = text.dropLast(1).toIntOrNull() ?: return null
            if (color !in CartridgePalette.colors.indices) return null
            return when (text.last()) {
                's' -> CartridgeStyle(color, true)
                'm' -> CartridgeStyle(color, false)
                else -> null
            }
        }
    }
}

/** The color each game's cartridge was given, kept on the device. Games without one stay as drawn. */
object CartridgeStyles {
    private const val PREFS = "cartridge_styles"

    private val styles = mutableStateMapOf<String, CartridgeStyle>()
    private var loaded = false

    // Same game after a rescan keeps its color: the key is the system and the file name.
    private fun key(game: Game) = "${game.systemId}/${game.fileName}"

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all.forEach { (key, value) ->
            CartridgeStyle.parse(value as? String)?.let { styles[key] = it }
        }
    }

    fun get(game: Game): CartridgeStyle? = styles[key(game)]

    fun set(
        context: Context,
        game: Game,
        style: CartridgeStyle?,
    ) {
        ensureLoaded(context)
        val editor = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (style == null) {
            styles.remove(key(game))
            editor.remove(key(game))
        } else {
            styles[key(game)] = style
            editor.putString(key(game), style.encode())
        }
        editor.apply()
    }
}

@Composable
fun rememberCartridgeStyle(game: Game): CartridgeStyle? {
    val context = LocalContext.current
    remember { CartridgeStyles.ensureLoaded(context) }
    return CartridgeStyles.get(game)
}

/**
 * Paints a cartridge picture in a chosen color. Only the plastic shell changes (low-saturation
 * pixels outside the label); label, gold contacts, stickers and the like keep their pixels.
 */
object CartridgeRecolor {
    private val cache = LruCache<String, ImageBitmap>(24)

    fun decode(
        context: Context,
        @DrawableRes res: Int,
    ): ImageBitmap {
        val key = "res:$res"
        cache.get(key)?.let { return it }
        val image = decodeBitmap(context, res).asImageBitmap()
        cache.put(key, image)
        return image
    }

    fun shell(
        context: Context,
        @DrawableRes res: Int,
        @DrawableRes labelRes: Int?,
        style: CartridgeStyle,
    ): ImageBitmap {
        val key = "shell:$res:${style.encode()}"
        cache.get(key)?.let { return it }
        val image = paint(context, res, labelRes, style).asImageBitmap()
        cache.put(key, image)
        return image
    }

    private fun decodeBitmap(
        context: Context,
        res: Int,
    ): Bitmap {
        val options =
            BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        return BitmapFactory.decodeResource(context.resources, res, options)
    }

    private fun paint(
        context: Context,
        res: Int,
        labelRes: Int?,
        style: CartridgeStyle,
    ): Bitmap {
        val source = decodeBitmap(context, res)
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val labelAlpha =
            labelRes?.let {
                val label = decodeBitmap(context, it)
                IntArray(w * h).also { out ->
                    if (label.width == w && label.height == h) label.getPixels(out, 0, w, 0, 0, w, h)
                }
            }

        val shell = BooleanArray(w * h)
        val histogram = IntArray(256)
        var count = 0
        for (i in pixels.indices) {
            val p = pixels[i]
            if ((p ushr 24) <= 200) continue
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            if (maxOf(r, g, b) - minOf(r, g, b) >= 45) continue
            if (labelAlpha != null && (labelAlpha[i] ushr 24) > 127) continue
            shell[i] = true
            histogram[(r + g + b) / 3]++
            count++
        }
        if (count == 0) return source

        // Stretch the shell's own light range over a dark-to-light ramp of the chosen color.
        var lo = 0
        var hi = 255
        var run = 0
        for (v in 0..255) {
            run += histogram[v]
            if (run >= count * 0.03) {
                lo = v
                break
            }
        }
        run = 0
        for (v in 255 downTo 0) {
            run += histogram[v]
            if (run >= count * 0.03) {
                hi = v
                break
            }
        }
        val range = (hi - lo).coerceAtLeast(1).toFloat()

        val color = CartridgePalette.colors[style.color]
        val cr = color.red * 255f
        val cg = color.green * 255f
        val cb = color.blue * 255f
        val dr = cr * 0.22f
        val dg = cg * 0.22f
        val db = cb * 0.22f
        val lr = minOf(255f, cr * 0.95f + 56f)
        val lg = minOf(255f, cg * 0.95f + 56f)
        val lb = minOf(255f, cb * 0.95f + 56f)

        val out = pixels.copyOf()
        for (i in pixels.indices) {
            if (!shell[i]) continue
            val p = pixels[i]
            val l = (((p shr 16) and 0xFF) + ((p shr 8) and 0xFF) + (p and 0xFF)) / 3
            val t = ((l - lo) / range).coerceIn(0f, 1f)
            val r = (dr * (1f - t) + lr * t).toInt().coerceIn(0, 255)
            val g = (dg * (1f - t) + lg * t).toInt().coerceIn(0, 255)
            val b = (db * (1f - t) + lb * t).toInt().coerceIn(0, 255)
            out[i] = (p and (0xFF shl 24)) or (r shl 16) or (g shl 8) or b
        }

        if (style.sparkly) {
            val random = Random(res * 31 + 7)
            for (i in out.indices) {
                if (!shell[i] || random.nextInt(55) != 0) continue
                val boost = intArrayOf(70, 110, 160)[random.nextInt(3)]
                brighten(out, i, boost)
                if (random.nextInt(8) == 0) {
                    val x = i % w
                    if (x > 0 && shell[i - 1]) brighten(out, i - 1, 70)
                    if (x < w - 1 && shell[i + 1]) brighten(out, i + 1, 70)
                    if (i >= w && shell[i - w]) brighten(out, i - w, 70)
                    if (i + w < out.size && shell[i + w]) brighten(out, i + w, 70)
                }
            }
        }

        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun brighten(
        pixels: IntArray,
        index: Int,
        amount: Int,
    ) {
        val p = pixels[index]
        val r = (((p shr 16) and 0xFF) + amount).coerceAtMost(255)
        val g = (((p shr 8) and 0xFF) + amount).coerceAtMost(255)
        val b = ((p and 0xFF) + amount).coerceAtMost(255)
        pixels[index] = (p and (0xFF shl 24)) or (r shl 16) or (g shl 8) or b
    }
}
