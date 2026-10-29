package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.backup.SaveFileImporter
import com.swordfish.lemuroid.app.shared.backup.SavesBackupManager
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Backup" section of the settings: export all saves and states to a zip file, import one,
 * or import single in-game save files (.sav/.srm) from other emulators.
 */
@Composable
fun BackupSettings(retrogradeDb: RetrogradeDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { SavesBackupManager(context.applicationContext) }
    val saveFileImporter = remember { SaveFileImporter(context.applicationContext, retrogradeDb) }

    var busy by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    // Save files that could not be matched to a game by name, waiting for the user to pick one.
    var unmatchedFiles by remember { mutableStateOf<List<SaveFileImporter.PickedFile>>(emptyList()) }
    var allGames by remember { mutableStateOf<List<Game>>(emptyList()) }
    var importedCount by remember { mutableStateOf(0) }

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

    fun finishSaveFileImport() {
        if (importedCount > 0) {
            toast(
                context.resources.getQuantityString(R.plurals.backup_save_files_imported, importedCount, importedCount),
            )
        }
        importedCount = 0
        busy = false
    }

    val saveFilesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            busy = true
            scope.launch {
                runCatching {
                    val plan = saveFileImporter.plan(uris)
                    plan.matched.forEach { (file, game) ->
                        runCatching { saveFileImporter.import(file, game) }
                            .onSuccess { importedCount++ }
                            .onFailure { Timber.e(it, "Failed to import ${file.name}") }
                    }
                    allGames = plan.games
                    unmatchedFiles = plan.unmatched
                }.onFailure {
                    Timber.e(it, "Save files import failed")
                    toast(context.getString(R.string.backup_import_failed, it.message ?: ""))
                }
                if (unmatchedFiles.isEmpty()) finishSaveFileImport()
            }
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
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_backup_import_sav)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_backup_import_sav)) },
            enabled = !busy,
            onClick = { saveFilesLauncher.launch(arrayOf("*/*")) },
        )
    }

    val currentFile = unmatchedFiles.firstOrNull()
    if (currentFile != null) {
        GamePickerDialog(
            fileName = currentFile.name,
            games = allGames,
            onGameSelected = { game ->
                unmatchedFiles = unmatchedFiles.drop(1)
                scope.launch {
                    runCatching { saveFileImporter.import(currentFile, game) }
                        .onSuccess { importedCount++ }
                        .onFailure {
                            Timber.e(it, "Failed to import ${currentFile.name}")
                            toast(context.getString(R.string.backup_import_failed, it.message ?: ""))
                        }
                    if (unmatchedFiles.isEmpty()) finishSaveFileImport()
                }
            },
            onSkip = {
                unmatchedFiles = unmatchedFiles.drop(1)
                if (unmatchedFiles.isEmpty()) finishSaveFileImport()
            },
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

/** Lets the user choose which game a save file belongs to, with a search box. */
@Composable
private fun GamePickerDialog(
    fileName: String,
    games: List<Game>,
    onGameSelected: (Game) -> Unit,
    onSkip: () -> Unit,
) {
    var query by remember(fileName) { mutableStateOf("") }
    val filtered =
        remember(query, games) {
            games.filter { query.isBlank() || it.displayName().contains(query.trim(), ignoreCase = true) }
        }

    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(text = stringResource(id = R.string.backup_pick_game_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(id = R.string.backup_pick_game_message, fileName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(text = stringResource(id = R.string.backup_pick_game_search)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(filtered, key = { it.id }) { game ->
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onGameSelected(game) }
                                    .padding(vertical = 10.dp),
                        ) {
                            Text(
                                text = game.displayName(),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(id = GameSystem.findById(game.systemId).shortTitleResId),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(text = stringResource(id = R.string.backup_pick_game_skip))
            }
        },
    )
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
