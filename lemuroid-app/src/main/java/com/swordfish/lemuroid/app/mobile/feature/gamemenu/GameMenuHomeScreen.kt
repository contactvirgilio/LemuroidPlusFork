package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.BlurLinear
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.GameMenuContract
import kotlin.reflect.KFunction1

private class MenuEntry(
    val label: String,
    val sublabel: String? = null,
    val active: Boolean = false,
    val icon: @Composable (Color) -> Unit,
    val onClick: () -> Unit,
)

private fun drawableIcon(
    id: Int,
    description: String,
): @Composable (Color) -> Unit =
    { tint ->
        Icon(
            painterResource(id),
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
    }

/** The menu home: one tile per action, in the same style as the Tetris DX / DKC menus. */
@Composable
fun GameMenuHomeScreen(
    navController: NavController,
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
) {
    val entries = mutableListOf<MenuEntry>()

    if (gameMenuRequest.coreConfig.statesSupported) {
        val save = stringResource(id = R.string.game_menu_save)
        entries +=
            MenuEntry(
                label = save,
                icon = drawableIcon(R.drawable.ic_menu_save, save),
                onClick = { navController.navigateToRoute(GameMenuRoute.SAVE) },
            )
        val load = stringResource(id = R.string.game_menu_load)
        entries +=
            MenuEntry(
                label = load,
                icon = drawableIcon(R.drawable.ic_menu_load, load),
                onClick = { navController.navigateToRoute(GameMenuRoute.LOAD) },
            )
    }

    val restart = stringResource(id = R.string.game_menu_restart)
    entries +=
        MenuEntry(
            label = restart,
            icon = drawableIcon(R.drawable.ic_menu_restart, restart),
            onClick = { onResult { putExtra(GameMenuContract.RESULT_RESET, true) } },
        )

    val quit = stringResource(id = R.string.game_menu_quit)
    entries +=
        MenuEntry(
            label = quit,
            icon = drawableIcon(R.drawable.ic_menu_quit, quit),
            onClick = { onResult { putExtra(GameMenuContract.RESULT_QUIT, true) } },
        )

    if (gameMenuRequest.numDisks > 1) {
        // Each tap on the tile goes to the next disk.
        val nextDisk = (gameMenuRequest.currentDisk + 1) % gameMenuRequest.numDisks
        val disk = stringResource(id = R.string.game_menu_change_disk_button)
        entries +=
            MenuEntry(
                label = stringResource(R.string.game_menu_change_disk_disk, gameMenuRequest.currentDisk + 1),
                sublabel = disk,
                icon = drawableIcon(R.drawable.ic_menu_disk, disk),
                onClick = { onResult { putExtra(GameMenuContract.RESULT_CHANGE_DISK, nextDisk) } },
            )
    }

    // Touch controls are hidden while a gamepad (e.g. the device's own buttons) is in use, so
    // there would be nothing to edit.
    if (gameMenuRequest.touchControlsVisible) {
        val edit = stringResource(id = R.string.game_menu_edit_touch_controls)
        entries +=
            MenuEntry(
                label = edit,
                icon = drawableIcon(R.drawable.ic_menu_controls, edit),
                onClick = { onResult { putExtra(GameMenuContract.RESULT_EDIT_TOUCH_CONTROLS, true) } },
            )
    }

    if (gameMenuRequest.colorCorrection >= 0) {
        val colorCorrection = stringResource(id = R.string.game_menu_color_correction)
        val isOn = gameMenuRequest.colorCorrection == 1
        entries +=
            MenuEntry(
                label = colorCorrection,
                sublabel = stringResource(if (isOn) R.string.game_menu_on else R.string.game_menu_off),
                active = isOn,
                icon = { tint ->
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = colorCorrection,
                        tint = tint,
                        modifier = Modifier.size(28.dp),
                    )
                },
                onClick = { onResult { putExtra(GameMenuContract.RESULT_COLOR_CORRECTION, !isOn) } },
            )
    }

    if (gameMenuRequest.motionBlur >= 0) {
        val motionBlur = stringResource(id = R.string.game_menu_motion_blur)
        val isOn = gameMenuRequest.motionBlur == 1
        entries +=
            MenuEntry(
                label = motionBlur,
                sublabel = stringResource(if (isOn) R.string.game_menu_on else R.string.game_menu_off),
                active = isOn,
                icon = { tint ->
                    Icon(
                        Icons.Outlined.BlurLinear,
                        contentDescription = motionBlur,
                        tint = tint,
                        modifier = Modifier.size(28.dp),
                    )
                },
                onClick = { onResult { putExtra(GameMenuContract.RESULT_MOTION_BLUR, !isOn) } },
            )
    }

    // Every core option is a tile right here (no separate Settings page): a tap moves to the next
    // value and is saved at once; the game picks it up when the menu closes.
    entries += coreOptionEntries(gameMenuRequest)

    val firstTile = remember { FocusRequester() }
    val hasGamepad = LocalMenuHasGamepad.current
    LaunchedEffect(hasGamepad) {
        // With a gamepad the first tile starts selected; with touch nothing is outlined.
        if (hasGamepad) runCatching { firstTile.requestFocus() }
    }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxWidth(),
        columns = GridCells.Adaptive(minSize = 128.dp),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        entries.forEachIndexed { index, entry ->
            item(key = entry.label) {
                MenuTile(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .then(if (index == 0) Modifier.focusRequester(firstTile) else Modifier),
                    onClick = entry.onClick,
                ) { focused ->
                    MenuTileLabel(
                        icon = entry.icon,
                        label = entry.label,
                        focused = focused,
                        sublabel = entry.sublabel,
                        active = entry.active,
                    )
                }
            }
        }
    }
}

@Composable
private fun coreOptionEntries(gameMenuRequest: GameMenuActivity.GameMenuRequest): List<MenuEntry> {
    val context = LocalContext.current
    val prefs = remember { com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper.getSharedPreferences(context) }
    val systemId = gameMenuRequest.game.systemId
    val on = stringResource(R.string.game_menu_on)
    val off = stringResource(R.string.game_menu_off)
    val result = mutableListOf<MenuEntry>()

    // DS: the core's screen layout is driven by our own Screens tile (single screen, R2 flips).
    val dsLayoutKey =
        if (systemId == com.swordfish.lemuroid.lib.library.SystemID.NDS.dbname) {
            com.swordfish.lemuroid.app.shared.game.DsSingleScreen.layoutKey(gameMenuRequest.coreConfig.coreID)
        } else {
            null
        }
    if (dsLayoutKey != null) {
        val ds = com.swordfish.lemuroid.app.shared.game.DsSingleScreen
        var layout by remember { mutableStateOf(ds.chosenLayout(context, gameMenuRequest.coreConfig)) }
        val label = stringResource(R.string.game_menu_ds_screens)
        result +=
            MenuEntry(
                label = label,
                sublabel =
                    stringResource(
                        when (layout) {
                            com.swordfish.lemuroid.app.shared.game.DsSingleScreen.Layout.BOTH -> R.string.game_menu_ds_screens_both
                            com.swordfish.lemuroid.app.shared.game.DsSingleScreen.Layout.SINGLE -> R.string.game_menu_ds_screens_single
                            com.swordfish.lemuroid.app.shared.game.DsSingleScreen.Layout.SIDE_BY_SIDE -> R.string.game_menu_ds_screens_side
                        },
                    ),
                active = layout != com.swordfish.lemuroid.app.shared.game.DsSingleScreen.Layout.BOTH,
                icon = { tint ->
                    Icon(Icons.Outlined.Splitscreen, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
                },
                onClick = {
                    val all = com.swordfish.lemuroid.app.shared.game.DsSingleScreen.Layout.values()
                    layout = all[(layout.ordinal + 1) % all.size]
                    ds.choose(context, gameMenuRequest.coreConfig, layout)
                },
            )
    }

    for (option in gameMenuRequest.coreOptions + gameMenuRequest.advancedCoreOptions) {
        if (option.getKey() == dsLayoutKey) continue
        // Only the essentials the owner picked get a tile (with a plain name); the rest are gone
        // from the menu and keep their defaults.
        val plainName = MENU_CORE_OPTIONS[option.getKey()] ?: continue
        val values = option.getEntriesValues()
        if (values.isEmpty()) continue
        val key = com.swordfish.lemuroid.lib.core.CoreVariablesManager.computeSharedPreferenceKey(option.getKey(), systemId)
        val isSwitch = values.toSet() == com.swordfish.lemuroid.app.shared.coreoptions.CoreOptionsPreferenceHelper.BOOLEAN_SET
        val names = option.getEntries(context)
        var current by remember(key) {
            mutableStateOf(option.getCurrentValue().takeIf { it in values } ?: values.first())
        }
        val label = stringResource(plainName)
        result +=
            MenuEntry(
                label = label,
                sublabel = if (isSwitch) (if (current == "enabled") on else off) else names.getOrNull(values.indexOf(current)),
                active = if (isSwitch) current == "enabled" else current != values.first(),
                icon = { tint ->
                    Icon(Icons.Outlined.Tune, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
                },
                onClick = {
                    val next = values[(values.indexOf(current) + 1) % values.size]
                    current = next
                    // Same storage as the old Settings page: switches as booleans, lists as values.
                    if (isSwitch) {
                        prefs.edit().putBoolean(key, next == "enabled").apply()
                    } else {
                        prefs.edit().putString(key, next).apply()
                    }
                },
            )
    }

    // Controller type for the first controller, when the system has more than one.
    val configs = gameMenuRequest.coreConfig.controllerConfigs[0]
    if (configs != null && configs.size >= 2) {
        val key =
            com.swordfish.lemuroid.app.shared.settings.ControllerConfigsManager.getSharedPreferencesId(
                systemId,
                gameMenuRequest.coreConfig.coreID,
                0,
            )
        val names = configs.map { stringResource(it.displayName) }
        var current by remember(key) {
            mutableStateOf(prefs.getString(key, null)?.takeIf { n -> configs.any { it.name == n } } ?: configs.first().name)
        }
        val label = context.getString(R.string.core_settings_controller, "1")
        result +=
            MenuEntry(
                label = label,
                sublabel = names.getOrNull(configs.indexOfFirst { it.name == current }),
                active = current != configs.first().name,
                icon = { tint ->
                    Icon(Icons.Outlined.SportsEsports, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
                },
                onClick = {
                    val i = configs.indexOfFirst { it.name == current }
                    val next = configs[(i + 1) % configs.size].name
                    current = next
                    prefs.edit().putString(key, next).apply()
                },
            )
    }
    return result
}

/** Core options shown as game menu tiles (owner's pick, 2026-11-19) and their plain names. */
private val MENU_CORE_OPTIONS =
    mapOf(
        // Crop edges (overscan)
        "fceumm_overscan_h" to R.string.menu_option_crop_sides,
        "fceumm_overscan_v" to R.string.menu_option_crop_top_bottom,
        "stella_crop_hoverscan" to R.string.menu_option_crop_edges,
        "genesis_plus_gx_overscan" to R.string.menu_option_show_edges,
        // No sprite flicker
        "fceumm_nospritelimit" to R.string.menu_option_no_flicker,
        "genesis_plus_gx_no_sprite_limit" to R.string.menu_option_no_flicker,
        // Game Boy colours
        "gambatte_gb_colorization" to R.string.menu_option_colorize,
        "gambatte_gb_internal_palette" to R.string.menu_option_palette,
        // Screen layout (DS has its own Screens tile)
        "citra_layout_option" to R.string.game_menu_ds_screens,
        // Rotate screen
        "handy_rot" to R.string.menu_option_rotate,
        "wswan_rotate_display" to R.string.menu_option_rotate,
        // Frameskip
        "pcsx_rearmed_frameskip" to R.string.menu_option_frameskip,
        "ppsspp_frameskip" to R.string.menu_option_frameskip,
        "ppsspp_auto_frameskip" to R.string.menu_option_auto_frameskip,
        "fbneo-frameskip" to R.string.menu_option_frameskip,
        "desmume_frameskip" to R.string.menu_option_frameskip,
        "mgba_frameskip" to R.string.menu_option_frameskip,
    )
