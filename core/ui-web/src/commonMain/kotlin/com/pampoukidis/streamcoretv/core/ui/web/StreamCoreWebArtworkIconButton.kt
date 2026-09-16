package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.components.StreamCorePlayIcon
import com.pampoukidis.streamcoretv.core.ui.extensions.onArtwork
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

/** TV artwork styling with browser-native pointer and keyboard activation. */
@Composable
fun StreamCoreWebArtworkIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    content: @Composable () -> Unit,
) {
    StreamCoreWebActionSurface(
        onClick = { if (!isLoading) onClick() },
        enabled = enabled,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
        focusedContainerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onArtwork,
        modifier = modifier.size(StreamCoreDimens.Button.MinHeight).semantics {
            this.contentDescription = contentDescription
            if (isLoading) stateDescription = "Loading"
        },
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onArtwork,
                strokeWidth = StreamCoreDimens.Stroke.Default,
                modifier = Modifier.size(StreamCoreDimens.Icon.Loading),
            )
        } else content()
    }
}

@Preview
@Composable
private fun StreamCoreWebArtworkIconButtonPreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreWebArtworkIconButton("Play", {}) { StreamCorePlayIcon() }
    }
}
