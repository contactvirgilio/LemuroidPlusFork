package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.focusHighlight

/**
 * The app icon's colour (2026-11-21): one launcher entry per colour in the manifest; the chosen
 * one is turned on and the others off. Orange is on by default.
 */
object AppIcon {
    enum class Choice(val alias: String, val color: Color) {
        ORANGE("AppIconOrange", Color(0xFFEC8428)),
        GREEN("AppIconGreen", Color(0xFF00C64E)),
        RED("AppIconRed", Color(0xFFE5534B)),
        YELLOW("AppIconYellow", Color(0xFFF2C94C)),
        BLUE("AppIconBlue", Color(0xFF3C6EDC)),
        PURPLE("AppIconPurple", Color(0xFF8C50D2)),
        PINK("AppIconPink", Color(0xFFE864AA)),
        GREY("AppIconGrey", Color(0xFF8A8D93)),
    }

    private fun component(
        context: Context,
        choice: Choice,
    ) = ComponentName(context.packageName, "com.swordfish.lemuroid.app.mobile.feature.main.${choice.alias}")

    fun current(context: Context): Choice {
        val pm = context.packageManager
        return Choice.values().firstOrNull {
            pm.getComponentEnabledSetting(component(context, it)) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } ?: Choice.ORANGE
    }

    fun set(
        context: Context,
        choice: Choice,
    ) {
        val pm = context.packageManager
        // Turn the new one on first, so there is never a moment with no icon at all.
        pm.setComponentEnabledSetting(component(context, choice), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        Choice.values().filter { it != choice }.forEach {
            pm.setComponentEnabledSetting(component(context, it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }
}

@Composable
fun AppIconPicker() {
    val context = LocalContext.current
    var current by remember { mutableStateOf(AppIcon.current(context)) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(text = stringResource(R.string.settings_title_app_icon), style = MaterialTheme.typography.titleMedium)
        Text(
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            text = stringResource(R.string.settings_description_app_icon),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppIcon.Choice.values().toList().chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { choice ->
                    val shape = RoundedCornerShape(22)
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .border(3.dp, if (choice == current) Color(0xFF5DCAA5) else Color.Transparent, shape)
                                .padding(4.dp)
                                .clip(shape)
                                .background(choice.color)
                                .focusHighlight(shape = shape, zoom = 1f)
                                .clickable {
                                    if (choice != current) {
                                        AppIcon.set(context, choice)
                                        current = choice
                                    }
                                },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.lemuroid_launcher_foreground),
                            contentDescription = choice.name.lowercase(),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
