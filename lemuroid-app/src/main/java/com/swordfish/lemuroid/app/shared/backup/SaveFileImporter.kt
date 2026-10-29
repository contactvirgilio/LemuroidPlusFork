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
    data class PickedFile(val uri: Uri, val name: String)

    data class Plan(
        val matched: List<Pair<PickedFile, Game>>,
        val unmatched: List<PickedFile>,
        val games: List<Game>,
    )

    private val savesManager = SavesManager(DirectoriesManager(appContext))

    suspend fun plan(uris: List<Uri>): Plan =
        withContext(Dispatchers.IO) {
            val games = retrogradeDb.gameDao().selectAll()
            val gamesByBaseName = games.groupBy { baseName(it.fileName).lowercase() }

            val files = uris.map { PickedFile(it, displayName(it)) }
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
