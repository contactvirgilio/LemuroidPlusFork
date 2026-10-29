package com.swordfish.lemuroid.lib.saves

import com.swordfish.lemuroid.common.kotlin.runCatchingWithRetry
import com.swordfish.lemuroid.common.kotlin.writeBytesAtomic
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.migrators.getSavesMigrator
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SavesManager(private val directoriesManager: DirectoriesManager) {
    suspend fun getSaveRAM(
        game: Game,
        systemCoreConfig: SystemCoreConfig,
    ): ByteArray? {
        return withContext(Dispatchers.IO) {
            val result =
                runCatchingWithRetry(FILE_ACCESS_RETRIES) {
                    val saveFile = getSaveFile(getSaveRAMFileName(game))
                    val importedSav = importSavFile(game, saveFile)
                    if (importedSav != null) {
                        importedSav
                    } else if (saveFile.exists() && saveFile.length() > 0) {
                        saveFile.readBytes()
                    } else {
                        val savesMigrator = systemCoreConfig.getSavesMigrator()
                        savesMigrator?.loadPreviousSaveForGame(game, directoriesManager)
                    }
                }
            result.getOrNull()
        }
    }

    suspend fun setSaveRAM(
        game: Game,
        data: ByteArray,
    ) {
        withContext(Dispatchers.IO) {
            val result =
                runCatchingWithRetry(FILE_ACCESS_RETRIES) {
                    if (data.isEmpty()) {
                        return@runCatchingWithRetry
                    }

                    val saveFile = getSaveFile(getSaveRAMFileName(game))
                    saveFile.writeBytesAtomic(data)
                }
            result.getOrNull()
        }
    }

    suspend fun getSaveRAMInfo(game: Game): SaveInfo {
        return withContext(Dispatchers.IO) {
            val saveFile = getSaveFile(getSaveRAMFileName(game))
            val fileExists = saveFile.exists() && saveFile.length() > 0
            SaveInfo(fileExists, saveFile.lastModified())
        }
    }

    private suspend fun getSaveFile(fileName: String): File {
        return withContext(Dispatchers.IO) {
            val savesDirectory = directoriesManager.getSavesDirectory()
            File(savesDirectory, fileName)
        }
    }

    /**
     * Lets users bring saves from other emulators or flash carts: a "<ROM name>.sav" file (any case)
     * in the saves folder is used when there is no .srm yet, or when it is newer than the .srm.
     * It is copied to the .srm and renamed to ".sav.imported" so it is only imported once.
     */
    private fun importSavFile(
        game: Game,
        srmFile: File,
    ): ByteArray? {
        val baseName = game.fileName.substringBeforeLast(".")
        val savFile =
            srmFile.parentFile
                ?.listFiles()
                ?.firstOrNull { it.isFile && it.name.equals("$baseName.sav", ignoreCase = true) }
                ?: return null

        val srmIsNewer = srmFile.exists() && srmFile.length() > 0 && srmFile.lastModified() >= savFile.lastModified()
        if (savFile.length() == 0L || srmIsNewer) return null

        val data = savFile.readBytes()
        srmFile.writeBytesAtomic(data)

        val importedFile = File(savFile.path + IMPORTED_SUFFIX)
        importedFile.delete()
        savFile.renameTo(importedFile)
        return data
    }

    /** This name should make it compatible with RetroArch so that users can freely sync saves across the two application. */
    private fun getSaveRAMFileName(game: Game) = "${game.fileName.substringBeforeLast(".")}.srm"

    companion object {
        private const val FILE_ACCESS_RETRIES = 3
        private const val IMPORTED_SUFFIX = ".imported"
    }
}
