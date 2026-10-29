package com.swordfish.lemuroid.lib.library

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

/**
 * Folder-based arcade handling, similar to Cannoli: every .zip/.7z inside a folder named like an
 * arcade folder (Arcade, MAME, FBNeo, FBN, Neo Geo, CPS...) is an arcade romset, whatever kind of set
 * it is (merged, split or non-merged). The emulator core resolves the set when the game starts.
 */
object ArcadeFolders {
    val ROMSET_EXTENSIONS = setOf("zip", "7z")

    private val EXACT_NAMES =
        setOf(
            "arcade", "arcades", "fbneo", "fbn", "fba", "finalburn", "finalburnneo", "finalburn neo",
            "mame", "neogeo", "neo geo", "neo-geo", "cps1", "cps2", "cps3", "cps",
        )

    private val PREFIXES = listOf("mame", "fbneo", "fbalpha", "arcade")

    /** True if any folder in [path] (a file path or a document URI path) is an arcade folder. */
    fun isInArcadeFolder(path: String?): Boolean = folderNames(path).any { isArcadeFolderName(it) }

    /** True if the arcade folder in [path] is a MAME folder, so MAME 2003-Plus should be preferred. */
    fun prefersMame(path: String?): Boolean = folderNames(path).any { it.startsWith("mame") }

    fun isRomset(fileName: String): Boolean = fileName.substringAfterLast('.', "").lowercase() in ROMSET_EXTENSIONS

    fun isArcadeSystem(systemId: String): Boolean =
        systemId == SystemID.FBNEO.dbname || systemId == SystemID.MAME2003PLUS.dbname

    private fun isArcadeFolderName(name: String): Boolean = name in EXACT_NAMES || PREFIXES.any { name.startsWith(it) }

    private fun folderNames(path: String?): List<String> {
        if (path == null) return emptyList()
        val segments =
            path.split('/', ':', '\\')
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
        return segments.dropLast(1)
    }

    /**
     * Real file path for a storage-framework document on the main storage or an SD card, so arcade
     * cores can find parent sets and BIOS zips next to the game. Null if it cannot be read directly
     * (for example without "All files access").
     */
    fun directFileFor(documentUri: Uri): File? {
        val documentId = runCatching { DocumentsContract.getDocumentId(documentUri) }.getOrNull() ?: return null
        val volume = documentId.substringBefore(':', "")
        val relativePath = documentId.substringAfter(':', "")
        if (volume.isEmpty() || relativePath.isEmpty()) return null

        val root =
            if (volume.equals("primary", ignoreCase = true)) {
                Environment.getExternalStorageDirectory()
            } else {
                File("/storage/$volume")
            }
        return File(root, relativePath).takeIf { it.isFile && it.canRead() }
    }
}
