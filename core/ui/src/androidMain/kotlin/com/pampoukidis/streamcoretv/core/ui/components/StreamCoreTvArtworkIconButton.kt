package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Border
import androidx.tv.material3.IconButton
import androidx.tv.material3.IconButtonDefaults
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreControlDefaults
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import androidx.compose.material3.LocalContentColor as ComposeLocalContentColor
import androidx.tv.material3.LocalContentColor as TvLocalContentColor

/** Circular artwork action with TV-owned target size and an unclipped outer focus ring. */
@Composable
fun StreamCoreTvArtworkIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    content: @Composable () -> Unit,
) {
    val style = StreamCoreControlDefaults.style()
    val shape = CircleShape
    val container = MaterialTheme.colorScheme.scrim.copy(alpha = ArtworkButtonContainerAlpha)
    val foreground = MaterialTheme.colorScheme.onArtwork
    val focusBorder = Border(
        border = BorderStroke(StreamCoreDimens.Tv.Focus.BorderWidth, style.primary),
        shape = shape,
    )
    IconButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .size(StreamCoreDimens.Button.MinHeight)
            .semantics {
                this.contentDescription = contentDescription
                if (isLoading) stateDescription = "Loading"
            },
        shape = streamCoreTvButtonShape(shape),
        scale = IconButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        colors = IconButtonDefaults.colors(
            containerColor = container,
            contentColor = foreground,
            focusedContainerColor = container,
            focusedContentColor = foreground,
            pressedContainerColor = style.pressed,
            pressedContentColor = foreground,
            disabledContainerColor = container,
            disabledContentColor = foreground,
        ),
        border = IconButtonDefaults.border(
            focusedBorder = focusBorder,
            pressedBorder = focusBorder,
            focusedDisabledBorder = focusBorder,
        ),
    ) {
        CompositionLocalProvider(ComposeLocalContentColor provides TvLocalContentColor.current) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = foreground,
                    strokeWidth = StreamCoreDimens.Stroke.Default,
                    modifier = Modifier.size(StreamCoreDimens.Icon.Loading),
                )
            } else {
                Box(modifier = Modifier.alpha(if (enabled) 1f else DisabledContentAlpha)) {
                    content()
                }
            }
        }
    }
}

private const val ArtworkButtonContainerAlpha = 0.6f
private const val DisabledContentAlpha = 0.38f

@Preview
@Composable
private fun StreamCoreTvArtworkIconButtonPreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreTvArtworkIconButton(contentDescription = "Play", onClick = {}) {
            StreamCorePlayIcon(modifier = Modifier.size(StreamCoreDimens.Icon.Standard))
        }
    }
}
