package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.alorma.compose.settings.storage.memory.rememberMemoryIntSettingState
import com.alorma.compose.settings.storage.base.SettingValueState
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.GameMenuContract
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import kotlin.reflect.KFunction1

@Composable
fun GameMenuHomeScreen(
    navController: NavController,
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (gameMenuRequest.coreConfig.statesSupported) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_save)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_save),
                        contentDescription = stringResource(id = R.string.game_menu_save),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.SAVE) },
            )

            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_load)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_load),
                        contentDescription = stringResource(id = R.string.game_menu_load),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.LOAD) },
            )
        }

        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.game_menu_quit)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_quit),
                    contentDescription = stringResource(id = R.string.game_menu_quit),
                )
            },
            onClick = {
                onResult { putExtra(GameMenuContract.RESULT_QUIT, true) }
            },
        )

        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.game_menu_restart)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_restart),
                    contentDescription = stringResource(id = R.string.game_menu_restart),
                )
            },
            onClick = {
                onResult { putExtra(GameMenuContract.RESULT_RESET, true) }
            },
        )

        if (gameMenuRequest.numDisks > 1) {
            LemuroidSettingsList(
                title = { Text(text = stringResource(id = R.string.game_menu_change_disk_button)) },
                items = (1..gameMenuRequest.numDisks).map { stringResource(R.string.game_menu_change_disk_disk, it) },
                useSelectedValueAsSubtitle = false,
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_disk),
                        contentDescription = stringResource(id = R.string.game_menu_change_disk_button),
                    )
                },
                state = rememberMemoryIntSettingState(gameMenuRequest.currentDisk),
                onItemSelected = { index, _ ->
                    onResult { putExtra(GameMenuContract.RESULT_CHANGE_DISK, index) }
                },
            )
        }

        // Touch controls are hidden while a gamepad (e.g. the device's own buttons) is in use, so
        // there would be nothing to edit.
        if (gameMenuRequest.touchControlsVisible) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_edit_touch_controls)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_controls),
                        contentDescription = stringResource(id = R.string.game_menu_edit_touch_controls),
                    )
                },
                onClick = {
                    onResult { putExtra(GameMenuContract.RESULT_EDIT_TOUCH_CONTROLS, true) }
                },
            )
        }

        if (gameMenuRequest.colorCorrection >= 0) {
            LemuroidSettingsSwitch(
                state = rememberBooleanSettingState(gameMenuRequest.colorCorrection == 1),
                title = { Text(text = stringResource(id = R.string.game_menu_color_correction)) },
                icon = {
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = stringResource(id = R.string.game_menu_color_correction),
                    )
                },
                onCheckedChange = { enabled ->
                    onResult { putExtra(GameMenuContract.RESULT_COLOR_CORRECTION, enabled) }
                },
            )
        }

        LemuroidSettingsSwitch(
            state = rememberBooleanSettingState(gameMenuRequest.immersiveMode),
            title = { Text(text = stringResource(id = R.string.game_menu_immersive_mode)) },
            subtitle = { Text(text = stringResource(id = R.string.game_menu_immersive_mode_hint)) },
            icon = {
                Icon(
                    Icons.Outlined.BlurOn,
                    contentDescription = stringResource(id = R.string.game_menu_immersive_mode),
                )
            },
            onCheckedChange = { enabled ->
                onResult { putExtra(GameMenuContract.RESULT_IMMERSIVE_MODE, enabled) }
            },
        )

        if (gameMenuRequest.advancedCoreOptions.isNotEmpty() || gameMenuRequest.coreOptions.isNotEmpty()) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_settings)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_settings),
                        contentDescription = stringResource(id = R.string.game_menu_settings),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.OPTIONS) },
            )
        }
    }
}

/** A switch state that lives only while the menu is open; the choice is sent back as a result. */
private class MenuSwitchState(private val initial: Boolean) : SettingValueState<Boolean> {
    override var value: Boolean by mutableStateOf(initial)

    override fun reset() {
        value = initial
    }
}

@Composable
private fun rememberBooleanSettingState(initial: Boolean): SettingValueState<Boolean> = remember { MenuSwitchState(initial) }
