package com.swordfish.touchinput.radial.layouts

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.swordfish.touchinput.radial.controls.LemuroidControlFaceButtons
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonMenu
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonStart
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import gg.padkit.PadKitScope
import gg.padkit.ids.Id
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

/** The buttons the running arcade game uses (Android key codes), set by the game screen. */
object ArcadeTouchButtons {
    var keys: List<Int> by mutableStateOf(emptyList())
}

/** Arcade pad with only the buttons the game uses (1 to 4), no names (owner, 2026-11-28). */
@Composable
fun PadKitScope.ArcadeCustomRight(
    modifier: Modifier = Modifier,
    settings: TouchControllerSettingsManager.Settings,
) {
    val keys =
        ArcadeTouchButtons.keys.ifEmpty {
            listOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_X)
        }
    val ids = keys.map { Id.Key(it) }
    BaseLayoutRight(
        settings = settings,
        modifier = modifier,
        primaryDial = {
            LemuroidControlFaceButtons(
                ids = ids.toPersistentList(),
                idsForegrounds =
                    ids.associateWith<Id.Key, @Composable (State<Boolean>) -> Unit> { { LemuroidButtonForeground(pressed = it) } }
                        .toPersistentMap(),
            )
        },
        secondaryDials = {
            SecondaryButtonStart()
            SecondaryButtonMenu(settings)
        },
    )
}
