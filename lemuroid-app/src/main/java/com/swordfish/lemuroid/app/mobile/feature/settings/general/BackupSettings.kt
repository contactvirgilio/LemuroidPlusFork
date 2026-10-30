package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
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
 * "Backup" section of the settings: export all saves and states to a zip file, and one "Import saves"
 * entry that accepts backup zips as well as single in-game save files (.sav/.srm) from other emulators.
 */
@Composable
fun BackupSettings(retrogradeDb: RetrogradeDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { SavesBackupManager(context.applicationContext) }
    val saveFileImporter = remember { SaveFileImporter(context.applicationContext, retrogradeDb) }

    var busy by remember { mutableStateOf(false) }

    // Export progress as (files done, total files), or null when no export is running.
    var exportProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Backup zips waiting for confirmation, and the save files picked together with them.
    var pendingZips by remember { mutableStateOf<List<SaveFileImporter.PickedFile>>(emptyList()) }
    var pendingSaveFiles by remember { mutableStateOf<List<SaveFileImporter.PickedFile>>(emptyList()) }

    // Save files that could not be matched to a game by name, waiting for the user to pick one.
    var unmatchedFiles by remember { mutableStateOf<List<SaveFileImporter.PickedFile>>(emptyList()) }
    var allGames by remember { mutableStateOf<List<Game>>(emptyList()) }
    var importedFiles by remember { mutableStateOf(0) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    fun finishImport() {
        if (importedFiles > 0) {
            toast(context.getString(R.string.backup_import_done, importedFiles))
        }
        importedFiles = 0
        busy = false
    }

    /** Restores the confirmed zips, then imports the save files (asking for unmatched ones). */
    fun runImport(
        zips: List<SaveFileImporter.PickedFile>,
        saveFiles: List<SaveFileImporter.PickedFile>,
    ) {
        busy = true
        scope.launch {
            zips.forEach { zip ->
                runCatching { backupManager.import(zip.uri) }
                    .onSuccess { importedFiles += it.files }
                    .onFailure {
                        Timber.e(it, "Backup import failed for ${zip.name}")
                        toast(context.getString(R.string.backup_import_failed, "${zip.name}: ${it.message ?: ""}"))
                    }
            }

            if (saveFiles.isNotEmpty()) {
                runCatching {
                    val plan = saveFileImporter.plan(saveFiles)
                    plan.matched.forEach { (file, game) ->
                        runCatching { saveFileImporter.import(file, game) }
                            .onSuccess { importedFiles++ }
                            .onFailure { Timber.e(it, "Failed to import ${file.name}") }
                    }
                    allGames = plan.games
                    unmatchedFiles = plan.unmatched
                }.onFailure {
                    Timber.e(it, "Save files import failed")
                    toast(context.getString(R.string.backup_import_failed, it.message ?: ""))
                }
            }

            if (unmatchedFiles.isEmpty()) finishImport()
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            busy = true
            exportProgress = 0 to 0
            scope.launch {
                runCatching {
                    backupManager.export(uri) { done, total ->
                        scope.launch { if (exportProgress != null) exportProgress = done to total }
                    }
                }
                    .onSuccess { toast(context.getString(R.string.backup_export_done, it.files)) }
                    .onFailure {
                        Timber.e(it, "Saves export failed")
                        toast(context.getString(R.string.backup_export_failed, it.message ?: ""))
                    }
                exportProgress = null
                busy = false
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            scope.launch {
                val files = saveFileImporter.describe(uris)
                val (zips, saveFiles) = files.partition { it.isBackupZip }
                if (zips.isEmpty()) {
                    runImport(emptyList(), saveFiles)
                } else {
                    // Restoring a backup can overwrite many saves, so ask first.
                    pendingSaveFiles = saveFiles
                    pendingZips = zips
                }
            }
        }

    LemuroidCardSettingsGroup(
        title = { Text(text = stringResource(id = R.string.settings_category_backup)) },
    ) {
        val progress = exportProgress
        if (progress != null) {
            ExportProgressRow(done = progress.first, total = progress.second)
        } else {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.settings_title_backup_export)) },
                subtitle = { Text(text = stringResource(id = R.string.settings_description_backup_export)) },
                enabled = !busy,
                onClick = { exportLauncher.launch(defaultBackupName()) },
            )
        }
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_backup_import)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_backup_import)) },
            enabled = !busy,
            onClick = { importLauncher.launch(arrayOf("*/*")) },
        )
    }

    if (pendingZips.isNotEmpty()) {
        val zips = pendingZips
        val saveFiles = pendingSaveFiles
        AlertDialog(
            onDismissRequest = {
                pendingZips = emptyList()
                pendingSaveFiles = emptyList()
            },
            title = { Text(text = stringResource(id = R.string.backup_import_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.backup_import_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingZips = emptyList()
                        pendingSaveFiles = emptyList()
                        runImport(zips, saveFiles)
                    },
                ) {
                    Text(text = stringResource(id = R.string.backup_import_confirm_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingZips = emptyList()
                        pendingSaveFiles = emptyList()
                    },
                ) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            },
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
                        .onSuccess { importedFiles++ }
                        .onFailure {
                            Timber.e(it, "Failed to import ${currentFile.name}")
                            toast(context.getString(R.string.backup_import_failed, it.message ?: ""))
                        }
                    if (unmatchedFiles.isEmpty()) finishImport()
                }
            },
            onSkip = {
                unmatchedFiles = unmatchedFiles.drop(1)
                if (unmatchedFiles.isEmpty()) finishImport()
            },
        )
    }
}

/** Shown in place of "Export saves" while an export runs: a live bar with a file count. */
@Composable
private fun ExportProgressRow(
    done: Int,
    total: Int,
) {
    val fraction = if (total > 0) done.toFloat() / total else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            text = stringResource(id = R.string.backup_exporting),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (total > 0) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(
                text = if (total > 0) stringResource(id = R.string.backup_export_progress, done, total) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (total > 0) "${(fraction * 100).toInt()}%" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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

private fun defaultBackupName(): String {
    val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
    return "Lemuroid-saves-$date.zip"
}
