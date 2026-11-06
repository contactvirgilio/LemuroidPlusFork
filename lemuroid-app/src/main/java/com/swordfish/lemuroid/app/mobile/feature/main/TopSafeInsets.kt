package com.swordfish.lemuroid.app.mobile.feature.main

import android.os.Build
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Top and side space the main screens keep free. The status bar is hidden on the main screens, so on a
 * phone with a camera cutout the top bar would slide under the camera. When the display has a cutout
 * we keep the status bar's height as a safe margin even while it is hidden; screens without a cutout
 * (like the Retroid Pocket) get no extra space.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun topSafeInsets(): WindowInsets {
    val view = LocalView.current
    val hasCutout =
        remember(view) {
            val displayCutout = if (Build.VERSION.SDK_INT >= 31) view.display?.cutout else null
            displayCutout != null || (Build.VERSION.SDK_INT >= 23 && view.rootWindowInsets?.displayCutout != null)
        }
    val base =
        WindowInsets.systemBars
            .union(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
    return if (hasCutout) {
        base.union(WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top))
    } else {
        base
    }
}
