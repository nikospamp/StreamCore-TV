package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreProfileArtwork(
    imageUrl: String?,
    fallbackAvatar: StreamCoreProfileAvatar,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val fallbackPainter = painterResource(fallbackAvatar.drawableRes)
    AsyncImage(
        model = imageUrl?.takeIf { it.isNotBlank() },
        contentDescription = contentDescription,
        placeholder = fallbackPainter,
        error = fallbackPainter,
        fallback = fallbackPainter,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun StreamCoreProfileArtworkPreview() {
    StreamCoreTheme {
        StreamCoreProfileArtwork(
            imageUrl = null,
            fallbackAvatar = StreamCoreProfileAvatar.Avatar01,
            contentDescription = "Profile avatar",
            modifier = Modifier.size(StreamCoreDimens.Mobile.Profiles.AvatarSize),
        )
    }
}
