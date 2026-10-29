package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.backup.SavesBackupManager
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "Backup" section of the settings: export all saves and states to a zip file, or import one. */
@Composable
fun BackupSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { SavesBackupManager(context.applicationContext) }

    var busy by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            busy = true
            scope.launch {
                runCatching { backupManager.export(uri) }
                    .onSuccess { toast(context.getString(R.string.backup_export_done, it.files)) }
                    .onFailure {
                        Timber.e(it, "Saves export failed")
                        toast(context.getString(R.string.backup_export_failed, it.message ?: ""))
                    }
                busy = false
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            pendingImport = uri
        }

    LemuroidCardSettingsGroup(
        title = { Text(text = stringResource(id = R.string.settings_category_backup)) },
    ) {
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_backup_export)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_backup_export)) },
            enabled = !busy,
            onClick = { exportLauncher.launch(defaultBackupName()) },
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_backup_import)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_backup_import)) },
            enabled = !busy,
            onClick = { importLauncher.launch(BACKUP_MIME_TYPES) },
        )
    }

    val importUri = pendingImport
    if (importUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(text = stringResource(id = R.string.backup_import_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.backup_import_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImport = null
                        busy = true
                        scope.launch {
                            runCatching { backupManager.import(importUri) }
                                .onSuccess { toast(context.getString(R.string.backup_import_done, it.files)) }
                                .onFailure {
                                    Timber.e(it, "Saves import failed")
                                    toast(context.getString(R.string.backup_import_failed, it.message ?: ""))
                                }
                            busy = false
                        }
                    },
                ) {
                    Text(text = stringResource(id = R.string.backup_import_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            },
        )
    }
}

private val BACKUP_MIME_TYPES =
    arrayOf(
        "application/zip",
        "application/x-zip-compressed",
        "application/octet-stream",
    )

private fun defaultBackupName(): String {
    val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
    return "Lemuroid-saves-$date.zip"
}
