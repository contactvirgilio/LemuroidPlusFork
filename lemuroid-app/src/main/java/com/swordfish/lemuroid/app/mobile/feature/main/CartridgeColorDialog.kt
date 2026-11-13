package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgePalette
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeStyle
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeStyles
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.GameMediaImage
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Pick a cartridge's color: matte or sparkly, from the predefined colors. */
@Composable
fun CartridgeColorDialog(
    game: Game,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    var choice by remember { mutableStateOf(CartridgeStyles.get(game)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.game_context_menu_cartridge_color)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GameMediaImage(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                    game = game,
                    style = choice,
                )
                SwatchSection(
                    title = stringResource(id = R.string.cartridge_color_matte),
                    sparkly = false,
                    selected = choice,
                    onPick = { choice = it },
                )
                SwatchSection(
                    title = stringResource(id = R.string.cartridge_color_sparkly),
                    sparkly = true,
                    selected = choice,
                    onPick = { choice = it },
                )
                TextButton(
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = { choice = null },
                ) {
                    Text(text = stringResource(id = R.string.cartridge_color_original))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    CartridgeStyles.set(context, game, choice)
                    onSaved()
                },
            ) {
                Text(text = stringResource(id = R.string.cartridge_color_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cartridge_color_cancel))
            }
        },
    )
}

@Composable
private fun SwatchSection(
    title: String,
    sparkly: Boolean,
    selected: CartridgeStyle?,
    onPick: (CartridgeStyle) -> Unit,
) {
    Text(
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
        text = title,
        style = MaterialTheme.typography.titleSmall,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CartridgePalette.colors.forEachIndexed { index, color ->
            val style = CartridgeStyle(index, sparkly)
            val isSelected = selected == style
            val shape = RoundedCornerShape(8.dp)
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(swatchBrush(color, sparkly))
                        .border(
                            border =
                                BorderStroke(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            shape = shape,
                        )
                        .clickable { onPick(style) },
            )
        }
    }
}

private fun swatchBrush(
    color: Color,
    sparkly: Boolean,
): Brush =
    if (sparkly) {
        Brush.linearGradient(listOf(lerp(color, Color.Black, 0.45f), color, lerp(color, Color.White, 0.5f)))
    } else {
        Brush.linearGradient(listOf(color, color))
    }
