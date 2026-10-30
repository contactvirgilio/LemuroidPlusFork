package com.swordfish.lemuroid.app.shared.backup

import android.content.Context
import android.net.Uri
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Exports saves, save states and state previews to a single zip file, and imports them back.
 *
 * The zip contains up to three top-level folders: "saves", "states" and "state-previews",
 * mirroring the Lemuroid folder. Import only ever writes inside those folders.
 */
class SavesBackupManager(private val appContext: Context) {
    data class Result(val files: Int)

    private val directoriesManager = DirectoriesManager(appContext)

    private fun backupFolders(): Map<String, File> =
        mapOf(
            SAVES to directoriesManager.getSavesDirectory(),
            STATES to directoriesManager.getStatesDirectory(),
            STATE_PREVIEWS to directoriesManager.getStatesPreviewDirectory(),
        )

    /** Exports everything to [destination], reporting (files done, total files) to [onProgress]. */
    suspend fun export(
        destination: Uri,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Result =
        withContext(Dispatchers.IO) {
            // List the files first so progress can be shown as "x of y".
            val entries =
                backupFolders().flatMap { (name, folder) ->
                    folder.walkTopDown()
                        .filter { it.isFile }
                        .map { file -> "$name/${file.relativeTo(folder).invariantSeparatorsPath}" to file }
                        .toList()
                }
            onProgress(0, entries.size)

            val output =
                appContext.contentResolver.openOutputStream(destination, "wt")
                    ?: throw IOException("Cannot open $destination for writing")

            ZipOutputStream(output.buffered()).use { zip ->
                entries.forEachIndexed { index, (entryName, file) ->
                    zip.putNextEntry(ZipEntry(entryName).apply { time = file.lastModified() })
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    onProgress(index + 1, entries.size)
                }
            }
            Result(entries.size)
        }

    suspend fun import(source: Uri): Result =
        withContext(Dispatchers.IO) {
            val folders = backupFolders()
            var count = 0
            val input =
                appContext.contentResolver.openInputStream(source)
                    ?: throw IOException("Cannot open $source for reading")

            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val target = resolveTarget(entry, folders)
                    if (target != null) {
                        target.parentFile?.mkdirs()
                        // Keep the in-game save being replaced, like the single save file import does.
                        if (target.extension.equals("srm", ignoreCase = true) && target.length() > 0) {
                            target.copyTo(File(target.path + ".bak"), overwrite = true)
                        }
                        target.outputStream().use { zip.copyTo(it) }
                        if (entry.time > 0) target.setLastModified(entry.time)
                        count++
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            if (count == 0) {
                throw IOException("No saves found in this file")
            }
            Result(count)
        }

    /** Maps a zip entry to a file inside one of the backup folders, rejecting anything else. */
    private fun resolveTarget(
        entry: ZipEntry,
        folders: Map<String, File>,
    ): File? {
        if (entry.isDirectory) return null
        val name = entry.name.replace('\\', '/').trimStart('/')
        val topLevel = name.substringBefore('/')
        val relative = name.substringAfter('/', "")
        val folder = folders[topLevel] ?: return null
        if (relative.isEmpty()) return null

        val target = File(folder, relative).canonicalFile
        val root = folder.canonicalFile
        return target.takeIf { it.path.startsWith(root.path + File.separator) }
    }

    companion object {
        private const val SAVES = "saves"
        private const val STATES = "states"
        private const val STATE_PREVIEWS = "state-previews"
    }
}
