package com.swordfish.lemuroid.lib.library

import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.library.db.entity.Game

/**
 * Paprium (2020) uses a custom cartridge chip that the regular Genesis cores don't emulate.
 * It is run automatically with a dedicated Genesis Plus GX build that supports it, while every
 * other Genesis game keeps the core chosen in Settings > Core selection.
 *
 * The game's music is read from MP3 files in a "paprium" folder next to the ROM.
 */
object Paprium {
    private const val NAME = "paprium"
    private val ROM_EXTENSIONS = setOf("bin", "md", "gen", "smd")

    /** True for a ROM file named like Paprium (e.g. "Paprium.bin"), so it is listed as a Genesis game. */
    fun isPapriumRom(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extension in ROM_EXTENSIONS && fileName.contains(NAME, ignoreCase = true)
    }

    fun isPaprium(game: Game): Boolean {
        return game.systemId == SystemID.GENESIS.dbname &&
            (game.fileName.contains(NAME, ignoreCase = true) || game.title.contains(NAME, ignoreCase = true))
    }

    val CORE_CONFIG =
        SystemCoreConfig(
            CoreID.GENESIS_PLUS_GX_PAPRIUM,
            defaultSettings =
                listOf(
                    // Look for the soundtrack as MP3 first (WAV and OGG are still found as a fallback).
                    CoreVariable("punchium_audio_track_format", "mp3"),
                    // Caches decoded graphics, which makes the game run smoother on handhelds.
                    CoreVariable("punchium_tile_cache", "enabled"),
                ),
            controllerConfigs =
                hashMapOf(
                    0 to arrayListOf(ControllerConfigs.GENESIS_6, ControllerConfigs.GENESIS_3),
                    1 to arrayListOf(ControllerConfigs.GENESIS_6, ControllerConfigs.GENESIS_3),
                ),
        )
}
