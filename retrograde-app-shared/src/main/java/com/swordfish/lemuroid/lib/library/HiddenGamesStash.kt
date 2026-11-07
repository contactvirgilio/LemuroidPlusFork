package com.swordfish.lemuroid.lib.library

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.json.JSONObject

/**
 * Games of a ROM folder that is not reachable (SD card out) are taken out of the library, so nothing
 * shows up that cannot load. What belongs to the game (favorite, last played, custom name and its id,
 * which play time and mute settings hang on) is kept here and handed back when the folder returns.
 */
class HiddenGamesStash(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun put(games: List<Game>) {
        if (games.isEmpty()) return
        val editor = prefs.edit()
        games.forEach { editor.putString(it.fileUri, encode(it)) }
        editor.apply()
    }

    /** Gives back the stashed details for a game found again, and forgets them. */
    fun restore(
        game: Game,
        isIdFree: (Int) -> Boolean,
    ): Game {
        val raw = prefs.getString(game.fileUri, null) ?: return game
        prefs.edit().remove(game.fileUri).apply()
        return runCatching {
            val json = JSONObject(raw)
            val oldId = json.getInt("id")
            game.copy(
                id = if (isIdFree(oldId)) oldId else game.id,
                isFavorite = json.getBoolean("fav"),
                lastPlayedAt = if (json.isNull("played")) null else json.getLong("played"),
                customName = if (json.isNull("name")) null else json.getString("name"),
                customCoverUri = if (json.isNull("cover")) null else json.getString("cover"),
                coverFrontUrl = if (json.isNull("front")) null else json.getString("front"),
            )
        }.getOrDefault(game)
    }

    private fun encode(game: Game): String =
        JSONObject()
            .put("id", game.id)
            .put("fav", game.isFavorite)
            .put("played", game.lastPlayedAt ?: JSONObject.NULL)
            .put("name", game.customName ?: JSONObject.NULL)
            .put("cover", game.customCoverUri ?: JSONObject.NULL)
            .put("front", game.coverFrontUrl ?: JSONObject.NULL)
            .toString()

    companion object {
        private const val FILE = "hidden_games_stash"
    }
}
