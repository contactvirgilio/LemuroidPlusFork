package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.swordfish.lemuroid.app.shared.tiles.GameTileUtils
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun LemuroidSmallGameImage(
    modifier: Modifier = Modifier,
    game: Game,
) {
    val tileDrawable = remember(game) { GameTileUtils.getTileDrawable(game) }

    Image(
        painter = rememberDrawablePainter(drawable = tileDrawable),
        contentDescription = game.displayName(),
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(1.0f),
        contentScale = ContentScale.Crop,
    )
}
