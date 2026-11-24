package com.swordfish.lemuroid.app.mobile.feature.settings.general

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.SelectionStyle
import com.swordfish.lemuroid.R

/** Settings row to choose the colour that shows what is selected (pink by default). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectionColorPicker() {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(text = stringResource(R.string.settings_title_selection_color), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.settings_description_selection_color),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelectionStyle.options.forEach { (name, color) ->
                val selected = SelectionStyle.name == name
                androidx.compose.foundation.layout.Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(0.dp, Color.Transparent),
                                CircleShape,
                            )
                            .clickable(role = Role.RadioButton) { SelectionStyle.set(context, name) },
                )
            }
        }
    }
}
