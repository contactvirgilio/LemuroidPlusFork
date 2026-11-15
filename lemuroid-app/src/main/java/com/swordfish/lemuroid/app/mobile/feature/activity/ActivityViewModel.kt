package com.swordfish.lemuroid.app.mobile.feature.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.activity.PlayActivityLog
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.text.DateFormatSymbols
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** Medal for a game's all-time play time (only games in the all-time top 50 get one). */
    enum class TrophyTier(val hours: Int, val color: Long) {
        BRONZE(5, 0xFFC27C45),
        SILVER(10, 0xFFB8C2CC),
        GOLD(25, 0xFFE8B923),
        DIAMOND(50, 0xFF4CC9F0),
    }

    data class Trophy(val tier: TrophyTier, val rank: Int)

    /** Everything one period shows: totals, the chart and the top lists. */
    data class PeriodData(
        val total: Long = 0,
        val games: Int = 0,
        // One bar per day (week, month) or per month (year, all time).
        val bars: List<Long> = emptyList(),
        val labels: List<String> = emptyList(),
        val currentBar: Int = -1,
        val topGames: List<GameStat> = emptyList(),
        val systems: List<SystemStat> = emptyList(),
    )

    data class State(
        val loaded: Boolean = false,
        val allTimeTotal: Long = 0,
        val periods: Map<ActivityPeriod, PeriodData> = emptyMap(),
        val trophies: Map<Int, Trophy> = emptyMap(),
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
        val periods = ActivityPeriod.values().associateWith { period(entries, it) }
        return State(
            loaded = true,
            allTimeTotal = entries.sumOf { it.duration },
            periods = periods,
            trophies = trophies(entries),
        )
    }

    private suspend fun period(
        entries: List<PlayActivityLog.Entry>,
        period: ActivityPeriod,
    ): PeriodData {
        val now = System.currentTimeMillis()
        val start = midnight(Calendar.getInstance())
        val byMonth = period == ActivityPeriod.YEAR || period == ActivityPeriod.ALL
        val count =
            when (period) {
                ActivityPeriod.WEEK -> 7
                ActivityPeriod.MONTH -> start.getActualMaximum(Calendar.DAY_OF_MONTH)
                ActivityPeriod.YEAR, ActivityPeriod.ALL -> 12
            }
        when (period) {
            ActivityPeriod.WEEK -> start.add(Calendar.DAY_OF_YEAR, -((start.get(Calendar.DAY_OF_WEEK) + 5) % 7))
            ActivityPeriod.MONTH -> start.set(Calendar.DAY_OF_MONTH, 1)
            ActivityPeriod.YEAR -> start.set(Calendar.DAY_OF_YEAR, 1)
            ActivityPeriod.ALL -> {
                // The chart shows the last 12 months; the totals and lists cover everything.
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.add(Calendar.MONTH, -11)
            }
        }
        // Start of every bar, worked out on the calendar so clock changes don't shift a bar.
        val starts =
            LongArray(count) { i ->
                val c = start.clone() as Calendar
                if (byMonth) c.add(Calendar.MONTH, i) else c.add(Calendar.DAY_OF_YEAR, i)
                c.timeInMillis
            }
        val bars = LongArray(count)
        entries.forEach { entry ->
            if (entry.endTime < starts[0]) return@forEach
            val index = starts.indexOfLast { it <= entry.endTime }
            if (index >= 0) bars[index] += entry.duration
        }
        val inPeriod = if (period == ActivityPeriod.ALL) entries else entries.filter { it.endTime >= starts[0] }
        val dayNames = appContext.getString(R.string.activity_week_days).split(',')
        val monthNames = DateFormatSymbols.getInstance().shortMonths
        val labels =
            List(count) { i ->
                val c = Calendar.getInstance().apply { timeInMillis = starts[i] }
                when (period) {
                    ActivityPeriod.WEEK -> dayNames.getOrElse(i) { "" }
                    ActivityPeriod.MONTH -> if (i % 7 == 0) "${i + 1}" else ""
                    else -> monthNames[c.get(Calendar.MONTH)].take(1)
                }
            }
        return PeriodData(
            total = inPeriod.sumOf { it.duration },
            games = inPeriod.map { it.gameId }.distinct().size,
            bars = bars.toList(),
            labels = labels,
            currentBar = starts.indexOfLast { it <= now },
            topGames = topGames(inPeriod, MAX_TOP_GAMES),
            systems = systems(inPeriod),
        )
    }

    private fun trophies(entries: List<PlayActivityLog.Entry>): Map<Int, Trophy> =
        entries.groupBy { it.gameId }
            .map { (id, list) -> id to list.sumOf { it.duration } }
            .sortedByDescending { it.second }
            .take(TROPHY_TOP)
            .mapIndexedNotNull { i, (id, duration) ->
                val hours = duration / 3_600_000
                val tier = TrophyTier.values().lastOrNull { hours >= it.hours } ?: return@mapIndexedNotNull null
                id to Trophy(tier, i + 1)
            }
            .toMap()
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

    private fun midnight(calendar: Calendar): Calendar {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }

    companion object {
        private const val FBNEO_ID = "fbneo"
        private const val MAME_ID = "mame2003plus"
        private const val MAX_TOP_GAMES = 25
        private const val TROPHY_TOP = 50
        private const val MIN_SESSION_MS = 5 * 60 * 1000L
    }
}
