package com.swordfish.lemuroid.app.mobile.feature.gamemenu.states

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.LocalMenuHasGamepad
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.MenuColors
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.MenuTile

/** Save/load state slots as tiles with the whole game picture, like the DKC states card. */
@Composable
fun GameMenuStatesScreen(
    viewModel: GameMenuStatesViewModel,
    onStateClicked: (Int) -> Unit,
) {
    val state = viewModel.uiStates.collectAsState(initial = GameMenuStatesViewModel.State())

    val firstTile = remember { FocusRequester() }
    val hasGamepad = LocalMenuHasGamepad.current
    val firstEnabled = state.value.entries.indexOfFirst { it.enabled }
    LaunchedEffect(hasGamepad, firstEnabled) {
        if (hasGamepad && firstEnabled >= 0) runCatching { firstTile.requestFocus() }
    }

    // The four slots are one row when they fit (RPC, landscape) and a 2 x 2 grid otherwise, so no
    // slot ends up alone on a row whose date line falls below the card.
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    val columns = if (maxWidth - 44.dp >= 96.dp * 4 + 14.dp * 3) 4 else 2
    LazyVerticalGrid(
        // Only as tall as the slots, centred in the card.
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(state.value.entries) { index, entry ->
            StateSlot(
                entry = entry,
                modifier = if (index == firstEnabled) Modifier.focusRequester(firstTile) else Modifier,
                onClick = { onStateClicked(index) },
            )
        }
    }
}
}

private const val PREVIEW_ASPECT_RATIO = 4f / 3f

@Composable
private fun StateSlot(
    entry: GameMenuStatesViewModel.StateEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().alpha(if (entry.enabled) 1f else 0.45f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MenuTile(
            modifier = Modifier.fillMaxWidth().aspectRatio(PREVIEW_ASPECT_RATIO),
            shape = RoundedCornerShape(8.dp),
            enabled = entry.enabled,
            onClick = onClick,
        ) { _ ->
            if (entry.preview != null) {
                Image(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    bitmap = entry.preview.asImageBitmap(),
                    contentScale = ContentScale.Fit,
                    contentDescription = entry.title,
                )
            } else {
                Text(
                    text = stringResource(id = R.string.game_menu_state_empty),
                    color = MenuColors.Title,
                    fontSize = 12.sp,
                )
            }
        }
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = entry.title,
            color = MenuColors.Label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = entry.description.ifBlank { stringResource(id = R.string.game_menu_state_no_save) },
            color = MenuColors.Title,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
