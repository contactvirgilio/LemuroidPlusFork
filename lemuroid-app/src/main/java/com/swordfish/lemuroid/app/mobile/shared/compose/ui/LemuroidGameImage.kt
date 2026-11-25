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
import com.swordfish.lemuroid.app.shared.screenshots.rememberGameScreenshot
import com.swordfish.lemuroid.app.shared.screenshots.rememberScreenshotStyle
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import com.swordfish.lemuroid.app.shared.tiles.GameTileUtils
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun LemuroidGameImage(
    modifier: Modifier = Modifier,
    game: Game,
) {
    // Screenshots style: a 4:3 picture with square corners; the game's screenshot whole (never
    // squished, black around it), or its normal tile until one is taken.
    if (rememberScreenshotStyle()) {
        ScreenshotPicture(modifier = modifier.fillMaxWidth().aspectRatio(4f / 3f), game = game)
        return
    }

    val imageModifier =
        modifier
            .fillMaxWidth()
            .aspectRatio(1.0f)

    // Cartridge view: the game's media shape.
    if (rememberMediaView()) {
        GameMediaImage(modifier = imageModifier.padding(6.dp), game = game)
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

/** The game's screenshot fitted whole on black with crisp pixels, or its tile when there is none. */
@Composable
fun ScreenshotPicture(
    modifier: Modifier,
    game: Game,
) {
    val shot = rememberGameScreenshot(game)
    if (shot != null) {
        val bitmap = remember(shot) { shot.asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = game.displayName(),
            modifier = modifier.background(Color.Black),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
        )
        return
    }
    val tileDrawable = remember(game) { GameTileUtils.getTileDrawable(game) }
    Image(
        painter = rememberDrawablePainter(drawable = tileDrawable),
        contentDescription = game.displayName(),
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}
