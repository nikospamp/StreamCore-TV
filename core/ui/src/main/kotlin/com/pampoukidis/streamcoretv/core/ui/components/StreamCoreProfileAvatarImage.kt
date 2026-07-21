package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme

@Composable
fun StreamCoreProfileAvatarImage(
    avatar: StreamCoreProfileAvatar,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(avatar.drawableRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun StreamCoreProfileAvatarImagePreview() {
    StreamCoreTheme {
        StreamCoreProfileAvatarImage(
            avatar = StreamCoreProfileAvatar.Avatar01,
            contentDescription = "Profile avatar",
            modifier = Modifier.size(StreamCoreDimens.Mobile.Profiles.AvatarSize),
        )
    }
}
