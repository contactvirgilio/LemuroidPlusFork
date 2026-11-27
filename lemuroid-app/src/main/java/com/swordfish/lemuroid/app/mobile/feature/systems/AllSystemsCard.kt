package com.swordfish.lemuroid.app.mobile.feature.systems

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidTexts
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.focusHighlight
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo

/** Route argument for "All systems" (every game). */
const val ALL_SYSTEMS_ID = "ALL"

/** Charcoal: no system uses a dark neutral, so this card stands apart (owner, 2026-11-27). */
private val ALL_SYSTEMS_TILE = Color(0xFF2A2D34)

/**
 * "All systems": the owner's boss key, flattened in two-tone gold, on a charcoal tile.
 * Opens every game in one list.
 */
@Composable
fun AllSystemsCard(
    modifier: Modifier = Modifier,
    systems: List<MetaSystemInfo>,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val total = systems.sumOf { it.count }
    val subtitle = remember(total) { context.getString(R.string.system_grid_details, total.toString()) }

    ElevatedCard(modifier = modifier.focusHighlight(), onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.0f)
                        .background(ALL_SYSTEMS_TILE),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(0.75f),
                    painter = painterResource(id = R.drawable.all_systems_key),
                    contentDescription = stringResource(R.string.title_all_systems),
                    contentScale = ContentScale.Fit,
                )
            }
            LemuroidTexts(title = stringResource(R.string.title_all_systems), subtitle = subtitle)
        }
    }
}
