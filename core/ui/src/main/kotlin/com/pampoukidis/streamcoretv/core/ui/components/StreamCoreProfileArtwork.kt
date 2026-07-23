package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.ui.avatar.LocalProfileAvatarArtworkResolver
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreProfileArtwork(
    avatar: ProfileAvatarModel,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val artworkResolver = LocalProfileAvatarArtworkResolver.current
    val fallbackDrawableRes = remember(artworkResolver, avatar.id) {
        artworkResolver.resolve(avatar.id)
    }
    val fallbackPainter = fallbackDrawableRes?.let { drawableRes ->
        painterResource(drawableRes)
    } ?: ColorPainter(MaterialTheme.colorScheme.primaryContainer)

    if (avatar.imageUrl.isNullOrBlank()) {
        Image(
            painter = fallbackPainter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        return
    }

    AsyncImage(
        model = avatar.imageUrl,
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
            avatar = ProfileAvatarModel(
                id = "preview-avatar",
                imageUrl = null,
            ),
            contentDescription = "Profile avatar",
            modifier = Modifier.size(StreamCoreDimens.Mobile.Profiles.AvatarSize),
        )
    }
}
