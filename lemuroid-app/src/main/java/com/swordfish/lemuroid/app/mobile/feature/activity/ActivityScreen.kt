package com.swordfish.lemuroid.app.mobile.feature.activity

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(
                    Modifier.weight(1f),
                    formatDuration(state.weekTotal),
                    stringResource(R.string.activity_this_week),
                )
                StatCard(
                    Modifier.weight(1f),
                    state.weekGames.toString(),
                    stringResource(R.string.activity_games_this_week),
                )
                StatCard(
                    Modifier.weight(1f),
                    formatDuration(state.allTimeTotal),
                    stringResource(R.string.activity_all_time),
                )
            }
        }
        item { SectionTitle(stringResource(R.string.activity_this_week)) }
        item { WeekChart(state.days, state.todayIndex) }
        if (state.topGames.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.activity_most_played)) }
            items(state.topGames, key = { "game-${it.game.id}" }) { stat -> GameRow(stat) }
        }
        if (state.systems.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.activity_by_system)) }
            val longest = state.systems.first().duration.coerceAtLeast(1)
            items(state.systems, key = { "system-${it.systemId}" }) { stat -> SystemRow(stat, longest) }
        }
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
private fun WeekChart(
    days: List<Long>,
    todayIndex: Int,
) {
    val dayLabels = stringResource(R.string.activity_week_days).split(',')
    val longest = days.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEachIndexed { index, duration ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.height(80.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((duration.toFloat() / longest).coerceAtLeast(0.03f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (index == todayIndex) TODAY_COLOR else FocusHighlightColor.copy(alpha = 0.85f),
                                ),
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = dayLabels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GameRow(stat: ActivityViewModel.GameStat) {
    val systemTitleId =
        remember(stat.game.systemId) { runCatching { GameSystem.findById(stat.game.systemId).titleResId }.getOrNull() }
    val systemName = systemTitleId?.let { stringResource(it) } ?: ""
    Row(
        modifier = Modifier.fillMaxWidth().focusHighlight(zoom = 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LemuroidSmallGameImage(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(7.dp)),
            game = stat.game,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stat.game.displayName(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.activity_game_details, systemName, stat.sessions),
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
        remember(stat.systemId) { runCatching { GameSystem.findById(stat.systemId).shortTitleResId }.getOrNull() }
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

private val TODAY_COLOR = Color(0xFF5EEAD4)
