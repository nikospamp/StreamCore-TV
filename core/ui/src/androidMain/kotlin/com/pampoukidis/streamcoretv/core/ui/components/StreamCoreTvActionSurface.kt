package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/** A single D-pad action around shared artwork, without changing its measured bounds on focus. */
@Composable
fun StreamCoreTvActionSurface(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    role: Role = Role.Button,
    content: @Composable () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium
    val controlStyle = StreamCoreControlDefaults.style()
    val transparentSurface = MaterialTheme.colorScheme.surface.copy(alpha = 0f)

    Box(
        contentAlignment = Alignment.Center,
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = role,
                onClick = onClick,
            ),
    ) {
        content()
    }
}

@Preview
@Composable
private fun StreamCoreTvActionSurfacePreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreTvActionSurface(onClick = {}, enabled = true) {
            Text(
                text = "Change avatar",
                modifier = Modifier.padding(StreamCoreDimens.Spacing.Large),
            )
        }
    }
}
