package com.swordfish.lemuroid.app.shared.screenshots

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import timber.log.Timber
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Game screenshots from one folder, Lemuroid/Screenshots, one picture per game (owner, 2026-11-25:
 * replaces the old "Show artwork"). "Take screenshot" in the game writes "<ROM file name>.png" here,
 * replacing the previous one. Shown as the game's picture in the Screenshots style.
 *
 * An image matches a game by ROM file name without extension ("Super Mario World (USA).png" for
 * "Super Mario World (USA).sfc"), then by a custom name, the game title, and the title without
 * tags such as "(USA)". Case is ignored. Nothing is ever downloaded.
 */
object ScreenshotManager {
    /** The Screenshots style (third style next to Tiles and Cartridges). */
    const val STYLE_KEY = "pref_key_screenshot_style"
    /** The camera shortcut in the game (on by default). */
    const val BUTTON_KEY = "pref_key_screenshot_button"
    const val FOLDER_NAME = "Screenshots"

    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")

    /** Longest side of the decoded images; tiles are small, so this keeps memory low. */
    private const val MAX_IMAGE_SIZE = 1024

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

    // Game to the cache entry of its image, so a tile coming back on screen can show the image
    // straight away instead of waiting for the background lookup.
    private val lastKeys = ConcurrentHashMap<String, String>()

    private fun gameKey(game: Game) = "${game.id}:${game.fileName}"

    /** The image if it was already loaded for this game. Safe on the main thread (no file access). */
    fun peekScreenshot(game: Game): Bitmap? = lastKeys[gameKey(game)]?.let { cache.get(it) }

    fun isEnabled(context: Context): Boolean = preferences(context).getBoolean(STYLE_KEY, false)

    fun isButtonEnabled(context: Context): Boolean = preferences(context).getBoolean(BUTTON_KEY, true)

    fun preferences(context: Context): SharedPreferences = SharedPreferencesHelper.getSharedPreferences(context)

    /** Lemuroid/Screenshots, or the app's own folder when there is no access to the Lemuroid folder. */
    fun getScreenshotsFolder(context: Context): File? {
        val public =
            if (DirectoriesManager.hasPublicStorageAccess(context)) {
                File(DirectoriesManager.getPublicRootDirectory(), FOLDER_NAME).takeIf { it.isDirectory || it.mkdirs() }
            } else {
                null
            }
        return public ?: File(context.getExternalFilesDir(null) ?: context.filesDir, FOLDER_NAME).takeIf { it.isDirectory || it.mkdirs() }
    }

    /**
     * Saves [bitmap] as the game's screenshot, replacing the previous one. Written to a temporary
     * file and renamed, so the library never reads a half-written picture.
     */
    fun saveScreenshot(
        context: Context,
        game: Game,
        bitmap: Bitmap,
    ): Boolean {
        val folder = getScreenshotsFolder(context) ?: return false
        val base = game.fileName.substringBeforeLast('.')
        val target = File(folder, "$base.png")
        val temp = File(folder, ".$base.tmp")
        return runCatching {
            temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            // Older pictures of this game under another extension would win the lookup otherwise.
            IMAGE_EXTENSIONS.filter { it != "png" }.forEach { File(folder, "$base.$it").delete() }
            target.delete()
            temp.renameTo(target)
        }.onFailure { Timber.e(it, "Cannot save screenshot") }.getOrDefault(false)
    }

    /** Bumped when the library comes back on screen, so tiles pick up screenshots taken in the game. */
    var version by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    fun refresh() {
        synchronized(lock) { indexedFolderStamp = Long.MIN_VALUE }
        version++
    }

    /** The decoded image for the game, or null when there is none. Call off the main thread. */
    fun loadScreenshot(
        context: Context,
        game: Game,
    ): Bitmap? {
        val file = findScreenshotFile(context, game)
        if (file == null) {
            lastKeys.remove(gameKey(game))
            return null
        }
        val cacheKey = "${file.absolutePath}:${file.lastModified()}"
        lastKeys[gameKey(game)] = cacheKey
        cache.get(cacheKey)?.let { return it }
        return runCatching { decode(file) }
            .onFailure { Timber.e(it, "Cannot read screenshot ${file.name}") }
            .getOrNull()
            ?.also { cache.put(cacheKey, it) }
    }

    private fun findScreenshotFile(
        context: Context,
        game: Game,
    ): File? {
        val folder = getScreenshotsFolder(context) ?: return null
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
