package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.Context
import android.net.Uri
import com.swordfish.lemuroid.lib.library.db.entity.Game

/**
 * The Boktai games (GBA) have a sun sensor in the cartridge; the game menu only shows the Sun
 * sensor tile for them. Found by name, or by the game code inside the ROM for renamed files.
 */
object Boktai {
    private val NAMES = listOf("boktai", "bokura no taiyou", "bokura no taiyo")

    // Boktai 1, 2 and 3 (Shin Bokura no Taiyou): 3-letter start of the 4-letter GBA game code.
    private val CODES = setOf("U3I", "U32", "U33")

    fun isBoktai(
        context: Context,
        game: Game,
    ): Boolean {
        if (game.systemId != "gba") return false
        val name = (game.title + " " + game.fileName).lowercase()
        if (NAMES.any { it in name }) return true
        return runCatching { CODES.contains(gameCode(context, Uri.parse(game.fileUri))?.take(3)) }.getOrDefault(false)
    }

    /** The game code at 0xAC in a GBA ROM header (unreadable for zipped ROMs: then by name only). */
    private fun gameCode(
        context: Context,
        uri: Uri,
    ): String? {
        if (!isPlainGbaFile(uri)) return null
        context.contentResolver.openInputStream(uri)?.use { input ->
            val header = ByteArray(0xB0)
            var read = 0
            while (read < header.size) {
                val n = input.read(header, read, header.size - read)
                if (n < 0) return null
                read += n
            }
            return String(header, 0xAC, 4, Charsets.US_ASCII)
        }
        return null
    }

    private fun isPlainGbaFile(uri: Uri): Boolean = uri.toString().lowercase().endsWith(".gba")
}
