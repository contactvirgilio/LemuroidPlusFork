package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import gg.padkit.ui.DefaultCrossForeground

@Composable
fun LemuroidCrossForeground(
    allowDiagonals: Boolean,
    directionState: State<Offset>,
) {
    val theme = LocalLemuroidPadTheme.current
    val pressedArms = remember { mutableStateMapOf<Int, Boolean>() }
    // Outline skin: one plus-shaped outline for the whole d-pad, with the pressed arms filled to match it.
    val outlineModifier =
        if (theme.outline) {
            Modifier.drawBehind {
                drawCrossOutline(
                    color = theme.outlineColor,
                    strokeWidth = theme.outlineStroke,
                    pressedFill = theme.outlinePressedFill,
                    pressedArms = pressedArms.filterValues { it }.keys,
                )
            }
        } else {
            Modifier
        }
    val report: (Int, Boolean) -> Unit = { arm, down -> pressedArms[arm] = down }
    Box(modifier = Modifier.fillMaxSize().then(outlineModifier)) {
        DefaultCrossForeground(
            modifier = Modifier.fillMaxSize(),
            directionState = directionState,
            allowDiagonals = allowDiagonals,
            leftDial = {
                LemuroidCrossButton(it, Icons.Default.KeyboardArrowLeft) { d -> report(0, d) }
            },
            rightDial = {
                LemuroidCrossButton(it, Icons.Default.KeyboardArrowRight) { d -> report(1, d) }
            },
            topDial = {
                LemuroidCrossButton(it, Icons.Default.KeyboardArrowUp) { d -> report(2, d) }
            },
            bottomDial = {
                LemuroidCrossButton(it, Icons.Default.KeyboardArrowDown) { d -> report(3, d) }
            },
            foregroundComposite = {
                LemuroidCompositeForeground(it)
            },
        )
    }
}

@Composable
private fun LemuroidCrossButton(
    pressedState: State<Boolean>,
    imageVector: ImageVector,
    onPressed: (Boolean) -> Unit,
) {
    val down = pressedState.value
    SideEffect { onPressed(down) }
    LemuroidButtonForeground(
        pressed = pressedState,
        rectangle = true,
        label = { },
        icon = {
            Icon(
                modifier = Modifier.size(maxWidth * 0.5f, maxHeight * 0.5f),
                imageVector = imageVector,
                contentDescription = "",
                tint = LocalLemuroidPadTheme.current.icons(pressedState.value),
            )
        },
    )
}
