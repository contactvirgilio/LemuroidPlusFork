package com.swordfish.lemuroid.app.mobile.feature.apps

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.app.shared.launcher.HomeLauncher
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/**
 * When the Apps tab is shown and which apps it hides.
 *
 * Mode "auto" (default) shows the tab only while Lemuroid Plus is the home screen; "on" and "off"
 * force it either way.
 */
object AppsTabSettings {
    const val MODE_KEY = "pref_key_apps_tab_mode"
    const val MODE_AUTO = "auto"
    const val MODE_ON = "on"
    const val MODE_OFF = "off"
    val MODES = listOf(MODE_AUTO, MODE_ON, MODE_OFF)

    private const val HIDDEN_KEY = "pref_key_apps_tab_hidden"

    private fun preferences(context: Context): SharedPreferences = SharedPreferencesHelper.getSharedPreferences(context)

    fun mode(context: Context): String = preferences(context).getString(MODE_KEY, MODE_AUTO) ?: MODE_AUTO

    fun isTabVisible(context: Context): Boolean {
        return when (mode(context)) {
            MODE_ON -> true
            MODE_OFF -> false
            else -> HomeLauncher.isDefaultHome(context)
        }
    }

    fun hiddenApps(context: Context): Set<String> {
        return preferences(context).getStringSet(HIDDEN_KEY, emptySet())?.toSet() ?: emptySet()
    }

    fun setHidden(
        context: Context,
        app: LauncherApp,
        hidden: Boolean,
    ) {
        val current = hiddenApps(context).toMutableSet()
        if (hidden) current.add(app.id) else current.remove(app.id)
        preferences(context).edit().putStringSet(HIDDEN_KEY, current).apply()
    }
}

/** Whether the Apps tab should be in the bottom bar; follows the setting and the home screen choice. */
@Composable
fun rememberAppsTabVisible(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var visible by remember { mutableStateOf(AppsTabSettings.isTabVisible(context)) }

    DisposableEffect(lifecycleOwner) {
        val preferences = SharedPreferencesHelper.getSharedPreferences(context)
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == AppsTabSettings.MODE_KEY) visible = AppsTabSettings.isTabVisible(context)
            }
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) visible = AppsTabSettings.isTabVisible(context)
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return visible
}
