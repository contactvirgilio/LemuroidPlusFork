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

    /**
     * Replaces the game's in-game save with [data] (e.g. a .sav from another emulator).
     * The previous save, if any, is kept as ".srm.bak" so the import can be undone.
     */
    suspend fun importSaveRAM(
        game: Game,
        data: ByteArray,
    ) {
        require(data.isNotEmpty()) { "The save file is empty" }
        withContext(Dispatchers.IO) {
            val saveFile = getSaveFile(getSaveRAMFileName(game))
            backUpExistingSave(saveFile)
            saveFile.writeBytesAtomic(withoutExtraFooter(data))
            deleteAutoSaveStates(game)
        }
    }

    /**
     * An auto-save state holds its own copy of the cartridge RAM and is restored right after the
     * save file is loaded, which would hide an imported save (and write the old RAM back on exit).
     * Remove it so the next launch starts fresh from the imported save. Save slots are kept.
     */
    private fun deleteAutoSaveStates(game: Game) {
        val autoSaveName = "${game.fileName}.state"
        directoriesManager.getStatesDirectory()
            .listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { coreDirectory ->
                File(coreDirectory, autoSaveName).delete()
                File(coreDirectory, "$autoSaveName.metadata").delete()
            }
    }

    /**
     * Some emulators append a small block after the cartridge RAM (e.g. a real-time clock for
     * Pokemon Gold/Silver/Crystal), giving sizes like 32,808 instead of 32,768 bytes. Cores expect
     * the plain RAM, so a few extra bytes after a power-of-two size are dropped.
     */
    private fun withoutExtraFooter(data: ByteArray): ByteArray {
        if (data.size < MIN_SAVE_SIZE) return data
        val ramSize = Integer.highestOneBit(data.size)
        val extra = data.size - ramSize
        return if (extra in 1..MAX_FOOTER_SIZE) data.copyOf(ramSize) else data
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

        val data = withoutExtraFooter(savFile.readBytes())
        backUpExistingSave(srmFile)
        srmFile.writeBytesAtomic(data)
        // Loaded before the auto-save state, so removing it here still applies to this launch.
        deleteAutoSaveStates(game)

        val importedFile = File(savFile.path + IMPORTED_SUFFIX)
        importedFile.delete()
        savFile.renameTo(importedFile)
        return data
    }

    private fun backUpExistingSave(saveFile: File) {
        if (!saveFile.exists() || saveFile.length() == 0L) return
        saveFile.copyTo(File(saveFile.path + BACKUP_SUFFIX), overwrite = true)
    }

    /** This name should make it compatible with RetroArch so that users can freely sync saves across the two application. */
    private fun getSaveRAMFileName(game: Game) = "${game.fileName.substringBeforeLast(".")}.srm"

    companion object {
        private const val FILE_ACCESS_RETRIES = 3
        private const val IMPORTED_SUFFIX = ".imported"
        private const val BACKUP_SUFFIX = ".bak"
        private const val MIN_SAVE_SIZE = 512
        private const val MAX_FOOTER_SIZE = 64
    }
}
