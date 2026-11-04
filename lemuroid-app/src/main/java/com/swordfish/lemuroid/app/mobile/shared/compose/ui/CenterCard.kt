package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.app.shared.ui.CenterCards
import kotlinx.coroutines.delay

private val CARD_BACKGROUND = Color(0xFF0A0C0E).copy(alpha = 0.82f)
private val CARD_TITLE = Color(0xFF9AA4AD)
private val CARD_ACCENT = Color(0xFFFFC107)

/**
 * The card in the middle of the screen: small title, big value and, when there are several
 * choices, one bar per choice with the current one lit.
 */
@Composable
fun CenterCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    index: Int = 0,
    count: Int = 0,
) {
    Column(
        modifier =
            modifier
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CARD_BACKGROUND)
                .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title.uppercase(),
            color = CARD_TITLE,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (count > 0) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(count) { bar ->
                    Box(
                        modifier =
                            Modifier
                                .size(width = 20.dp, height = 5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (bar == index) CARD_ACCENT else Color.White.copy(alpha = 0.22f)),
                    )
                }
            }
        }
    }
}

/** Draws the messages of [CenterCards] over the screen it is placed in. */
@Composable
fun CenterCardHost(modifier: Modifier = Modifier) {
    DisposableEffect(Unit) {
        CenterCards.hostAttached = true
        onDispose { CenterCards.hostAttached = false }
    }

    val message by CenterCards.messages.collectAsState()
    var lastShown by remember { mutableStateOf<CenterCards.Message?>(null) }
    if (message != null) lastShown = message

    LaunchedEffect(message?.id) {
        val current = message ?: return@LaunchedEffect
        delay(current.millis)
        CenterCards.clear(current.id)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
            lastShown?.let { CenterCard(title = it.title, value = it.value) }
        }
    }
}
