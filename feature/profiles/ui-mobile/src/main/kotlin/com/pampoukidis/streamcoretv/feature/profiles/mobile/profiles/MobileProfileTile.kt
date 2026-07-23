package com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreEditIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.motion.streamCoreSharedBounds
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
internal fun MobileProfileTile(
    profile: ProfileModel,
    mode: ProfilesMode,
    isSelecting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val avatarScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(),
        label = "profile-avatar-press",
    )
    val semanticLabel = if (mode == ProfilesMode.Manage) {
        "Edit ${profile.displayName} profile"
    } else {
        "${profile.displayName}, ${if (profile.isKidsProfile) "kids" else "unrestricted"} profile"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minWidth = 136.dp, minHeight = 148.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = semanticLabel
            }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .testTag(ProfilesTestTags.ProfileCardPrefix + profile.id),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(StreamCoreDimens.Mobile.Profiles.AvatarSize),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .scale(avatarScale)
                    .clip(CircleShape)
                    .background(
                        if (profile.isKidsProfile) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                    )
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true),
                    ),
            ) {
                StreamCoreProfileArtwork(
                    avatar = profile.avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .streamCoreSharedBounds(
                            sharedElementScope = sharedElementScope,
                            key = StreamCoreSharedKey.profileAvatar(profile.id),
                            clipShape = CircleShape,
                        ),
                )
                if (isSelecting) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f)),
                    )
                }
            }
            if (isSelecting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = StreamCoreDimens.Stroke.Progress,
                    modifier = Modifier.size(28.dp),
                )
            }
            ProfileKidsChip(
                profileId = profile.id,
                visible = profile.isKidsProfile,
            )
            ProfileEditBadge(visible = mode == ProfilesMode.Manage)
        }
        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BoxScope.ProfileKidsChip(
    profileId: String,
    visible: Boolean,
) {
    if (!visible) return

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shadowElevation = StreamCoreDimens.Elevation.Low,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 8.dp, y = (-4).dp)
            .testTag(ProfilesTestTags.KidsChipPrefix + profileId),
    ) {
        Text(
            text = "Kids",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = StreamCoreDimens.Spacing.Small,
                vertical = StreamCoreDimens.Spacing.Tiny,
            ),
        )
    }
}

@Composable
private fun BoxScope.ProfileEditBadge(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(x = 2.dp, y = 2.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = StreamCoreDimens.Elevation.Low,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                StreamCoreEditIcon(modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Preview
@Composable
private fun MobileProfileTilePreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface {
            MobileProfileTile(
                profile = ProfilesPreviewData.profiles.first(),
                mode = ProfilesMode.Selection,
                isSelecting = false,
                enabled = true,
                onClick = {},
            )
        }
    }
}
