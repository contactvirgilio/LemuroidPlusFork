package com.swordfish.lemuroid.app.mobile.feature.settings.general

import com.swordfish.lemuroid.app.mobile.feature.games.HomeTileSize
import com.swordfish.lemuroid.app.mobile.feature.games.HomeTileSizeStore
import com.swordfish.lemuroid.app.mobile.feature.games.GamesViewMode
import com.swordfish.lemuroid.app.mobile.feature.games.GamesViewModeStore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import android.app.Activity
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.apps.AppsTabSettings
import com.swordfish.lemuroid.app.mobile.feature.main.MainRoute
import com.swordfish.lemuroid.app.mobile.feature.main.VERTICAL_BAR_KEY
import com.swordfish.lemuroid.app.mobile.feature.main.navigateToRoute
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.MediaViewSetting
import com.swordfish.lemuroid.app.shared.artwork.ArtworkManager
import com.swordfish.lemuroid.app.shared.game.GameShortcutsSetting
import com.swordfish.lemuroid.app.shared.game.GameShellSkin
import com.swordfish.lemuroid.app.shared.game.PadSkinSetting
import com.swordfish.lemuroid.app.shared.launcher.HomeLauncher
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.shared.sounds.NavigationSounds
import com.swordfish.lemuroid.app.shared.tiles.SystemTileColors
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.app.utils.android.settings.booleanPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState
import com.swordfish.lemuroid.app.utils.android.stringListResource
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    navController: NavController,
    retrogradeDb: RetrogradeDatabase,
) {
    val state =
        viewModel.uiState
            .collectAsState(SettingsViewModel.State())
            .value

    val scanInProgress =
        viewModel.directoryScanInProgress
            .collectAsState(false)
            .value

    val indexingInProgress =
        viewModel.indexingInProgress
            .collectAsState(false)
            .value

    LemuroidSettingsPage(modifier = modifier) {
        RomsSettings(
            state = state,
            onChangeFolder = { viewModel.changeLocalStorageFolder() },
            onChangeSecondFolder = { viewModel.changeSecondStorageFolder() },
            onRemoveSecondFolder = { viewModel.removeSecondStorageFolder() },
            indexingInProgress = indexingInProgress,
            scanInProgress = scanInProgress,
        )
        InGameLookSettings()
        LibraryLookSettings()
        InputSettings(navController = navController)
        HomeScreenSettings(navController = navController)
        BackupSettings(
            retrogradeDb,
            title = { GroupTitle(R.string.settings_group_system) },
            accent = SettingsGroupColors.SYSTEM,
            icon = Icons.Outlined.Build,
        ) {
            MiscRows(indexingInProgress = indexingInProgress, navController = navController)
        }
    }
}

/** Lemuroid Plus: use the app as the device's home screen (launcher). */
@Composable
private fun HomeScreenSettings(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isHome by remember { mutableStateOf(HomeLauncher.isDefaultHome(context)) }

    // Android shows its own screen to pick the home app; refresh when coming back from it.
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) isHome = HomeLauncher.isDefaultHome(context)
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LemuroidCardSettingsGroup(
        title = { GroupTitle(R.string.settings_category_home_screen) },
        accent = SettingsGroupColors.HOME,
        icon = Icons.Outlined.Home,
    ) {
        AppIconPicker()
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_use_as_home_screen)) },
            subtitle = {
                Text(
                    text =
                        stringResource(
                            id =
                                if (isHome) {
                                    R.string.settings_description_home_screen_on
                                } else {
                                    R.string.settings_description_home_screen_off
                                },
                        ),
                )
            },
            onClick = { (context as? Activity)?.let { HomeLauncher.openHomeScreenChooser(it) } },
        )
        // The Apps tab settings unlock only once Lemuroid Plus is the home screen.
        if (isHome) {
            val appsTab = booleanPreferenceState(AppsTabSettings.SHOW_KEY, AppsTabSettings.isSwitchOn(context))
            LemuroidSettingsSwitch(
                state = appsTab,
                title = { Text(text = stringResource(id = R.string.settings_title_apps_tab)) },
                subtitle = { Text(text = stringResource(id = R.string.settings_description_apps_tab)) },
            )
            if (appsTab.value) {
                LemuroidSettingsMenuLink(
                    title = { Text(text = stringResource(id = R.string.settings_title_hidden_apps)) },
                    subtitle = { Text(text = stringResource(id = R.string.settings_description_hidden_apps)) },
                    onClick = { navController.navigateToRoute(MainRoute.SETTINGS_HIDDEN_APPS) },
                )
            }
        }
    }
}

@Composable
private fun MiscRows(
    indexingInProgress: Boolean,
    navController: NavController,
) {
    LemuroidSettingsMenuLink(
        title = { Text(text = stringResource(id = R.string.settings_title_open_cores_selection)) },
        subtitle = {
            Text(text = stringResource(id = R.string.settings_description_open_cores_selection))
        },
        onClick = { navController.navigateToRoute(MainRoute.SETTINGS_CORES_SELECTION) },
    )
    LemuroidSettingsMenuLink(
        title = { Text(text = stringResource(id = R.string.settings_title_display_bios_info)) },
        subtitle = {
            Text(text = stringResource(id = R.string.settings_description_display_bios_info))
        },
        enabled = !indexingInProgress,
        onClick = { navController.navigateToRoute(MainRoute.SETTINGS_BIOS) },
    )
    LemuroidSettingsMenuLink(
        title = { Text(text = stringResource(id = R.string.settings_title_advanced_settings)) },
        subtitle = {
            Text(text = stringResource(id = R.string.settings_description_advanced_settings))
        },
        onClick = { navController.navigateToRoute(MainRoute.SETTINGS_ADVANCED) },
    )
}

@Composable
private fun InputSettings(navController: NavController) {
    LemuroidCardSettingsGroup(
        title = { GroupTitle(R.string.settings_group_controls) },
        accent = SettingsGroupColors.CONTROLS,
        icon = Icons.Outlined.TouchApp,
    ) {
        LemuroidSettingsList(
            state =
                indexPreferenceState(
                    R.string.pref_key_haptic_feedback_mode,
                    "press",
                    stringListResource(R.array.pref_key_haptic_feedback_mode_values),
                ),
            title = {
                Text(text = stringResource(id = R.string.settings_title_enable_touch_feedback))
            },
            items = stringListResource(R.array.pref_key_haptic_feedback_mode_display_names),
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_gamepad_settings)) },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_gamepad_settings))
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_INPUT_DEVICES) },
        )
        // Lemuroid Plus: which button does what, per system (and per game from the game menu).
        // Only with a controller connected: there is nothing to press otherwise.
        if (com.swordfish.lemuroid.app.mobile.feature.buttonmapping.rememberHasGamepad()) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.settings_title_button_mapping)) },
                subtitle = { Text(text = stringResource(id = R.string.settings_description_button_mapping)) },
                onClick = { navController.navigateToRoute(MainRoute.SETTINGS_BUTTON_MAPPING) },
            )
            // Shortcuts switch: on shows "Set shortcuts"; off turns them off in games and hides them.
            val shortcutsOn = booleanPreferenceState(com.swordfish.lemuroid.app.shared.input.ButtonMapping.SHORTCUTS_ENABLED_KEY, true)
            LemuroidSettingsSwitch(
                state = shortcutsOn,
                title = { Text(text = stringResource(id = R.string.settings_title_shortcuts)) },
                subtitle = { Text(text = stringResource(id = R.string.settings_description_shortcuts)) },
            )
            if (shortcutsOn.value) {
                LemuroidSettingsMenuLink(
                    title = { Text(text = stringResource(id = R.string.settings_title_set_shortcuts)) },
                    subtitle = { Text(text = shortcutsSummary(LocalContext.current)) },
                    onClick = { navController.navigateToRoute(MainRoute.SETTINGS_SHORTCUTS) },
                )
            }
        }
    }
}

@Composable
private fun InGameLookSettings() {
    LemuroidCardSettingsGroup(
        title = { GroupTitle(R.string.settings_group_in_game_look) },
        accent = SettingsGroupColors.IN_GAME,
        icon = Icons.Outlined.SportsEsports,
    ) {
        val nestedShortcuts = booleanPreferenceState(GameShortcutsSetting.KEY, GameShortcutsSetting.defaultNested(LocalContext.current))
        ShortcutsPicker(nested = nestedShortcuts.value, onPick = { nestedShortcuts.value = it })
        val shellSkins = booleanPreferenceState(GameShellSkin.SETTING_KEY, false)
        LemuroidSettingsSwitch(
            state = shellSkins,
            title = { Text(text = stringResource(id = R.string.settings_title_shell_skins)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_shell_skins)) },
        )
        ShellSkinsPreview(enabled = shellSkins.value)
        val padOutline = booleanPreferenceState(PadSkinSetting.KEY, true)
        PadSkinPicker(outline = padOutline.value, onPick = { padOutline.value = it })
        // Vertical bar: only on 4:3-ish screens like the Retroid Pocket Classic.
        if (GameShortcutsSetting.defaultNested(LocalContext.current)) {
            LemuroidSettingsSwitch(
                state = booleanPreferenceState(VERTICAL_BAR_KEY, false),
                title = { Text(text = stringResource(id = R.string.settings_title_vertical_bar)) },
                subtitle = { Text(text = stringResource(id = R.string.settings_description_vertical_bar)) },
            )
        }
    }
}

@Composable
private fun LibraryLookSettings() {
    LemuroidCardSettingsGroup(
        title = { GroupTitle(R.string.settings_group_library_look) },
        accent = SettingsGroupColors.LIBRARY,
        icon = Icons.Outlined.GridView,
    ) {
        // Game style, layout and colours in one place, with a live preview (2026-11-20).
        val pastel = booleanPreferenceState(SystemTileColors.PASTEL_PREF_KEY, false)
        val context = LocalContext.current
        val cartridges = booleanPreferenceState(MediaViewSetting.KEY, false)
        val layout by GamesViewModeStore.get(context).collectAsState()
        LibraryLookPicker(
            cartridges = cartridges.value,
            pastel = pastel.value,
            layout = layout,
            onCartridges = {
                cartridges.value = it
                MediaViewSetting.set(context, it)
            },
            onPastel = {
                pastel.value = it
                SystemTileColors.pastelEnabled = it
            },
            onLayout = {
                GamesViewModeStore.set(context, it)
                // Home and Favorites only have small and large tiles.
                HomeTileSizeStore.set(context, if (it == GamesViewMode.GRID_LARGE) HomeTileSize.LARGE else HomeTileSize.SMALL)
            },
        )
        // HD mode is hidden (owner, 2026-11-19): the in-game Filter shortcut covers the look.
        LemuroidSettingsSwitch(
            state = booleanPreferenceState(ArtworkManager.SHOW_ARTWORK_KEY, false),
            title = { Text(text = stringResource(id = R.string.settings_title_show_artwork)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_show_artwork)) },
        )
        LemuroidSettingsSwitch(
            state = booleanPreferenceState(NavigationSounds.ENABLED_KEY, true),
            title = { Text(text = stringResource(id = R.string.settings_title_navigation_sounds)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_navigation_sounds)) },
        )
    }
}

/** "Select+Start · Select+R2 · …" for the Set shortcuts row. */
private fun shortcutsSummary(context: android.content.Context): String {
    val map = com.swordfish.lemuroid.app.shared.input.ButtonMapping.effectiveShortcuts(context, null, null)
    return com.swordfish.lemuroid.app.shared.input.ButtonMapping.Shortcut.values().joinToString(" · ") { type ->
        val keys = map[type].orEmpty().sortedBy { if (it == android.view.KeyEvent.KEYCODE_BUTTON_SELECT) 0 else 1 }
        if (keys.isEmpty()) "—" else keys.joinToString("+") { com.swordfish.lemuroid.app.shared.input.ButtonMapping.keyName(it) }
    }
}

/** Colours of the settings cards; they match the strips on the game menu tiles. */
private object SettingsGroupColors {
    val GAMES = Color(0xFFB4B2A9)
    val IN_GAME = Color(0xFF85B7EB)
    val LIBRARY = Color(0xFFED93B1)
    val CONTROLS = Color(0xFF5DCAA5)
    val HOME = Color(0xFFAFA9EC)
    val SYSTEM = Color(0xFFEF9F27)
}

@Composable
private fun GroupTitle(id: Int) {
    Text(text = stringResource(id = id).uppercase())
}

@Composable
private fun RomsSettings(
    state: SettingsViewModel.State,
    onChangeFolder: () -> Unit,
    onChangeSecondFolder: () -> Unit,
    onRemoveSecondFolder: () -> Unit,
    indexingInProgress: Boolean,
    scanInProgress: Boolean,
) {
    val context = LocalContext.current

    val currentDirectory = state.currentDirectory
    val emptyDirectory = stringResource(R.string.none)

    val unavailable = stringResource(R.string.second_directory_unavailable)
    val currentDirectoryName =
        remember(state.currentDirectory) {
            if (currentDirectory.isBlank()) {
                emptyDirectory
            } else {
                runCatching {
                    DocumentFile.fromTreeUri(context, Uri.parse(currentDirectory))?.name
                }.getOrNull() ?: unavailable
            }
        }

    val secondDirectoryName =
        remember(state.secondDirectory) {
            if (state.secondDirectory.isBlank()) {
                emptyDirectory
            } else {
                runCatching {
                    DocumentFile.fromTreeUri(context, Uri.parse(state.secondDirectory))?.name
                }.getOrNull() ?: unavailable
            }
        }

    LemuroidCardSettingsGroup(
        title = { GroupTitle(R.string.settings_group_games) },
        accent = SettingsGroupColors.GAMES,
        icon = Icons.Outlined.Folder,
    ) {
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.directory)) },
            subtitle = { Text(text = currentDirectoryName) },
            onClick = { onChangeFolder() },
            enabled = !indexingInProgress,
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.second_directory)) },
            subtitle = { Text(text = secondDirectoryName) },
            onClick = { onChangeSecondFolder() },
            enabled = !indexingInProgress,
        )
        if (state.secondDirectory.isNotBlank()) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.second_directory_remove)) },
                onClick = { onRemoveSecondFolder() },
                enabled = !indexingInProgress,
            )
        }
        if (scanInProgress) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.stop)) },
                onClick = { LibraryIndexScheduler.cancelLibrarySync(context) },
            )
        } else {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.rescan)) },
                onClick = { LibraryIndexScheduler.scheduleLibrarySync(context) },
                enabled = !indexingInProgress,
            )
        }
        LemuroidSettingsSwitch(
            state = booleanPreferenceState(R.string.pref_key_autosave, true),
            title = { Text(text = stringResource(id = R.string.settings_title_enable_autosave)) },
            subtitle = { Text(text = stringResource(id = R.string.settings_description_enable_autosave)) },
        )
    }
}
