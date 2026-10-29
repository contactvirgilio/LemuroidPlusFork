package com.swordfish.lemuroid.app.shared.storage

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import timber.log.Timber

/**
 * Asks for permission to write to the main storage, so saves and states can live in
 * /Lemuroid where they are easy to back up.
 */
object PublicStorageAccess {
    private const val PREFS_NAME = "public_storage_access"
    private const val PREF_DONT_ASK = "dont_ask_again"
    private const val LEGACY_PERMISSION_REQUEST = 4721

    fun requestIfNeeded(activity: Activity) {
        if (DirectoriesManager.hasPublicStorageAccess(activity)) return

        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_DONT_ASK, false)) return

        AlertDialog.Builder(activity)
            .setTitle(R.string.public_storage_dialog_title)
            .setMessage(
                activity.getString(
                    R.string.public_storage_dialog_message,
                    DirectoriesManager.PUBLIC_FOLDER_NAME,
                ),
            )
            .setPositiveButton(R.string.public_storage_dialog_allow) { _, _ -> openPermissionScreen(activity) }
            .setNegativeButton(R.string.public_storage_dialog_not_now, null)
            .setNeutralButton(R.string.public_storage_dialog_dont_ask) { _, _ ->
                prefs.edit().putBoolean(PREF_DONT_ASK, true).apply()
            }
            .show()
    }

    private fun openPermissionScreen(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appSpecific =
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${activity.packageName}"),
                )
            val generic = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            runCatching { activity.startActivity(appSpecific) }
                .recoverCatching { activity.startActivity(generic) }
                .onFailure { Timber.e(it, "Cannot open all files access settings") }
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                LEGACY_PERMISSION_REQUEST,
            )
        }
    }
}
