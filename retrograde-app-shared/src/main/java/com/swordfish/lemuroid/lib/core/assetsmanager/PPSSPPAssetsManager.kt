package com.swordfish.lemuroid.lib.core.assetsmanager

import android.content.SharedPreferences
import android.net.Uri
import com.swordfish.lemuroid.lib.core.CoreUpdater
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.util.zip.ZipInputStream

class PPSSPPAssetsManager : CoreID.AssetsManager {
    override suspend fun clearAssets(directoriesManager: DirectoriesManager) {
        getAssetsDirectory(directoriesManager).deleteRecursively()
    }

    override suspend fun retrieveAssetsIfNeeded(
        coreUpdaterApi: CoreUpdater.CoreManagerApi,
        directoriesManager: DirectoriesManager,
        sharedPreferences: SharedPreferences,
    ) {
        if (!updatedRequested(directoriesManager, sharedPreferences)) {
            return
        }

        try {
            // Release builds ship the assets inside the APK, so no download is needed.
            val bundled = withContext(Dispatchers.IO) { directoriesManager.openBundledAsset(BUNDLED_ASSETS_NAME) }
            if (bundled != null) {
                withContext(Dispatchers.IO) {
                    ZipInputStream(bundled.buffered()).use { extract(directoriesManager, it, sharedPreferences) }
                }
                return
            }
            val response = coreUpdaterApi.downloadZip(PPSSPP_ASSETS_URL.toString())
            handleSuccess(directoriesManager, response, sharedPreferences)
        } catch (e: Throwable) {
            getAssetsDirectory(directoriesManager).deleteRecursively()
        }
    }

    private suspend fun handleSuccess(
        directoriesManager: DirectoriesManager,
        response: Response<ZipInputStream>,
        sharedPreferences: SharedPreferences,
    ) {
        response.body()?.use { extract(directoriesManager, it, sharedPreferences) }
    }

    private suspend fun extract(
        directoriesManager: DirectoriesManager,
        zipInputStream: ZipInputStream,
        sharedPreferences: SharedPreferences,
    ) {
        val coreAssetsDirectory = getAssetsDirectory(directoriesManager)
        coreAssetsDirectory.deleteRecursively()
        coreAssetsDirectory.mkdirs()

        while (true) {
            val entry = zipInputStream.nextEntry ?: break
            Timber.d("Writing file: ${entry.name}")
            val destFile =
                File(
                    coreAssetsDirectory,
                    entry.name,
                )
            if (entry.isDirectory) {
                destFile.mkdirs()
            } else {
                destFile.parentFile?.mkdirs()
                destFile.outputStream().use { zipInputStream.copyTo(it) }
            }
        }

        sharedPreferences.edit()
            .putString(PPSSPP_ASSETS_VERSION_KEY, PPSSPP_ASSETS_VERSION)
            .commit()
    }

    private suspend fun updatedRequested(
        directoriesManager: DirectoriesManager,
        sharedPreferences: SharedPreferences,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val directoryExists = getAssetsDirectory(directoriesManager).exists()

            val currentVersion = sharedPreferences.getString(PPSSPP_ASSETS_VERSION_KEY, "none")
            val hasCurrentVersion = currentVersion == PPSSPP_ASSETS_VERSION

            !directoryExists || !hasCurrentVersion
        }

    private suspend fun getAssetsDirectory(directoriesManager: DirectoriesManager): File {
        return withContext(Dispatchers.IO) {
            File(directoriesManager.getSystemDirectory(), PPSSPP_ASSETS_FOLDER_NAME)
        }
    }

    companion object {
        const val PPSSPP_ASSETS_VERSION = "1.15"

        val PPSSPP_ASSETS_URL: Uri =
            Uri.parse("https://github.com/Swordfish90/LemuroidCores/")
                .buildUpon()
                .appendEncodedPath("raw/$PPSSPP_ASSETS_VERSION/assets/ppsspp.zip")
                .build()

        private const val BUNDLED_ASSETS_NAME = "ppsspp.zip"

        const val PPSSPP_ASSETS_VERSION_KEY = "ppsspp_assets_version_key"

        const val PPSSPP_ASSETS_FOLDER_NAME = "PPSSPP"
    }
}
