package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/** Keeps the shared switch row's single toggleable target and adds the TV focus treatment. */
@Composable
fun StreamCoreTvSettingsSwitchRow(
    title: String,
    supportingText: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.medium
    val controlStyle = StreamCoreControlDefaults.style()
    val transparentSurface = MaterialTheme.colorScheme.surface.copy(alpha = 0f)

    StreamCoreSettingsSwitchRow(
        title = title,
        supportingText = supportingText,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                color = if (isFocused) MaterialTheme.colorScheme.surfaceContainerHighest else transparentSurface,
                shape = shape,
            )
            .border(
                width = StreamCoreDimens.Tv.Focus.BorderWidth,
                color = if (isFocused) controlStyle.primary else transparentSurface,
                shape = shape,
            )
            .padding(horizontal = StreamCoreDimens.Spacing.Small),
    )
}

@Preview
@Composable
private fun StreamCoreTvSettingsSwitchRowPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface {
            StreamCoreTvSettingsSwitchRow(
                title = "Kids profile",
                supportingText = "Only age-appropriate content",
                checked = true,
                enabled = true,
                onCheckedChange = {},
                modifier = Modifier.padding(StreamCoreDimens.Spacing.Large),
            )
        }
    }
}
