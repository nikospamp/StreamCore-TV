package com.pampoukidis.streamcoretv.core.ui.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreWebArtwork(
    imageUrl: String?,
    contentDescription: String?,
    fallbackText: String,
    requestWidthPx: Int,
    requestHeightPx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    require(requestWidthPx > 0)
    require(requestHeightPx > 0)
    val normalizedUrl = imageUrl?.takeIf(String::isNotBlank)
    val platformContext = LocalPlatformContext.current
    val request = remember(platformContext, normalizedUrl, requestWidthPx, requestHeightPx) {
        normalizedUrl?.let { url ->
            ImageRequest.Builder(platformContext)
                .data(url)
                .size(requestWidthPx, requestHeightPx)
                .crossfade(true)
                .build()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(containerColor),
    ) {
        Text(
            text = fallbackText,
            style = MaterialTheme.typography.displayMedium,
            color = contentColor,
        )
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
        overlay()
    }
}

@Preview
@Composable
private fun StreamCoreWebArtworkPreview() {
    StreamCoreTheme(darkTheme = true) {
        StreamCoreWebArtwork(
            imageUrl = null,
            contentDescription = "Preview artwork",
            fallbackText = "S",
            requestWidthPx = 640,
            requestHeightPx = 360,
            modifier = Modifier.size(
                width = StreamCoreDimens.Web.Artwork.FallbackWidth,
                height = StreamCoreDimens.Web.Artwork.FallbackHeight,
            ),
        )
    }
}
