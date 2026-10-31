package com.swordfish.lemuroid.app.shared.artwork

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import timber.log.Timber
import java.io.File
import java.util.Locale

/**
 * Game artwork from a single folder, Lemuroid/artwork, with one image per game.
 *
 * An image matches a game by ROM file name without extension ("Super Mario World (USA).png" for
 * "Super Mario World (USA).sfc"), then by a custom name, the game title, and the title without
 * tags such as "(USA)". Case is ignored. Nothing is ever downloaded.
 */
object ArtworkManager {
    const val SHOW_ARTWORK_KEY = "pref_key_show_artwork"
    const val FOLDER_NAME = "artwork"

    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")

    /** Longest side of the decoded images; tiles are small, so this keeps memory low. */
    private const val MAX_IMAGE_SIZE = 512

    private val lock = Any()
    private var indexedFolderStamp = Long.MIN_VALUE
    private var index: Map<String, File> = emptyMap()

    private val cache =
        object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
            override fun sizeOf(
                key: String,
                value: Bitmap,
            ): Int = value.byteCount
        }

    fun isEnabled(context: Context): Boolean = preferences(context).getBoolean(SHOW_ARTWORK_KEY, false)

    fun preferences(context: Context): SharedPreferences = SharedPreferencesHelper.getSharedPreferences(context)

    fun getArtworkFolder(context: Context): File? {
        if (!DirectoriesManager.hasPublicStorageAccess(context)) return null
        return File(DirectoriesManager.getPublicRootDirectory(), FOLDER_NAME)
            .takeIf { it.isDirectory || it.mkdirs() }
    }

    /** The decoded image for the game, or null when there is none. Call off the main thread. */
    fun loadArtwork(
        context: Context,
        game: Game,
    ): Bitmap? {
        val file = findArtworkFile(context, game) ?: return null
        val cacheKey = "${file.absolutePath}:${file.lastModified()}"
        cache.get(cacheKey)?.let { return it }
        return runCatching { decode(file) }
            .onFailure { Timber.e(it, "Cannot read artwork ${file.name}") }
            .getOrNull()
            ?.also { cache.put(cacheKey, it) }
    }

    private fun findArtworkFile(
        context: Context,
        game: Game,
    ): File? {
        val folder = getArtworkFolder(context) ?: return null
        val files = indexFolder(folder)
        if (files.isEmpty()) return null
        return candidateNames(game).firstNotNullOfOrNull { files[it] }
    }

    private fun candidateNames(game: Game): List<String> {
        val fileBase = game.fileName.substringBeforeLast('.')
        return listOfNotNull(
            fileBase,
            game.customName,
            game.title,
            Game.cleanTitle(game.title),
            Game.cleanTitle(fileBase),
        )
            .map { normalize(it) }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun normalize(name: String): String = name.trim().lowercase(Locale.ROOT)

    /** Name (without extension, lower case) to file. Rebuilt only when the folder changes. */
    private fun indexFolder(folder: File): Map<String, File> {
        val stamp = folder.lastModified()
        synchronized(lock) {
            if (stamp == indexedFolderStamp) return index
            index =
                folder.listFiles()
                    .orEmpty()
                    .filter { it.isFile && it.extension.lowercase(Locale.ROOT) in IMAGE_EXTENSIONS }
                    .associateBy { normalize(it.nameWithoutExtension) }
            indexedFolderStamp = stamp
            return index
        }
    }

    private fun decode(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_IMAGE_SIZE) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }
}
