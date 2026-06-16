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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile

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
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val normalizedImageUrl = imageUrl?.takeIf { url -> url.isNotBlank() }
    val context = LocalContext.current
    val imageRequest = remember(
        context,
        normalizedImageUrl,
        crossfade,
    ) {
        normalizedImageUrl?.let { url ->
            ImageRequest.Builder(context)
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
                modifier = Modifier.fillMaxSize(),
            )
        }
        overlay()
    }
}

@PreviewMobile
@Composable
private fun StreamCoreContentImagePreview() {
    StreamCoreTVTheme {
        Row {
            StreamCoreContentImage(
                imageUrl = null,
                contentDescription = "Preview",
                fallbackText = "S",
                modifier = Modifier.size(160.dp),
            )
        }
    }
}