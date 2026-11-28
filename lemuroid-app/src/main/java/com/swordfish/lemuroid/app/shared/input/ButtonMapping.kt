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
 * which button the game receives; [UNBOUND] means nothing. The logic follows Cannoli's (2026-11-21):
 * no swapping, a button can be unbound, two buttons may send the same thing, and a game stores
 * only what differs from its system, laid over the system's mapping when playing.
 */
object ButtonMapping {
    private const val FILE = "button_mapping.properties"

    /** "Sends nothing." */
    const val UNBOUND = 0

    fun systemKey(systemId: String) = "sys:$systemId"

    /**
     * The id mappings are kept under: the game's system, except Neo Geo and CPS2 games, which run on
     * the arcade cores but have their own button layouts (and their own tile in Settings).
     */
    fun mappingId(
        systemId: String,
        fileName: String,
    ): String {
        if (systemId != "fbneo" && systemId != "mame2003plus") return systemId
        return when {
            com.swordfish.lemuroid.app.shared.game.NeoGeoSets.isNeoGeo(fileName) -> "neogeo"
            com.swordfish.lemuroid.app.shared.game.Cps2Sets.isCps2(fileName) -> "cps2"
            else -> systemId
        }
    }

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

    private fun encode(map: Map<Int, Int>): String = map.entries.joinToString(",") { "${it.key}:${it.value}" }

    /** The stored mapping (pressed -> sent) for [key], or empty. */
    fun get(
        context: Context,
        key: String,
    ): Map<Int, Int> = decode(load(context).getProperty(key))

    fun has(
        context: Context,
        key: String,
    ): Boolean = load(context).getProperty(key)?.isNotEmpty() == true

    /** Saves [map] as is: callers drop the entries that need no storing (see [systemDelta], [gameDelta]). */
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

    /** What a system needs stored: buttons that don't send themselves (defaults included, harmless). */
    fun systemDelta(map: Map<Int, Int>): Map<Int, Int> = map.filter { it.key != it.value }

    /** What a game needs stored: buttons that send something else than they do for its system. */
    fun gameDelta(
        map: Map<Int, Int>,
        system: Map<Int, Int>,
    ): Map<Int, Int> {
        val keys = map.keys + system.keys
        return keys.mapNotNull { k ->
            val game = map[k] ?: k
            val sys = system[k] ?: k
            if (game != sys) k to game else null
        }.toMap()
    }

    /**
     * Systems whose real controller has no X or Y: some cores put hidden rapid fire there
     * (mGBA and Gambatte: X/Y = turbo A/B; FCEUmm's turbo buttons), so X and Y send nothing by
     * default (owner, 2026-11-22). Mapping X or Y in Button mapping brings it back.
     */
    private val NO_XY = setOf("gb", "gbc", "gba", "nes", "sms", "gg", "pce", "ngp", "ngc", "lynx", "atari2600", "atari7800")

    fun defaults(systemId: String): Map<Int, Int> =
        if (systemId in NO_XY) {
            mapOf(KeyEvent.KEYCODE_BUTTON_X to UNBOUND, KeyEvent.KEYCODE_BUTTON_Y to UNBOUND)
        } else {
            emptyMap()
        }

    /** The mapping in effect for a game: the defaults, its system's, then the game's own differences. */
    fun effective(
        context: Context,
        systemId: String,
        fileName: String?,
    ): Map<Int, Int> {
        val props = load(context)
        val system = decode(props.getProperty(systemKey(systemId)))
        val game = fileName?.let { decode(props.getProperty(gameKey(systemId, it))) }.orEmpty()
        return defaults(systemId) + system + game
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
            KeyEvent.KEYCODE_DPAD_UP -> "Up"
            KeyEvent.KEYCODE_DPAD_DOWN -> "Down"
            KeyEvent.KEYCODE_DPAD_LEFT -> "Left"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "Right"
            UNBOUND -> "—"
            else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        }

    /** Buttons that can be mapped, in list order: face, shoulders, sticks, Start/Select, D-pad. */
    val MAPPABLE =
        listOf(
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR,
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        )

    /** Whether PlayStation player 1 is set to the DualShock (the only pad with L3/R3). */
    fun psxDualShock(context: Context): Boolean =
        com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper.getSharedPreferences(context).all.any { (key, value) ->
            key.startsWith("pref_key_controller_type_psx_") && key.endsWith("_0") && value.toString().contains("dualshock", ignoreCase = true)
        }

    // ---- Shortcuts (2026-11-22): button combos for the game menu, fast forward, save, load menu.

    enum class Shortcut { MENU, FAST_FORWARD, SAVE_STATE, LOAD_MENU, SCREENSHOT }

    /** Defaults: hold Select and press another button. */
    val DEFAULT_SHORTCUTS: Map<Shortcut, Set<Int>> =
        mapOf(
            Shortcut.MENU to setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_START),
            Shortcut.FAST_FORWARD to setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_R2),
            Shortcut.SAVE_STATE to setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_R1),
            Shortcut.LOAD_MENU to setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_L1),
            Shortcut.SCREENSHOT to setOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_L2),
        )

    const val GLOBAL_SHORTCUTS = "sc:all"

    /** Settings > Controls > Shortcuts switch (on by default). */
    const val SHORTCUTS_ENABLED_KEY = "pref_key_shortcuts_enabled"

    fun shortcutsEnabled(context: Context): Boolean =
        com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper.getSharedPreferences(context).getBoolean(SHORTCUTS_ENABLED_KEY, true)

    fun shortcutsKey(mappingKey: String) = "sc:$mappingKey"

    private fun decodeShortcuts(text: String?): Map<Shortcut, Set<Int>>? {
        if (text == null) return null
        return text.split(';').mapNotNull { entry ->
            val name = entry.substringBefore('=', "")
            val type = Shortcut.values().firstOrNull { it.name == name } ?: return@mapNotNull null
            val keys = entry.substringAfter('=', "").split('+').mapNotNull { it.toIntOrNull() }.toSet()
            type to keys
        }.toMap()
    }

    private fun encodeShortcuts(map: Map<Shortcut, Set<Int>>): String =
        map.entries.joinToString(";") { "${it.key.name}=${it.value.joinToString("+")}" }

    fun getShortcuts(
        context: Context,
        key: String,
    ): Map<Shortcut, Set<Int>>? = decodeShortcuts(load(context).getProperty(key))

    fun setShortcuts(
        context: Context,
        key: String,
        map: Map<Shortcut, Set<Int>>?,
    ) {
        val props = load(context)
        if (map == null) props.remove(key) else props.setProperty(key, encodeShortcuts(map))
        save(context, props)
    }

    /** Shortcuts in effect: the game's, else its system's, else the ones for all systems, else the defaults. */
    fun effectiveShortcuts(
        context: Context,
        systemId: String?,
        fileName: String?,
    ): Map<Shortcut, Set<Int>> {
        val props = load(context)
        val game = if (systemId != null && fileName != null) decodeShortcuts(props.getProperty(shortcutsKey(gameKey(systemId, fileName)))) else null
        val system = systemId?.let { decodeShortcuts(props.getProperty(shortcutsKey(systemKey(it)))) }
        val all = decodeShortcuts(props.getProperty(GLOBAL_SHORTCUTS))
        return DEFAULT_SHORTCUTS + (all ?: emptyMap()) + (system ?: emptyMap()) + (game ?: emptyMap())
    }

    /** Used while a game runs: re-reads the file only when it changed. */
    class Runtime(
        private val context: Context,
        gameSystemId: String,
        private val fileName: String,
    ) {
        private val systemId = mappingId(gameSystemId, fileName)

        private var stamp = -1L
        private var map: Map<Int, Int> = emptyMap()

        fun apply(keyCode: Int): Int {
            val f = file(context)
            val now = if (f.exists()) f.lastModified() else 0L
            if (now != stamp) {
                stamp = now
                map = effective(context, systemId, fileName)
            }
            // UNBOUND (0) means: don't send anything.
            return map[keyCode] ?: keyCode
        }

        /** The shortcut whose combo is complete now that [keyCode] went down, if any. */
        fun shortcutFor(
            keyCode: Int,
            held: Set<Int>,
        ): Shortcut? {
            if (!shortcutsEnabled(context)) return null
            apply(keyCode) // refreshes the file if it changed
            return shortcuts.entries.firstOrNull { (_, keys) ->
                keys.size >= 2 && keyCode in keys && held.containsAll(keys)
            }?.key
        }

        private val shortcuts: Map<Shortcut, Set<Int>>
            get() {
                if (shortcutsStamp != stamp) {
                    shortcutsStamp = stamp
                    shortcutsCache = effectiveShortcuts(context, systemId, fileName)
                }
                return shortcutsCache
            }
        private var shortcutsStamp = -2L
        private var shortcutsCache: Map<Shortcut, Set<Int>> = DEFAULT_SHORTCUTS
    }

    /** Arcade cores (incl. Neo Geo / CPS2 on FBNeo): the only ones whose button names we show (owner, 2026-11-28). */
    private val NAMED_BUTTON_SYSTEMS = setOf("fbneo", "mame2003plus", "neogeo", "cps2")

    fun showsButtonNames(systemId: String): Boolean = systemId in NAMED_BUTTON_SYSTEMS

    private fun buttonNamesKey(fileName: String) = "button_names_$fileName"

    /** Saves the game's own button names (Android key code to name), reported by the core when the game runs. */
    fun saveButtonNames(
        context: Context,
        fileName: String,
        names: Map<Int, String>,
    ) {
        val text = names.entries.joinToString("\n") { "${it.key}\t${it.value}" }
        com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper.getSharedPreferences(context)
            .edit().putString(buttonNamesKey(fileName), text).apply()
    }

    fun buttonNames(
        context: Context,
        fileName: String?,
    ): Map<Int, String> {
        if (fileName == null) return emptyMap()
        val text =
            com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper.getSharedPreferences(context)
                .getString(buttonNamesKey(fileName), null) ?: return emptyMap()
        return text.lines().mapNotNull { line ->
            val key = line.substringBefore('\t').toIntOrNull() ?: return@mapNotNull null
            key to line.substringAfter('\t')
        }.toMap()
    }

    /** RetroPad button id (libretro) to the Android key code our mapping list uses. */
    val RETROPAD_TO_KEYCODE =
        mapOf(
            0 to KeyEvent.KEYCODE_BUTTON_B,
            1 to KeyEvent.KEYCODE_BUTTON_Y,
            2 to KeyEvent.KEYCODE_BUTTON_SELECT,
            3 to KeyEvent.KEYCODE_BUTTON_START,
            8 to KeyEvent.KEYCODE_BUTTON_A,
            9 to KeyEvent.KEYCODE_BUTTON_X,
            10 to KeyEvent.KEYCODE_BUTTON_L1,
            11 to KeyEvent.KEYCODE_BUTTON_R1,
            12 to KeyEvent.KEYCODE_BUTTON_L2,
            13 to KeyEvent.KEYCODE_BUTTON_R2,
            14 to KeyEvent.KEYCODE_BUTTON_THUMBL,
            15 to KeyEvent.KEYCODE_BUTTON_THUMBR,
        )
}
