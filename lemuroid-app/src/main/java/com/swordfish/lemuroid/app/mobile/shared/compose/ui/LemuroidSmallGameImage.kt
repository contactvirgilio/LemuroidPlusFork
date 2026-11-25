package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.swordfish.lemuroid.app.shared.screenshots.rememberScreenshotStyle
import com.swordfish.lemuroid.app.shared.tiles.GameTileUtils
import com.swordfish.lemuroid.app.shared.tiles.SystemTileColors
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun LemuroidSmallGameImage(
    modifier: Modifier = Modifier,
    game: Game,
) {
    val imageModifier =
        modifier
            .fillMaxWidth()
            .aspectRatio(1.0f)

    // Follows the chosen style: with Cartridges, small pictures (menus, lists) show the cartridge too.
    if (rememberMediaView()) {
        GameMediaImage(modifier = imageModifier, game = game)
        return
    }

    if (rememberScreenshotStyle()) {
        ScreenshotPicture(modifier = imageModifier, game = game)
        return
    }

    val soft = SystemTileColors.pastelEnabled
    val tileDrawable = remember(game, soft) { GameTileUtils.getTileDrawable(game) }

    Image(
        painter = rememberDrawablePainter(drawable = tileDrawable),
        contentDescription = game.displayName(),
        modifier = imageModifier,
        contentScale = ContentScale.Crop,
    )
}
