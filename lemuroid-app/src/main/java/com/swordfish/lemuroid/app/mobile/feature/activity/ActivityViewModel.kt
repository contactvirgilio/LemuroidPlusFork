package com.swordfish.lemuroid.app.mobile.feature.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.app.shared.activity.PlayActivityLog
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ActivityViewModel(
    private val appContext: Context,
    private val retrogradeDb: RetrogradeDatabase,
) : ViewModel() {
    class Factory(
        private val appContext: Context,
        private val retrogradeDb: RetrogradeDatabase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ActivityViewModel(appContext, retrogradeDb) as T
        }
    }

    data class GameStat(val game: Game, val duration: Long, val sessions: Int)

    data class SystemStat(val systemId: String, val duration: Long)

    data class State(
        val loaded: Boolean = false,
        val weekTotal: Long = 0,
        val weekGames: Int = 0,
        val allTimeTotal: Long = 0,
        // Monday to Sunday of the current week.
        val days: List<Long> = List(7) { 0L },
        val todayIndex: Int = 0,
        val monthTopGames: List<GameStat> = emptyList(),
        val monthSystems: List<SystemStat> = emptyList(),
        val allTimeTopGames: List<GameStat> = emptyList(),
        val allTimeSystems: List<SystemStat> = emptyList(),
    )

    private val state = MutableStateFlow(State())

    fun getState(): StateFlow<State> = state

    fun refresh() {
        viewModelScope.launch {
            state.value = withContext(Dispatchers.IO) { computeState() }
        }
    }

    private suspend fun computeState(): State {
        val entries = PlayActivityLog.readAll(appContext)

        val weekStart = startOfWeek()
        val dayLength = 24L * 60 * 60 * 1000
        val days = LongArray(7)
        val thisWeek = entries.filter { it.endTime >= weekStart }
        thisWeek.forEach { entry ->
            val day = ((entry.endTime - weekStart) / dayLength).toInt().coerceIn(0, 6)
            days[day] += entry.duration
        }

        val thisMonth = entries.filter { it.endTime >= startOfMonth() }

        val now = System.currentTimeMillis()
        return State(
            loaded = true,
            weekTotal = thisWeek.sumOf { it.duration },
            weekGames = thisWeek.map { it.gameId }.distinct().size,
            allTimeTotal = entries.sumOf { it.duration },
            days = days.toList(),
            todayIndex = ((now - weekStart) / dayLength).toInt().coerceIn(0, 6),
            monthTopGames = topGames(thisMonth, MONTH_TOP_GAMES),
            monthSystems = systems(thisMonth),
            allTimeTopGames = topGames(entries, ALL_TIME_TOP_GAMES),
            allTimeSystems = systems(entries),
        )
    }

    /** Most played games; games without a full session (5 minutes) aren't listed. */
    private suspend fun topGames(
        entries: List<PlayActivityLog.Entry>,
        count: Int,
    ): List<GameStat> {
        val byGame = entries.groupBy { it.gameId }.filterValues { countSessions(it) > 0 }
        val topIds =
            byGame.entries
                .sortedByDescending { (_, list) -> list.sumOf { it.duration } }
                .take(count * 2)
                .map { it.key }
        val games = retrogradeDb.gameDao().selectByIds(topIds).associateBy { it.id }
        return topIds.mapNotNull { id ->
            val game = games[id] ?: return@mapNotNull null
            val list = byGame.getValue(id)
            GameStat(game, list.sumOf { it.duration }, countSessions(list))
        }.take(count)
    }

    /** A session (one time the game was opened) only counts once it reached 5 minutes of play. */
    private fun countSessions(entries: List<PlayActivityLog.Entry>): Int {
        return entries.groupBy { it.sessionId }
            .count { (_, list) -> list.sumOf { it.duration } >= MIN_SESSION_MS }
    }

    private fun systems(entries: List<PlayActivityLog.Entry>): List<SystemStat> {
        // Both arcade systems (FBNeo and MAME) count as one "Arcade" row.
        return entries.groupBy { if (it.systemId == MAME_ID) FBNEO_ID else it.systemId }
            .map { (systemId, list) -> SystemStat(systemId, list.sumOf { it.duration }) }
            .sortedByDescending { it.duration }
    }

    /** The 1st of the current month at 00:00, in local time. */
    private fun startOfMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /** Monday 00:00 of the current week, in local time. */
    private fun startOfWeek(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val daysSinceMonday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
        calendar.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        return calendar.timeInMillis
    }

    companion object {
        private const val FBNEO_ID = "fbneo"
        private const val MAME_ID = "mame2003plus"
        private const val MONTH_TOP_GAMES = 5
        private const val ALL_TIME_TOP_GAMES = 10
        private const val MIN_SESSION_MS = 5 * 60 * 1000L
    }
}
