package com.swordfish.lemuroid.app.mobile.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.FocusHighlightColor
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidSmallGameImage
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.focusHighlight
import com.swordfish.lemuroid.app.shared.tiles.SystemTileColors
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.MetaSystemID

/** Simple play tracker: this week, a bar per day, most played games and time per system. */
@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel,
) {
    // Recompute whenever the screen comes back, e.g. after playing a game.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state = viewModel.getState().collectAsState().value
    if (!state.loaded) return

    val context = LocalContext.current
    var period by remember { mutableStateOf(ActivityRange.period(context)) }
    var topCount by remember { mutableIntStateOf(ActivityRange.topCount(context)) }
    val data = state.periods[period] ?: ActivityViewModel.PeriodData()
    val topGames = data.topGames.take(topCount)
    val periodLabel =
        stringResource(
            when (period) {
                ActivityPeriod.WEEK -> R.string.activity_this_week
                ActivityPeriod.MONTH -> R.string.activity_this_month
                ActivityPeriod.YEAR -> R.string.activity_this_year
                ActivityPeriod.ALL -> R.string.activity_all_time
            },
        )

    if (state.allTimeTotal == 0L) {
        Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.activity_empty),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PillRow(
                options = ActivityPeriod.values().toList(),
                selected = period,
                label = {
                    stringResource(
                        when (it) {
                            ActivityPeriod.WEEK -> R.string.activity_week
                            ActivityPeriod.MONTH -> R.string.activity_month
                            ActivityPeriod.YEAR -> R.string.activity_year
                            ActivityPeriod.ALL -> R.string.activity_all_time
                        },
                    )
                },
                onSelect = {
                    period = it
                    ActivityRange.setPeriod(context, it)
                },
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(Modifier.weight(1f), formatDuration(data.total), periodLabel)
                StatCard(
                    Modifier.weight(1f),
                    data.games.toString(),
                    stringResource(R.string.activity_games_played),
                )
                StatCard(
                    Modifier.weight(1f),
                    formatDuration(state.allTimeTotal),
                    stringResource(R.string.activity_total),
                )
            }
        }
        item { BarChart(data.bars, data.labels, data.currentBar) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) { SectionTitle(stringResource(R.string.activity_most_played)) }
                PillRow(
                    options = ActivityRange.TOP_COUNTS,
                    selected = topCount,
                    label = { stringResource(R.string.activity_top_count, it) },
                    onSelect = {
                        topCount = it
                        ActivityRange.setTopCount(context, it)
                    },
                )
            }
        }
        if (topGames.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.activity_nothing_period),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(topGames, key = { _, it -> "game-${it.game.id}" }) { index, stat ->
            GameRow(index + 1, stat, state.trophies[stat.game.id])
        }
        if (data.systems.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.activity_by_system)) }
            val longest = data.systems.first().duration.coerceAtLeast(1)
            items(data.systems, key = { "system-${it.systemId}" }) { stat -> SystemRow(stat, longest) }
        }
    }
}

/** A row of pills, one selected (period, or how many top games). */
@Composable
private fun <T> PillRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp),
    ) {
        options.forEach { option ->
            RangePill(label(option), selected = option == selected) { onSelect(option) }
        }
    }
}

@Composable
private fun RangePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .focusHighlight(shape = RoundedCornerShape(17.dp), zoom = 1f)
                .clip(RoundedCornerShape(17.dp))
                .background(if (selected) FocusHighlightColor else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color(0xFF062A25) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    value: String,
    label: String,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
    ) {
        Text(text = value, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        modifier = Modifier.padding(top = 6.dp),
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun BarChart(
    bars: List<Long>,
    labels: List<String>,
    currentBar: Int,
) {
    val longest = bars.maxOrNull()?.coerceAtLeast(1) ?: 1
    val gap = if (bars.size > 12) 2.dp else 6.dp
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        bars.forEachIndexed { index, duration ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.height(80.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((duration.toFloat() / longest).coerceAtLeast(0.03f))
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(
                                    if (index == currentBar) TODAY_COLOR else FocusHighlightColor.copy(alpha = 0.85f),
                                ),
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun GameRow(
    rank: Int,
    stat: ActivityViewModel.GameStat,
    trophy: ActivityViewModel.Trophy?,
) {
    val systemTitleId =
        remember(stat.game.systemId) {
            if (isArcadeId(stat.game.systemId)) {
                MetaSystemID.ARCADE.titleResId
            } else {
                runCatching { GameSystem.findById(stat.game.systemId).titleResId }.getOrNull()
            }
        }
    val systemName = systemTitleId?.let { stringResource(it) } ?: ""
    Row(
        modifier = Modifier.fillMaxWidth().focusHighlight(zoom = 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            modifier = Modifier.width(22.dp),
            text = "$rank",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (rank <= 3) FocusHighlightColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LemuroidSmallGameImage(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(7.dp)),
            game = stat.game,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = stat.game.displayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (trophy != null) {
                    Icon(
                        modifier = Modifier.padding(start = 6.dp).size(16.dp),
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = stringResource(trophyLabel(trophy.tier)),
                        tint = Color(trophy.tier.color),
                    )
                }
            }
            Text(
                text =
                    if (trophy == null) {
                        systemName
                    } else {
                        stringResource(R.string.activity_trophy_line, systemName, stringResource(trophyLabel(trophy.tier)), trophy.rank)
                    },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(text = formatDuration(stat.duration), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SystemRow(
    stat: ActivityViewModel.SystemStat,
    longest: Long,
) {
    val shortTitleId =
        remember(stat.systemId) {
            if (isArcadeId(stat.systemId)) {
                MetaSystemID.ARCADE.titleResId
            } else {
                runCatching { GameSystem.findById(stat.systemId).shortTitleResId }.getOrNull()
            }
        }
    val name = shortTitleId?.let { stringResource(it) } ?: stat.systemId
    val color = Color(SystemTileColors.forSystemId(stat.systemId).base)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(modifier = Modifier.width(64.dp), text = name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((stat.duration.toFloat() / longest).coerceIn(0.02f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(color),
            )
        }
        Text(
            modifier = Modifier.width(64.dp),
            text = formatDuration(stat.duration),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours >= 100 -> "${hours}h"
        hours > 0 -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
        else -> "${minutes}m"
    }
}

private fun trophyLabel(tier: ActivityViewModel.TrophyTier) =
    when (tier) {
        ActivityViewModel.TrophyTier.BRONZE -> R.string.activity_trophy_bronze
        ActivityViewModel.TrophyTier.SILVER -> R.string.activity_trophy_silver
        ActivityViewModel.TrophyTier.GOLD -> R.string.activity_trophy_gold
        ActivityViewModel.TrophyTier.DIAMOND -> R.string.activity_trophy_diamond
    }

private val TODAY_COLOR = Color(0xFF5EEAD4)

/** FBNeo and MAME are both called "Arcade" in the library, so Activity uses that name too. */
private fun isArcadeId(systemId: String) = systemId == "fbneo" || systemId == "mame2003plus"
