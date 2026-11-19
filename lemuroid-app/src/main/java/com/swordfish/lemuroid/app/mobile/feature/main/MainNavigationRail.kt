package com.swordfish.lemuroid.app.mobile.feature.main

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.apps.rememberAppsTabVisible
import com.swordfish.lemuroid.app.mobile.feature.games.HomeTileSize
import com.swordfish.lemuroid.app.mobile.feature.games.HomeTileSizeStore
import com.swordfish.lemuroid.app.mobile.feature.games.rememberHomeTileSize
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.focusHighlight
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

const val VERTICAL_BAR_KEY = "pref_key_vertical_bar"

/** The "Vertical bar" layout choice in Settings; follows the setting while the app is open. */
@Composable
fun rememberVerticalBar(): Boolean {
    val context = LocalContext.current
    var vertical by remember { mutableStateOf(readVerticalBar(context)) }

    DisposableEffect(Unit) {
        val preferences = SharedPreferencesHelper.getSharedPreferences(context)
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == VERTICAL_BAR_KEY) vertical = readVerticalBar(context)
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return vertical
}

private fun readVerticalBar(context: Context): Boolean =
    SharedPreferencesHelper.getSharedPreferences(context).getBoolean(VERTICAL_BAR_KEY, false)

/**
 * One bar down the left side instead of the top and bottom bars: the pages, then the shortcuts
 * that normally sit in the top bar (view options, sync, settings).
 */
@Composable
fun MainNavigationRail(
    currentRoute: MainRoute?,
    navController: NavHostController,
    onHelpPressed: () -> Unit,
    operationsInProgress: Boolean,
) {
    val context = LocalContext.current
    val appsTabVisible = rememberAppsTabVisible()

    // Leave the Apps tab if it gets hidden while open.
    LaunchedEffect(appsTabVisible, currentRoute) {
        if (!appsTabVisible && currentRoute == MainRoute.APPS) {
            navController.navigate(MainRoute.HOME.route) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxHeight()
                .width(84.dp)
                .background(BottomAppBarDefaults.containerColor)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MainNavigationRoutes.values()
            .filter { it != MainNavigationRoutes.APPS || appsTabVisible }
            .forEach { destination ->
                val isSelected = currentRoute?.root == destination.route
                NavigationRailItem(
                    modifier =
                        Modifier
                            .height(52.dp)
                            .focusHighlight(shape = RoundedCornerShape(16.dp), zoom = 1f),
                    selected = isSelected,
                    onClick = {
                        navController.navigate(destination.route.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = stringResource(destination.titleId),
                        )
                    },
                    label = { Text(stringResource(destination.titleId), fontSize = 11.sp, maxLines = 1) },
                    alwaysShowLabel = true,
                )
            }

        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        // View options of the page (the top bar holds them in the horizontal layout).
        if (currentRoute == MainRoute.HOME || currentRoute == MainRoute.FAVORITES) {
            Row {
                HomeTileSizeCycleButton()
                GameStyleButton()
            }
            Text(
                text = stringResource(R.string.rail_view),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        // Settings, without a label, to fit short screens.
        Row(horizontalArrangement = Arrangement.Center) {
            IconButton(
                modifier = Modifier.focusHighlight(shape = RoundedCornerShape(16.dp), zoom = 1f),
                onClick = { navController.navigate(MainRoute.SETTINGS.route) },
            ) {
                Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
            }
        }
    }
}

/** Small / large tiles in one button: each tap switches, the icon shows the current size. */
@Composable
private fun HomeTileSizeCycleButton() {
    val context = LocalContext.current
    val size = rememberHomeTileSize()
    val next = if (size == HomeTileSize.SMALL) HomeTileSize.LARGE else HomeTileSize.SMALL
    IconButton(
        modifier = Modifier.focusHighlight(shape = RoundedCornerShape(16.dp), zoom = 1f),
        onClick = { HomeTileSizeStore.set(context, next) },
    ) {
        Icon(
            imageVector = if (size == HomeTileSize.SMALL) Icons.Outlined.ViewModule else Icons.Outlined.GridView,
            contentDescription =
                stringResource(
                    if (size == HomeTileSize.SMALL) R.string.games_view_grid_small else R.string.games_view_grid_large,
                ),
        )
    }
}

