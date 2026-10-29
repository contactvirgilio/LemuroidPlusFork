package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Shows only the game name: system and publisher are intentionally left out. */
@Composable
fun LemuroidGameTexts(
    modifier: Modifier = Modifier,
    game: Game,
    lines: Int = 1,
) {
    Text(
        modifier = modifier.padding(8.dp),
        text = game.displayName(),
        style = MaterialTheme.typography.titleSmall,
        minLines = lines,
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun LemuroidTexts(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = modifier.padding(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
