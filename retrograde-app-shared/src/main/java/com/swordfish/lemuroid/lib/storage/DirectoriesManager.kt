package com.swordfish.lemuroid.lib.storage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.io.File

class DirectoriesManager(private val appContext: Context) {
    companion object {
        /** Folder created in the root of the device's main storage, e.g. /storage/emulated/0/Lemuroid. */
        const val PUBLIC_FOLDER_NAME = "Lemuroid"

        private val MIGRATED_FOLDERS = listOf("saves", "states", "state-previews")
        private val migrationLock = Any()

        @Volatile
        private var migrationDone = false

        /**
         * True when the app can write to the root of the main storage: "All files access" on
         * Android 11+, or the legacy write permission on older versions.
         */
        fun hasPublicStorageAccess(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun getPublicRootDirectory(): File = File(Environment.getExternalStorageDirectory(), PUBLIC_FOLDER_NAME)
    }

    @Deprecated("Use the external states directory")
    fun getInternalStatesDirectory(): File =
        File(appContext.filesDir, "states").apply {
            mkdirs()
        }

    fun getCoresDirectory(): File =
        File(appContext.filesDir, "cores").apply {
            mkdirs()
        }

    fun getSystemDirectory(): File =
        File(appContext.filesDir, "system").apply {
            mkdirs()
        }

    fun getStatesDirectory(): File =
        File(getUserDataRoot(), "states").apply {
            mkdirs()
        }

    fun getStatesPreviewDirectory(): File =
        File(getUserDataRoot(), "state-previews").apply {
            mkdirs()
        }

    fun getSavesDirectory(): File =
        File(getUserDataRoot(), "saves").apply {
            mkdirs()
        }

    fun getInternalRomsDirectory(): File =
        File(appContext.getExternalFilesDir(null), "roms").apply {
            mkdirs()
        }

    /** Where saves and states lived before: inside Android/data, hard to reach for backups. */
    private fun getAppPrivateRoot(): File = appContext.getExternalFilesDir(null) ?: appContext.filesDir

    /**
     * Saves and states go to /Lemuroid in the main storage when the app is allowed to write there,
     * so they are easy to find and back up. Otherwise they stay in the app's private folder.
     */
    private fun getUserDataRoot(): File {
        if (!hasPublicStorageAccess(appContext)) {
            return getAppPrivateRoot()
        }

        val publicRoot = getPublicRootDirectory()
        if (!publicRoot.exists() && !publicRoot.mkdirs()) {
            Timber.w("Cannot create ${publicRoot.absolutePath}, keeping saves in the app folder")
            return getAppPrivateRoot()
        }

        migrateFromAppPrivateRoot(publicRoot)
        return publicRoot
    }

    /**
     * One-time move of existing saves and states into the public folder. Files already present
     * in the public folder are never overwritten, and an old file is only deleted after it has
     * been copied successfully.
     */
    private fun migrateFromAppPrivateRoot(publicRoot: File) {
        if (migrationDone) return
        synchronized(migrationLock) {
            if (migrationDone) return
            val privateRoot = getAppPrivateRoot()
            MIGRATED_FOLDERS.forEach { name ->
                val source = File(privateRoot, name)
                if (!source.isDirectory) return@forEach
                source.walkBottomUp().forEach { file ->
                    runCatching {
                        if (file.isFile) {
                            val target = File(File(publicRoot, name), file.relativeTo(source).path)
                            if (!target.exists()) {
                                target.parentFile?.mkdirs()
                                file.copyTo(target)
                            }
                            if (target.length() == file.length()) {
                                file.delete()
                            }
                        } else if (file.isDirectory && file.listFiles().isNullOrEmpty()) {
                            file.delete()
                        }
                    }.onFailure { Timber.e(it, "Failed to move ${file.absolutePath}") }
                }
            }
            migrationDone = true
        }
    }
}
