package com.swordfish.lemuroid.app.shared.input

import android.content.Context
import android.view.KeyEvent
import java.io.File
import java.util.Properties

/**
 * Button mapping per system, or per game (2026-11-20). Kept in a small file rather than shared
 * preferences: the menu and the game run in different processes, and a file is always read fresh.
 *
 * A mapping says, for a button you press (in RetroPad terms, after the gamepad's own bindings),
 * which button the game receives. Only changed buttons are stored. A game's mapping replaces its
 * system's.
 */
object ButtonMapping {
    private const val FILE = "button_mapping.properties"

    fun systemKey(systemId: String) = "sys:$systemId"

    fun gameKey(
        systemId: String,
        fileName: String,
    ) = "game:$systemId/$fileName"

    private fun file(context: Context) = File(context.applicationContext.filesDir, FILE)

    private fun load(context: Context): Properties {
        val props = Properties()
        val f = file(context)
        if (f.exists()) runCatching { f.inputStream().use { props.load(it) } }
        return props
    }

    private fun save(
        context: Context,
        props: Properties,
    ) {
        val f = file(context)
        val temp = File(f.path + ".tmp")
        temp.outputStream().use { props.store(it, null) }
        temp.renameTo(f)
    }

    private fun decode(text: String?): Map<Int, Int> =
        text.orEmpty().split(',').mapNotNull { pair ->
            val parts = pair.split(':')
            val from = parts.getOrNull(0)?.toIntOrNull()
            val to = parts.getOrNull(1)?.toIntOrNull()
            if (from != null && to != null) from to to else null
        }.toMap()

    private fun encode(map: Map<Int, Int>): String = map.filter { it.key != it.value }.entries.joinToString(",") { "${it.key}:${it.value}" }

    /** The stored mapping (pressed -> sent) for [key], or empty. */
    fun get(
        context: Context,
        key: String,
    ): Map<Int, Int> = decode(load(context).getProperty(key))

    fun has(
        context: Context,
        key: String,
    ): Boolean = load(context).getProperty(key)?.isNotEmpty() == true

    fun set(
        context: Context,
        key: String,
        map: Map<Int, Int>,
    ) {
        val props = load(context)
        val text = encode(map)
        if (text.isEmpty()) props.remove(key) else props.setProperty(key, text)
        save(context, props)
    }

    /**
     * Makes [target] (a button the game knows) come from [pressed]. The button that used to give
     * [target] takes over whatever [pressed] gave before, so nothing gets lost: a swap.
     */
    fun assign(
        map: Map<Int, Int>,
        buttons: List<Int>,
        target: Int,
        pressed: Int,
    ): Map<Int, Int> {
        val out = map.toMutableMap()
        fun sentBy(key: Int) = out[key] ?: key
        val oldSource = (buttons + out.keys + pressed).distinct().firstOrNull { sentBy(it) == target } ?: target
        val pressedSent = sentBy(pressed)
        out[pressed] = target
        if (oldSource != pressed) out[oldSource] = pressedSent
        return out.filter { it.key != it.value }
    }

    /** Short name of a RetroPad button, as printed on the RPC and most pads. */
    fun keyName(keyCode: Int): String =
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> "A"
            KeyEvent.KEYCODE_BUTTON_B -> "B"
            KeyEvent.KEYCODE_BUTTON_X -> "X"
            KeyEvent.KEYCODE_BUTTON_Y -> "Y"
            KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
            KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
            KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
            KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
            KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
            KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
            KeyEvent.KEYCODE_BUTTON_START -> "Start"
            KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
            else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        }

    /** Buttons that can be mapped (the D-pad stays as it is). */
    val MAPPABLE =
        setOf(
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR,
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT,
        )

    /** Used while a game runs: re-reads the file only when it changed. */
    class Runtime(
        private val context: Context,
        private val systemId: String,
        private val fileName: String,
    ) {
        private var stamp = -1L
        private var map: Map<Int, Int> = emptyMap()

        fun apply(keyCode: Int): Int {
            val f = file(context)
            val now = if (f.exists()) f.lastModified() else 0L
            if (now != stamp) {
                stamp = now
                val props = load(context)
                val game = decode(props.getProperty(gameKey(systemId, fileName)))
                map = game.ifEmpty { decode(props.getProperty(systemKey(systemId))) }
            }
            return map[keyCode] ?: keyCode
        }
    }
}
