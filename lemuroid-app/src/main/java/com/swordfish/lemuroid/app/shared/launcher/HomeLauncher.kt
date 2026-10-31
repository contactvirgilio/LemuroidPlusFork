package com.swordfish.lemuroid.app.shared.launcher

import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import timber.log.Timber

/**
 * Lets Lemuroid Plus be the device's home screen (launcher).
 *
 * The home intent filter lives on a disabled activity-alias of MainActivity, so installing the
 * app never makes Android ask which home screen to use. It is enabled only when the user picks
 * "Use as home screen" in Settings.
 */
object HomeLauncher {
    private const val HOME_ALIAS = "com.swordfish.lemuroid.app.mobile.feature.main.HomeLauncherAlias"

    fun isDefaultHome(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    /** True when the activity was opened by the Home button (not by tapping the app icon). */
    fun isHomeIntent(intent: Intent?): Boolean {
        return intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)
    }

    /** Asks Android to make Lemuroid Plus the home screen, or opens the home app settings to change it. */
    fun openHomeScreenChooser(activity: Activity) {
        enableHomeAlias(activity)

        if (!isDefaultHome(activity) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                val started =
                    runCatching {
                        activity.startActivityForResult(
                            roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                            REQUEST_HOME_ROLE,
                        )
                    }.isSuccess
                if (started) return
            }
        }

        runCatching { activity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
            .recoverCatching { activity.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            .onFailure { Timber.e(it, "Cannot open home screen settings") }
    }

    private fun enableHomeAlias(context: Context) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context.packageName, HOME_ALIAS),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    private const val REQUEST_HOME_ROLE = 4242
}
