package com.swordfish.lemuroid.app.shared.tiles

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.swordfish.lemuroid.lib.library.db.entity.Game

object GameTileUtils {
    fun loadTile(
        game: Game,
        imageView: ImageView?,
    ) {
        imageView?.setImageDrawable(getTileDrawable(game))
    }

    fun getTileDrawable(game: Game): Drawable = GameTileDrawable(computeTitle(game), getColors(game))

    fun getColors(game: Game): SystemTileColors = SystemTileColors.forSystemId(game.systemId)

    /** Remote rendering of the default tile, used only where a URI is required (Android TV channels). */
    fun getTileRemoteUrl(game: Game): String {
        val colors = getColors(game)
        val background = Integer.toHexString(colors.base).substring(2)
        val text = Integer.toHexString(colors.text).substring(2)
        return "https://fakeimg.pl/512x512/$background/$text/?font=bebas&text=${computeTitle(game)}"
    }

    private fun computeTitle(game: Game): String {
        val sanitizedName =
            game.displayName()
                .replace(Regex("\\(.*\\)"), "")

        return sanitizedName.asSequence()
            .filter { it.isDigit() or it.isUpperCase() or (it == '&') }
            .take(3)
            .joinToString("")
            .ifBlank { game.displayName().first().toString() }
            .uppercase()
    }
}
