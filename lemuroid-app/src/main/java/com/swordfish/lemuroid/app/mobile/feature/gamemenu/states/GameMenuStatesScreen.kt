package com.swordfish.lemuroid.app.mobile.feature.gamemenu.states

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.launch

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val gap = 14.dp
        val slotModifier = { index: Int -> if (index == firstEnabled) Modifier.focusRequester(firstTile) else Modifier }
        if (maxWidth > maxHeight) {
            // Option A (wide cards, e.g. RPC): one row of big slots filling the card's height; the
            // last one peeks in at the right and the row scrolls sideways to it.
            val byHeight = (maxHeight - 24.dp - SLOT_TEXT_HEIGHT) * PREVIEW_ASPECT_RATIO
            val byWidth = (maxWidth - 22.dp - gap * 3) / VISIBLE_SLOTS_IN_ROW
            val slotWidth = minOf(byHeight, byWidth).coerceAtLeast(96.dp)
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val count = state.value.entries.size
            var focused by remember { mutableStateOf(-1) }
            // The slot the dots point at: the focused one (gamepad), else the one at the left edge,
            // or the last one once the row is scrolled to the end.
            val current =
                when {
                    focused >= 0 -> focused
                    !listState.canScrollForward && count > 0 -> count - 1
                    else -> listState.firstVisibleItemIndex
                }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    LazyRow(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        itemsIndexed(state.value.entries) { index, entry ->
                            StateSlot(
                                entry = entry,
                                modifier =
                                    slotModifier(index)
                                        .width(slotWidth)
                                        .onFocusChanged { if (it.isFocused) focused = index },
                                onClick = { onStateClicked(entry.slot) },
                            )
                        }
                    }
                    // Hint that more slots follow: a fade at the right edge and a solid arrow
                    // that scrolls to the next ones. Gone once the row reaches the end.
                    if (listState.canScrollForward) {
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(64.dp)
                                    .height(slotWidth / PREVIEW_ASPECT_RATIO + 8.dp + SLOT_TEXT_HEIGHT)
                                    .background(Brush.horizontalGradient(listOf(Color.Transparent, MenuColors.Card))),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp + slotWidth / PREVIEW_ASPECT_RATIO / 2 - 20.dp, end = 10.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    // Touch only: the D-pad moves between the slots, not to this.
                                    .focusProperties { canFocus = false }
                                    .clickable {
                                        scope.launch { listState.animateScrollToItem((current + 1).coerceAtMost(count - 1)) }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }
                    }
                }
                // One dot per slot; the current one is a longer accent pill.
                Row(
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(count) { i ->
                        Box(
                            modifier =
                                Modifier
                                    .size(width = if (i == current) 22.dp else 9.dp, height = 9.dp)
                                    .clip(CircleShape)
                                    .background(if (i == current) MenuColors.Gold else Color.White.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
        } else {
            // Option B (kept): 2 x 2, as big as the card allows, all four without scrolling.
            val byWidth = (maxWidth - 44.dp - gap) / 2
            val byHeight = ((maxHeight - 24.dp - gap) / 2 - SLOT_TEXT_HEIGHT) * PREVIEW_ASPECT_RATIO
            val slotWidth = minOf(byWidth, byHeight).coerceAtLeast(96.dp)
            LazyVerticalGrid(
                modifier = Modifier.width(slotWidth * 2 + gap + 44.dp).wrapContentHeight(),
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                itemsIndexed(state.value.entries) { index, entry ->
                    StateSlot(
                        entry = entry,
                        modifier = slotModifier(index),
                        onClick = { onStateClicked(entry.slot) },
                    )
                }
            }
        }
    }
}

// In the one-row layout: about 2.3 slots across, so the next one visibly peeks in.
private const val VISIBLE_SLOTS_IN_ROW = 2.3f

private const val PREVIEW_ASPECT_RATIO = 4f / 3f

/** Gold, as the page dots and the shortcut toggles. */
private val LATEST_PILL_COLOR = Color(0xFFF5C828)

/** White, so the auto-save doesn't read as "Latest". */
private val AUTO_PILL_COLOR = Color(0xFFF1EFE8)

// Name and date under each picture.
private val SLOT_TEXT_HEIGHT = 46.dp

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
            Box(modifier = Modifier.fillMaxSize()) {
                if (entry.preview != null) {
                    Image(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        bitmap = entry.preview.asImageBitmap(),
                        contentScale = ContentScale.Fit,
                        contentDescription = entry.title,
                    )
                } else {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(id = R.string.game_menu_state_empty),
                        color = MenuColors.Title,
                        fontSize = 12.sp,
                    )
                }
                if (entry.auto || entry.latest) {
                    Text(
                        modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .background(if (entry.auto) AUTO_PILL_COLOR else LATEST_PILL_COLOR, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        text = stringResource(id = if (entry.auto) R.string.game_menu_state_auto else R.string.game_menu_state_latest),
                        color = Color(0xFF1E1804),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
