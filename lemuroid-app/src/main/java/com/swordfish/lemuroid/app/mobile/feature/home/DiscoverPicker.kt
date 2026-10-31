package com.swordfish.lemuroid.app.mobile.feature.home

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.dao.DiscoveryCandidate
import java.util.TimeZone
import kotlin.random.Random

/**
 * Picks the games shown in Discover: a fresh set every day that stays the same all day.
 *
 * Unplayed games come first, with one game from as many different systems as possible; the rest
 * of the slots are filled with more unplayed games, and only then with played ones (so Discover
 * keeps working once everything has been played). The day's picks are remembered, so playing a
 * Discover game doesn't make the list change until tomorrow.
 */
class DiscoverPicker(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun pick(
        candidates: List<DiscoveryCandidate>,
        count: Int,
        today: Long = currentLocalDay(),
    ): List<Int> {
        val existingIds = candidates.mapTo(HashSet()) { it.id }

        val day = today.toString()
        if (preferences.getString(KEY_DAY, null) == day) {
            val saved =
                preferences.getString(KEY_IDS, null)
                    ?.split(',')
                    ?.mapNotNull { it.toIntOrNull() }
                    .orEmpty()
            // Reuse today's picks unless games were removed or the library grew from empty.
            if (saved.isNotEmpty() && saved.all { it in existingIds } && saved.size >= minOf(count, candidates.size)) {
                return saved
            }
        }

        val picked = pickFresh(candidates, count, Random(today))
        preferences.edit()
            .putString(KEY_DAY, day)
            .putString(KEY_IDS, picked.joinToString(","))
            .apply()
        return picked
    }

    private fun pickFresh(
        candidates: List<DiscoveryCandidate>,
        count: Int,
        random: Random,
    ): List<Int> {
        val (unplayed, played) = candidates.partition { !it.played }
        val result = LinkedHashSet<Int>()

        // One unplayed game per system, systems in a random order.
        unplayed.groupBy { it.systemId }
            .values
            .shuffled(random)
            .forEach { games ->
                if (result.size < count) result.add(games.random(random).id)
            }

        // Then any other unplayed games, then played ones.
        unplayed.shuffled(random).forEach { if (result.size < count) result.add(it.id) }
        played.shuffled(random).forEach { if (result.size < count) result.add(it.id) }

        return result.toList()
    }

    companion object {
        /** Days since 1970 in the device's time zone, so the set changes at local midnight. */
        fun currentLocalDay(): Long {
            val now = System.currentTimeMillis()
            return (now + TimeZone.getDefault().getOffset(now)) / (24L * 60 * 60 * 1000)
        }

        private const val PREFS_NAME = "home_discover"
        private const val KEY_DAY = "day"
        private const val KEY_IDS = "ids"
    }
}
