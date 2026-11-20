package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * Carousel view of a system's games (test, 2026-11-20): each game drawn as its cartridge or disc,
 * one big in the middle, the neighbours smaller and faded. Swipes snap one game at a time.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameCarousel(
    modifier: Modifier = Modifier,
    count: Int,
    gameAt: (Int) -> Game?,
    key: (Int) -> Any = { it },
    maxItemSize: Dp = 220.dp,
    onClick: (Game) -> Unit,
    onLongClick: (Game) -> Unit,
) {
    if (count == 0) return
    val pagerState = rememberPagerState(pageCount = { count })
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // The middle game takes about half the width (or the height left for it), never more than maxItemSize.
        val itemSize = minOf(maxItemSize, maxWidth * 0.5f, if (maxHeight == Dp.Infinity) maxItemSize else maxHeight * 0.62f)
        val pageWidth = itemSize * 0.8f
        val side = (maxWidth - pageWidth) / 2

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSize = PageSize.Fixed(pageWidth),
            contentPadding = PaddingValues(horizontal = side),
            key = key,
        ) { page ->
            val game = gameAt(page)
            // 0 for the middle game, 1 for its neighbours; only a scale and a fade, nothing heavier.
            val distance = min(1f, abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction))
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused && pagerState.currentPage != page) scope.launch { pagerState.animateScrollToPage(page) } }
                        .focusable()
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (page == pagerState.currentPage) {
                                    game?.let(onClick)
                                } else {
                                    scope.launch { pagerState.animateScrollToPage(page) }
                                }
                            },
                            onLongClick = { if (page == pagerState.currentPage) game?.let(onLongClick) },
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val scale = 1f - 0.4f * distance
                Column(
                    modifier =
                        Modifier
                            // Wider than its page on purpose: the neighbours are shrunk, so they don't overlap.
                            .requiredSize(itemSize)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = 1f - 0.6f * distance
                            },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (game != null) GameMediaImage(modifier = Modifier.fillMaxSize(), game = game)
                }
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 4.dp)
                            .graphicsLayer { alpha = 1f - distance },
                    text = game?.displayName() ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
