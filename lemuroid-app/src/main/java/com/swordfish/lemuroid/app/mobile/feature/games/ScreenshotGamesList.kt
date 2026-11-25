package com.swordfish.lemuroid.app.mobile.feature.games

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.FavoriteToggle
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameTexts
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.ScreenshotPicture
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.focusHighlight
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.gameFocusColor
import com.swordfish.lemuroid.lib.library.db.entity.Game

/**
 * Screenshots style, List layout (like Cannoli, owner 2026-11-25): game names with the heart on the
 * left, and the selected game's screenshot large beside them (above them on a tall screen). With
 * touch, the picture follows the first game on screen; with a controller, the selected one.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ScreenshotGamesList(
    modifier: Modifier,
    games: LazyPagingItems<Game>,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    var focusedId by remember { mutableStateOf<Int?>(null) }
    val focusedGame = focusedId?.let { id -> games.itemSnapshotList.items.firstOrNull { it.id == id } }
    val shown =
        focusedGame
            ?: games.itemCount.takeIf { it > 0 }?.let { games.peek(listState.firstVisibleItemIndex.coerceIn(0, it - 1)) }

    @Composable
    fun Picture(pictureModifier: Modifier) {
        Column(modifier = pictureModifier) {
            if (shown != null) {
                ScreenshotPicture(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f), game = shown)
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = shown.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    @Composable
    fun Names(namesModifier: Modifier) {
        LazyColumn(modifier = namesModifier, state = listState) {
            items(games.itemCount, key = { games.peek(it)?.id ?: it }) { index ->
                val game = games[index] ?: return@items
                var focused by remember { mutableStateOf(false) }
                Surface(
                    modifier =
                        Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .focusHighlight(shape = RectangleShape, zoom = 1f, color = gameFocusColor(game))
                            .onFocusChanged {
                                focused = it.hasFocus
                                if (it.hasFocus) focusedId = game.id
                            }
                            .combinedClickable(onClick = { onGameClick(game) }, onLongClick = { onGameLongClick(game) }),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.size(40.dp)) {
                            FavoriteToggle(isToggled = game.isFavorite, onFavoriteToggle = { onGameFavoriteToggle(game, it) })
                        }
                        LemuroidGameTexts(modifier = Modifier.weight(1f), game = game, focused = focused)
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            Row(modifier = Modifier.fillMaxSize()) {
                Names(Modifier.weight(0.55f).fillMaxSize())
                Picture(Modifier.weight(0.45f).padding(16.dp))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Picture(Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 12.dp))
                Names(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}
