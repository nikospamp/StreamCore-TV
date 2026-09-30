package com.pampoukidis.streamcoretv.feature.profiles.common.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData

@Composable
fun ProfilePinIdentity(profile: StreamCoreProfile, avatarSize: Dp, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier,
    ) {
        StreamCoreProfileArtwork(
            avatar = profile.avatar,
            contentDescription = null,
            modifier = Modifier.size(avatarSize).clip(CircleShape),
        )
        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
private fun ProfilePinIdentityPreview() {
    StreamCoreTheme {
        ProfilePinIdentity(ProfilesPreviewData.profiles.first(), StreamCoreDimens.Mobile.Profiles.AvatarSize)
    }
}
