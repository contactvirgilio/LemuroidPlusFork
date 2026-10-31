package com.swordfish.lemuroid.app.mobile.feature.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.focusHighlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Every app on the device with a switch to show or hide it in the Apps tab. */
@Composable
fun HiddenAppsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<LauncherApp>?>(null) }
    var hidden by remember { mutableStateOf(AppsTabSettings.hiddenApps(context)) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadApps(context) }
    }

    val current = apps
    if (current == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = stringResource(R.string.settings_description_hidden_apps_screen),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(current, key = { it.id }) { app ->
            val shown = app.id !in hidden
            val toggle = {
                AppsTabSettings.setHidden(context, app, shown)
                hidden = AppsTabSettings.hiddenApps(context)
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusHighlight(shape = RectangleShape, zoom = 1f)
                        .clickable(onClick = toggle)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(modifier = Modifier.size(40.dp), bitmap = app.icon, contentDescription = null)
                Text(
                    modifier = Modifier.weight(1f),
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Switch(checked = shown, onCheckedChange = { toggle() })
            }
        }
    }
}
