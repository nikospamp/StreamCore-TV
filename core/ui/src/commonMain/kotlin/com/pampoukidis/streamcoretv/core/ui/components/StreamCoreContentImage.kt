package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun StreamCoreContentImage(
    imageUrl: String?,
    contentDescription: String?,
    fallbackText: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fallbackTextStyle: TextStyle = MaterialTheme.typography.displayMedium,
    crossfade: Boolean = true,
    alignment: Alignment = Alignment.Center,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val normalizedImageUrl = imageUrl?.takeIf { url -> url.isNotBlank() }
    val platformContext = LocalPlatformContext.current
    val imageRequest = remember(
        platformContext,
        normalizedImageUrl,
        crossfade,
    ) {
        normalizedImageUrl?.let { url ->
            ImageRequest.Builder(platformContext)
                .data(url)
                .crossfade(crossfade)
                .build()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(containerColor),
    ) {
        Text(
            text = fallbackText,
            style = fallbackTextStyle,
            color = contentColor,
        )
        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                modifier = Modifier.fillMaxSize(),
            )
        }
        overlay()
    }
}

@Preview
@Composable
private fun StreamCoreContentImagePreview() {
    StreamCoreTheme {
        Row {
            StreamCoreContentImage(
                imageUrl = null,
                contentDescription = "Preview",
                fallbackText = "S",
                modifier = Modifier.size(StreamCoreDimens.Icon.ArtworkFallback),
            )
        }
    }
}
