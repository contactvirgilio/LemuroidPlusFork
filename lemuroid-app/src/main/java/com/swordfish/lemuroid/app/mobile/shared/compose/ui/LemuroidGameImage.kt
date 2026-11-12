package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.swordfish.lemuroid.app.shared.artwork.rememberGameArtwork
import com.swordfish.lemuroid.app.shared.tiles.GameTileUtils
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun LemuroidGameImage(
    modifier: Modifier = Modifier,
    game: Game,
) {
    val imageModifier =
        modifier
            .fillMaxWidth()
            .aspectRatio(1.0f)

    // Cartridge view: the game's media shape, never artwork.
    if (rememberMediaView()) {
        GameMediaImage(modifier = imageModifier.padding(6.dp), game = game)
        return
    }

    val artwork = rememberGameArtwork(game)

    if (artwork != null) {
        // Artwork fills the tile, keeping its proportions; whatever doesn't fit is trimmed evenly.
        val bitmap = remember(artwork) { artwork.asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = game.displayName(),
            modifier = imageModifier,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        )
        return
    }

    val tileDrawable = remember(game) { GameTileUtils.getTileDrawable(game) }

    Image(
        painter = rememberDrawablePainter(drawable = tileDrawable),
        contentDescription = game.displayName(),
        modifier = imageModifier,
        contentScale = ContentScale.Crop,
    )
}
