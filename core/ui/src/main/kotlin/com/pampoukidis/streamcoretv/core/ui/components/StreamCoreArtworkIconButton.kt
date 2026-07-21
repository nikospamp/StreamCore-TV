package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.scrim.copy(alpha = ArtworkButtonContainerAlpha),
        contentColor = MaterialTheme.colorScheme.onArtwork,
        modifier = modifier
            .size(StreamCoreDimens.Icon.TouchTarget)
            .semantics {
                this.contentDescription = contentDescription
                if (isLoading) {
                    stateDescription = "Loading"
                }
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = LocalContentColor.current,
                    strokeWidth = StreamCoreDimens.Stroke.Default,
                    modifier = Modifier.size(StreamCoreDimens.Icon.Loading),
                )
            } else {
                content()
            }
        }
    }
}

private const val ArtworkButtonContainerAlpha = 0.6f

@Preview
@Composable
private fun StreamCoreIconButtonPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface(
            color = MaterialTheme.colorScheme.error
        ) {
            StreamCoreIconButton(
                contentDescription = "Information",
                onClick = {},
            ) {
                StreamCoreInfoIcon()
            }
        }
    }
}
