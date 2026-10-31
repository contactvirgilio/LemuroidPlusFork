package com.swordfish.lemuroid.app.shared.activity

import android.content.Context
import timber.log.Timber
import java.io.File

/**
 * Play time records for the Activity tab, kept in a small text file in the app's private folder.
 *
 * The game process appends one line per stretch of play (from the game appearing on screen until
 * it's paused, the menu opens or the game is left); the library reads them back. Nothing leaves
 * the device.
 *
 * Line format: sessionId,gameId,systemId,endTimeMillis,durationMillis
 */
object PlayActivityLog {
    private const val FILE_NAME = "play_activity.csv"
    private const val MIN_DURATION_MS = 1_000L

    data class Entry(
        val sessionId: Long,
        val gameId: Int,
        val systemId: String,
        val endTime: Long,
        val duration: Long,
    )

    private fun file(context: Context) = File(context.applicationContext.filesDir, FILE_NAME)

    fun append(
        context: Context,
        sessionId: Long,
        gameId: Int,
        systemId: String,
        endTime: Long,
        duration: Long,
    ) {
        if (duration < MIN_DURATION_MS) return
        runCatching {
            synchronized(this) {
                file(context).appendText("$sessionId,$gameId,$systemId,$endTime,$duration\n")
            }
        }.onFailure { Timber.e(it, "Cannot record play time") }
    }

    fun readAll(context: Context): List<Entry> {
        val file = file(context)
        if (!file.exists()) return emptyList()
        return runCatching {
            file.readLines().mapNotNull { line ->
                val parts = line.split(',')
                if (parts.size != 5) return@mapNotNull null
                Entry(
                    sessionId = parts[0].toLongOrNull() ?: return@mapNotNull null,
                    gameId = parts[1].toIntOrNull() ?: return@mapNotNull null,
                    systemId = parts[2],
                    endTime = parts[3].toLongOrNull() ?: return@mapNotNull null,
                    duration = parts[4].toLongOrNull() ?: return@mapNotNull null,
                )
            }
        }.getOrElse {
            Timber.e(it, "Cannot read play time")
            emptyList()
        }
    }
}

/** Counts how long a game is actually on screen and writes it to [PlayActivityLog]. */
class PlayTimeTracker(
    private val context: Context,
    private val gameId: Int,
    private val systemId: String,
) {
    private val sessionId = System.currentTimeMillis()
    private var resumedAt: Long? = null

    fun onGameVisible() {
        if (resumedAt == null) resumedAt = System.currentTimeMillis()
    }

    fun onGameHidden() {
        val start = resumedAt ?: return
        resumedAt = null
        val now = System.currentTimeMillis()
        PlayActivityLog.append(context, sessionId, gameId, systemId, now, now - start)
    }
}
