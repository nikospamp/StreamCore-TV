package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import kotlinx.coroutines.launch

/** Browser pointer/keyboard wrapper around portable content. One shape owns every painted layer. */
@Composable
fun StreamCoreWebActionSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    role: Role = Role.Button,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
    focusedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val relocation = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Surface(
        shape = shape,
        color = when {
            focused || hovered -> focusedContainerColor
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> containerColor
        },
        contentColor = contentColor.copy(alpha = if (enabled) contentColor.alpha else contentColor.alpha * 0.38f),
        border = if (focused || hovered) BorderStroke(
            StreamCoreDimens.Tv.Focus.BorderWidth,
            MaterialTheme.colorScheme.primary.copy(alpha = if (focused) 1f else 0.55f),
        ) else null,
        modifier = modifier
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = role,
                onClick = onClick,
            )
            .hoverable(interaction, enabled)
            .bringIntoViewRequester(relocation)
            .onFocusChanged { if (it.isFocused) scope.launch { relocation.bringIntoView() } }
            .onPreviewKeyEvent { event ->
                if (enabled && event.key == Key.Spacebar) {
                    if (event.type == KeyEventType.KeyUp) onClick()
                    true
                } else false
            }
            .semantics { this.role = role },
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Preview
@Composable
private fun StreamCoreWebActionSurfacePreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreWebActionSurface(onClick = {}) {
            Text("Quality", Modifier.padding(StreamCoreDimens.Spacing.Large))
        }
    }
}
