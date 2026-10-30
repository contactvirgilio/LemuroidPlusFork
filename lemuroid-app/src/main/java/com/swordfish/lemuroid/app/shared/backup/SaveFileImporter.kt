package com.swordfish.lemuroid.app.shared.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Imports in-game save files (.sav, .srm, ...) picked by the user from anywhere on the device.
 * Each file is matched to a game by name; files that cannot be matched are assigned by the user.
 */
class SaveFileImporter(
    private val appContext: Context,
    private val retrogradeDb: RetrogradeDatabase,
) {
    data class PickedFile(val uri: Uri, val name: String) {
        val isBackupZip: Boolean
            get() = name.endsWith(".zip", ignoreCase = true)
    }

    data class Plan(
        val matched: List<Pair<PickedFile, Game>>,
        val unmatched: List<PickedFile>,
        val games: List<Game>,
    )

    private val savesManager = SavesManager(DirectoriesManager(appContext))

    suspend fun describe(uris: List<Uri>): List<PickedFile> =
        withContext(Dispatchers.IO) {
            uris.map { PickedFile(it, displayName(it)) }
        }

    suspend fun plan(files: List<PickedFile>): Plan =
        withContext(Dispatchers.IO) {
            val games = retrogradeDb.gameDao().selectAll()
            val gamesByBaseName = games.groupBy { baseName(it.fileName).lowercase() }

            val matched = mutableListOf<Pair<PickedFile, Game>>()
            val unmatched = mutableListOf<PickedFile>()

            files.forEach { file ->
                val candidates = gamesByBaseName[baseName(file.name).lowercase()].orEmpty()
                if (candidates.size == 1) {
                    matched += file to candidates.first()
                } else {
                    unmatched += file
                }
            }
            Plan(matched, unmatched, games)
        }

    /**
     * Imports save files found inside a zip (not in Lemuroid's backup folders). They are matched to
     * games by name only; returns (imported, skipped file names).
     */
    suspend fun importLooseSaves(saves: List<Pair<String, ByteArray>>): Pair<Int, List<String>> {
        if (saves.isEmpty()) return 0 to emptyList()
        val games = withContext(Dispatchers.IO) { retrogradeDb.gameDao().selectAll() }
        val gamesByBaseName = games.groupBy { baseName(it.fileName).lowercase() }

        var imported = 0
        val skipped = mutableListOf<String>()
        saves.forEach { (name, data) ->
            val game = gamesByBaseName[baseName(name).trim().lowercase()]?.singleOrNull()
            if (game == null || data.isEmpty()) {
                skipped += name
            } else {
                savesManager.importSaveRAM(game, data)
                imported++
            }
        }
        return imported to skipped
    }

    suspend fun import(
        file: PickedFile,
        game: Game,
    ) {
        val data =
            withContext(Dispatchers.IO) {
                appContext.contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                    ?: throw IOException("Cannot read ${file.name}")
            }
        savesManager.importSaveRAM(game, data)
    }

    private fun displayName(uri: Uri): String {
        return runCatching { DocumentFile.fromSingleUri(appContext, uri)?.name }.getOrNull()
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: uri.toString()
    }

    private fun baseName(fileName: String) = fileName.substringBeforeLast(".")
}
