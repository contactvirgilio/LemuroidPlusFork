package com.swordfish.lemuroid.lib.saves

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class StatesPreviewManager(private val directoriesManager: DirectoriesManager) {
    suspend fun getPreviewForSlot(
        game: Game,
        coreID: CoreID,
        index: Int,
        size: Int,
        cropToSquare: Boolean = true,
    ): Bitmap? =
        withContext(Dispatchers.IO) {
            val screenshotName = getSlotScreenshotName(game, index)
            val file = getPreviewFile(screenshotName, coreID.coreName)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
            if (cropToSquare) {
                ThumbnailUtils.extractThumbnail(bitmap, size, size)
            } else {
                // Keep the whole game screen; only shrink it if it is larger than needed.
                val scale = size.toFloat() / maxOf(bitmap.width, bitmap.height)
                if (scale >= 1f) {
                    bitmap
                } else {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt().coerceAtLeast(1),
                        (bitmap.height * scale).toInt().coerceAtLeast(1),
                        true,
                    )
                }
            }
        }

    suspend fun setPreviewForSlot(
        game: Game,
        bitmap: Bitmap,
        coreID: CoreID,
        index: Int,
    ) = withContext(Dispatchers.IO) {
        val screenshotName = getSlotScreenshotName(game, index)
        val file = getPreviewFile(screenshotName, coreID.coreName)
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
    }

    private fun getPreviewFile(
        fileName: String,
        coreName: String,
    ): File {
        val statesDirectories = File(directoriesManager.getStatesPreviewDirectory(), coreName)
        statesDirectories.mkdirs()
        return File(statesDirectories, fileName)
    }

    private fun getSlotScreenshotName(
        game: Game,
        index: Int,
    ) = "${game.fileName}.slot${index + 1}.jpg"

    companion object {
        // Large enough to stay sharp in the 2 x 2 save state grid.
        val PREVIEW_SIZE_DP = 240f
    }
}
