package com.swordfish.lemuroid.metadata.libretrodb

import com.swordfish.lemuroid.common.kotlin.filterNullable
import com.swordfish.lemuroid.lib.library.ArcadeFolders
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.Paprium
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.metadata.GameMetadata
import com.swordfish.lemuroid.lib.library.metadata.GameMetadataProvider
import com.swordfish.lemuroid.lib.storage.StorageFile
import com.swordfish.lemuroid.metadata.libretrodb.db.LibretroDBManager
import com.swordfish.lemuroid.metadata.libretrodb.db.LibretroDatabase
import com.swordfish.lemuroid.metadata.libretrodb.db.entity.LibretroRom
import timber.log.Timber
import java.util.Locale

class LibretroDBMetadataProvider(private val ovgdbManager: LibretroDBManager) :
    GameMetadataProvider {
    private val sortedSystemIds: List<String> by lazy {
        SystemID.values()
            .map { it.dbname }
            .sortedByDescending { it.length }
    }

    override suspend fun retrieveMetadata(storageFile: StorageFile): GameMetadata? {
        val db = ovgdbManager.dbInstance

        Timber.d("Looking metadata for file: $storageFile")

        // Arcade BIOS/system sets (neogeo.zip, pgm.zip...) in an arcade folder are not games.
        if (isArcadeBiosInArcadeFolder(storageFile)) {
            Timber.d("Skipping arcade BIOS set: ${storageFile.name}")
            return null
        }

        val metadata =
            runCatching {
                findArcadeByFolder(storageFile, db)
                    ?: findPaprium(storageFile)
                    ?: findByCRC(storageFile, db)
                    ?: findBySerial(storageFile, db)
                    ?: findByFilename(db, storageFile)
                    ?: findByPathAndFilename(db, storageFile)
                    ?: findByUniqueExtension(storageFile)
                    ?: findByKnownSystem(storageFile)
                    ?: findByPathAndSupportedExtension(storageFile)
            }.getOrElse {
                Timber.e("Error in retrieving $storageFile metadata: $it... Skipping.")
                null
            }

        metadata?.let { Timber.d("Metadata retrieved for item: $it") }

        return metadata
    }

    /** Paprium isn't in the libretro database; a ROM named like it is listed as a Genesis game. */
    private fun findPaprium(file: StorageFile): GameMetadata? {
        if (!Paprium.isPapriumRom(file.name)) return null
        return GameMetadata(
            name = "Paprium",
            romName = file.name,
            system = SystemID.GENESIS.dbname,
            developer = "WaterMelon",
        )
    }

    private fun convertToGameMetadata(rom: LibretroRom): GameMetadata {
        return GameMetadata(
            name = rom.name,
            romName = rom.romName,
            system = rom.system,
            developer = rom.developer,
        )
    }

    private suspend fun findByFilename(
        db: LibretroDatabase,
        file: StorageFile,
    ): GameMetadata? {
        return db.gameDao().findByFileName(file.name)
            .filterNullable { extractGameSystem(it).scanOptions.scanByFilename }
            ?.let { convertToGameMetadata(it) }
    }

    private suspend fun findByPathAndFilename(
        db: LibretroDatabase,
        file: StorageFile,
    ): GameMetadata? {
        // The same romset name can exist for several systems (e.g. FBNeo and MAME 2003-Plus):
        // check every entry, not just the first one.
        return db.gameDao().findAllByFileName(file.name)
            .filter { extractGameSystem(it).scanOptions.scanByPathAndFilename }
            .firstOrNull { parentContainsSystem(file.path, extractGameSystem(it).id.dbname) }
            ?.let { convertToGameMetadata(it) }
    }

    private fun isArcadeBiosInArcadeFolder(file: StorageFile): Boolean {
        if (!ArcadeFolders.isRomset(file.name) || !ArcadeFolders.isInArcadeFolder(file.path)) return false
        return ovgdbManager.arcadeTitles[file.name.lowercase()]?.startsWith(".") == true
    }

    /**
     * Cannoli-style arcade detection: every romset in an arcade folder is a game, whether or not
     * it is a merged, split or non-merged set. The libretro database decides the core and title
     * when it knows the set; otherwise FBNeo is used with the title from the arcade map.
     */
    private suspend fun findArcadeByFolder(
        file: StorageFile,
        db: LibretroDatabase,
    ): GameMetadata? {
        if (!ArcadeFolders.isRomset(file.name) || !ArcadeFolders.isInArcadeFolder(file.path)) return null

        val knownSets = db.gameDao().findAllByFileName(file.name)
        val fbneo = knownSets.firstOrNull { it.system == SystemID.FBNEO.dbname }
        val mame = knownSets.firstOrNull { it.system == SystemID.MAME2003PLUS.dbname }
        val preferred =
            if (ArcadeFolders.prefersMame(file.path)) {
                mame ?: fbneo
            } else {
                fbneo ?: mame
            }

        if (preferred != null) return convertToGameMetadata(preferred)

        return GameMetadata(
            name = ovgdbManager.arcadeTitles[file.name.lowercase()] ?: file.extensionlessName,
            romName = file.name,
            system = SystemID.FBNEO.dbname,
            developer = null,
        )
    }

    private fun findByPathAndSupportedExtension(file: StorageFile): GameMetadata? {
        val system =
            sortedSystemIds
                .filter { parentContainsSystem(file.path, it) }
                .map { GameSystem.findById(it) }
                .filter { it.scanOptions.scanByPathAndSupportedExtensions }
                .firstOrNull { it.supportedExtensions.contains(file.extension) }

        return system?.let {
            GameMetadata(
                name = file.extensionlessName,
                romName = file.name,
                system = it.id.dbname,
                developer = null,
            )
        }
    }

    private fun parentContainsSystem(
        parent: String?,
        dbname: String,
    ): Boolean {
        return parent?.toLowerCase(Locale.getDefault())?.contains(dbname) == true
    }

    private suspend fun findByCRC(
        file: StorageFile,
        db: LibretroDatabase,
    ): GameMetadata? {
        if (file.crc == null || file.crc == "0") return null
        return file.crc?.let { crc32 -> db.gameDao().findByCRC(crc32) }
            ?.let { convertToGameMetadata(it) }
    }

    private suspend fun findBySerial(
        file: StorageFile,
        db: LibretroDatabase,
    ): GameMetadata? {
        if (file.serial == null) return null
        return db.gameDao().findBySerial(file.serial!!)
            ?.let { convertToGameMetadata(it) }
    }

    private fun findByKnownSystem(file: StorageFile): GameMetadata? {
        if (file.systemID == null) return null

        return GameMetadata(
            name = file.extensionlessName,
            romName = file.name,
            system = file.systemID!!.dbname,
            developer = null,
        )
    }

    private fun findByUniqueExtension(file: StorageFile): GameMetadata? {
        val system = GameSystem.findByUniqueFileExtension(file.extension)

        if (system?.scanOptions?.scanByUniqueExtension == false) {
            return null
        }

        val result =
            system?.let {
                GameMetadata(
                    name = file.extensionlessName,
                    romName = file.name,
                    system = it.id.dbname,
                    developer = null,
                )
            }

        return result
    }

    private fun extractGameSystem(rom: LibretroRom): GameSystem {
        return GameSystem.findById(rom.system!!)
    }
}
