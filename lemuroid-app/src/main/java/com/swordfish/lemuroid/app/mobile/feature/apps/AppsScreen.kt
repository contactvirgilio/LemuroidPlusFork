package com.swordfish.lemuroid.app.mobile.feature.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.ui.CenterCards
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.focusHighlight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** An installed app that can be opened from the launcher. */
data class LauncherApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap,
) {
    val id: String
        get() = "$packageName/$activityName"
}

/** Grid of the device's Android apps, so Lemuroid Plus can work as the home screen. */
@Composable
fun AppsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Starts from the last loaded list, so coming back to the tab shows the apps at once.
    var apps by remember {
        val hidden = AppsTabSettings.hiddenApps(context)
        mutableStateOf(cachedApps()?.filter { it.id !in hidden })
    }
    var reloadKey by remember { mutableIntStateOf(0) }

    // Reload when coming back to the screen, so newly installed or removed apps show up.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) reloadKey++
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(reloadKey) {
        apps =
            withContext(Dispatchers.IO) {
                val hidden = AppsTabSettings.hiddenApps(context)
                loadApps(context).filter { it.id !in hidden }
            }
    }

    val current = apps
    if (current == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Adaptive(minSize = 88.dp),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(current, key = { it.id }) { app ->
            AppTile(
                app = app,
                onOpen = { openApp(context, app) },
                onAppInfo = { openAppInfo(context, app) },
                onHide = {
                    // It can be shown again in Settings > Home screen.
                    AppsTabSettings.setHidden(context, app, true)
                    apps = apps?.filter { it.id != app.id }
                    CenterCards.show(context.getString(R.string.apps_card_hidden_title), app.label.toString())
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: LauncherApp,
    onOpen: () -> Unit,
    onAppInfo: () -> Unit,
    onHide: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusHighlight()
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(onClick = onOpen, onLongClick = { menuOpen = true })
                    .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                modifier = Modifier.size(52.dp),
                bitmap = app.icon,
                contentDescription = app.label,
            )
            Text(
                modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
                text = app.label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Held down: a small menu instead of hiding straight away.
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.apps_menu_open)) },
                onClick = {
                    menuOpen = false
                    onOpen()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.apps_menu_info)) },
                onClick = {
                    menuOpen = false
                    onAppInfo()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.apps_menu_hide)) },
                onClick = {
                    menuOpen = false
                    onHide()
                },
            )
        }
    }
}

private fun openAppInfo(
    context: Context,
    app: LauncherApp,
) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { Timber.e(it, "Cannot open app info for ${app.packageName}") }
}

/** The last loaded list, reused while the same apps (and versions) are installed. */
private object AppsCache {
    var signature: Set<String>? = null
    var apps: List<LauncherApp>? = null
}

/** The list from the last load, if any. No waiting: used to show the screen at once. */
internal fun cachedApps(): List<LauncherApp>? = synchronized(AppsCache) { AppsCache.apps }

internal fun loadApps(context: Context): List<LauncherApp> {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val iconSize = (52 * context.resources.displayMetrics.density).toInt()
    val infos =
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != context.packageName }

    // Reading names and icons is the slow part. Skip it when nothing was installed, removed or
    // updated since the last load.
    val updateTimes = HashMap<String, Long>()
    val signature =
        infos.mapTo(HashSet()) { info ->
            val packageName = info.activityInfo.packageName
            val updated =
                updateTimes.getOrPut(packageName) {
                    runCatching { packageManager.getPackageInfo(packageName, 0).lastUpdateTime }.getOrDefault(0L)
                }
            "$packageName/${info.activityInfo.name}@$updated"
        }
    synchronized(AppsCache) {
        val cached = AppsCache.apps
        if (cached != null && AppsCache.signature == signature) return cached
    }

    val loaded = readApps(infos, packageManager, iconSize)
    synchronized(AppsCache) {
        AppsCache.signature = signature
        AppsCache.apps = loaded
    }
    return loaded
}

private fun readApps(
    infos: List<ResolveInfo>,
    packageManager: PackageManager,
    iconSize: Int,
): List<LauncherApp> {
    return infos
        .mapNotNull { info ->
            runCatching {
                LauncherApp(
                    label = info.loadLabel(packageManager).toString(),
                    packageName = info.activityInfo.packageName,
                    activityName = info.activityInfo.name,
                    icon = info.loadIcon(packageManager).toBitmap(iconSize, iconSize).asImageBitmap(),
                )
            }.getOrNull()
        }
        .distinctBy { it.id }
        .sortedBy { it.label.lowercase() }
}

private fun openApp(
    context: Context,
    app: LauncherApp,
) {
    val intent =
        Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.packageName, app.activityName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching { context.startActivity(intent) }
        .onFailure { Timber.e(it, "Cannot open ${app.packageName}") }
}
