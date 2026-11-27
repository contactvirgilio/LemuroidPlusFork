package com.swordfish.lemuroid.app.mobile.feature.systems

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidTexts
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.focusHighlight
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo
import com.swordfish.lemuroid.app.shared.tiles.SystemTileColors
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Route argument for "All systems" (every game). */
const val ALL_SYSTEMS_ID = "ALL"

/** How often the three system tiles on the card change. */
private const val RESHUFFLE_MILLIS = 12 * 60 * 60 * 1000L

/**
 * "All systems" (owner, 2026-11-27): three fanned tiles of systems that have games, picked at
 * random and changed every 12 hours. Opens every game in one list.
 */
@Composable
fun AllSystemsCard(
    modifier: Modifier = Modifier,
    systems: List<MetaSystemInfo>,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    // The current 12-hour period; ticks over while the screen stays open.
    val period by produceState(System.currentTimeMillis() / RESHUFFLE_MILLIS) {
        while (true) {
            delay(RESHUFFLE_MILLIS - System.currentTimeMillis() % RESHUFFLE_MILLIS + 50)
            value = System.currentTimeMillis() / RESHUFFLE_MILLIS
        }
    }
    val withGames = systems.filter { it.count > 0 }
    val picks =
        remember(period, withGames.map { it.metaSystem }) {
            withGames.shuffled(Random(period)).take(3)
        }
    val total = withGames.sumOf { it.count }
    val subtitle = remember(total) { context.getString(R.string.system_grid_details, total.toString()) }

    ElevatedCard(modifier = modifier.focusHighlight(), onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.0f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val tile = maxWidth * 0.46f
                // Back left, back right, then front centre.
                val spots = listOf(Triple(-0.22f, 12f, 0), Triple(0.22f, -12f, 1), Triple(0f, 0f, 2))
                val ordered = when (picks.size) {
                    0 -> emptyList()
                    1 -> listOf(spots[2] to picks[0])
                    2 -> listOf(spots[0] to picks[0], spots[1] to picks[1])
                    else -> spots.mapIndexed { i, s -> s to picks[i] }
                }
                ordered.forEach { (spot, system) ->
                    SystemTile(
                        modifier =
                            Modifier
                                .offset(x = maxWidth * spot.first)
                                .rotate(spot.second)
                                .size(tile),
                        system = system,
                    )
                }
            }
            LemuroidTexts(title = stringResource(R.string.title_all_systems), subtitle = subtitle)
        }
    }
}

@Composable
private fun SystemTile(
    modifier: Modifier,
    system: MetaSystemInfo,
) {
    val colors = SystemTileColors.forMetaSystem(system.metaSystem)
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(Color(colors.base))
                .border(2.dp, Color.Black.copy(alpha = 0.5f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            modifier = Modifier.fillMaxSize(0.78f).padding(2.dp),
            painter = painterResource(id = system.metaSystem.imageResId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = if (SystemTileColors.pastelEnabled) ColorFilter.tint(Color(colors.text)) else null,
        )
    }
}
