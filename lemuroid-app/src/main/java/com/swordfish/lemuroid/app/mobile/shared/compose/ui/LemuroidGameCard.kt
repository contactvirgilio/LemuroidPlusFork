package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.screenshots.rememberScreenshotStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LemuroidGameCard(
    modifier: Modifier = Modifier,
    game: Game,
    onClick: () -> Unit = { },
    onLongClick: () -> Unit = { },
) {
    var focused by remember { mutableStateOf(false) }
    // Screenshots style: square corners, and a heart before the name of a favourite.
    val screenshots = rememberScreenshotStyle()
    val shape = if (screenshots) RectangleShape else CardDefaults.elevatedShape
    ElevatedCard(
        modifier =
            if (screenshots) {
                modifier.focusHighlight(shape = RectangleShape, color = gameFocusColor(game))
            } else {
                modifier.focusHighlight(color = gameFocusColor(game))
            },
        shape = shape,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.hasFocus }
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ),
        ) {
            LemuroidGameImage(game = game)
            if (screenshots) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (game.isFavorite) FavoriteHeart(modifier = Modifier.padding(start = 8.dp))
                    LemuroidGameTexts(modifier = Modifier.weight(1f), game = game, lines = 2, focused = focused)
                }
            } else {
                LemuroidGameTexts(game = game, lines = 2, focused = focused)
            }
        }
    }
}
