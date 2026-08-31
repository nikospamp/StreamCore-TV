package com.pampoukidis.streamcoretv.feature.profiles.tv.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
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
internal fun TvProfileTile(
    profile: ProfileModel,
    mode: ProfilesMode,
    isSelecting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused && enabled) 1.06f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "tv-profile-focus",
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
            .size(
                width = StreamCoreDimens.Tv.Profiles.TileWidth,
                height = StreamCoreDimens.Tv.Profiles.TileHeight,
            )
            .scale(scale)
            .semantics(mergeDescendants = true) {
                contentDescription = semanticLabel
            }
            .onFocusChanged { isFocused = it.isFocused }
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
            modifier = Modifier.size(StreamCoreDimens.Tv.Profiles.FocusContainerSize),
        ) {
            Surface(
                shape = CircleShape,
                color = if (profile.isKidsProfile) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                border = BorderStroke(
                    width = if (isFocused) StreamCoreDimens.Tv.Focus.BorderWidth else 0.dp,
                    color = if (isFocused) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.background
                    },
                ),
                tonalElevation = if (isFocused) {
                    StreamCoreDimens.Elevation.Medium
                } else {
                    0.dp
                },
                modifier = Modifier
                    .size(StreamCoreDimens.Tv.Profiles.AvatarSize)
                    .clip(CircleShape),
            ) {
                Box {
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
            }
            if (isSelecting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = StreamCoreDimens.Stroke.Progress,
                    modifier = Modifier.size(StreamCoreDimens.Tv.Profiles.SelectionProgressSize),
                )
            }
            TvProfileKidsChip(
                profileId = profile.id,
                visible = profile.isKidsProfile,
            )
            TvProfileEditBadge(visible = mode == ProfilesMode.Manage)
        }
        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BoxScope.TvProfileKidsChip(
    profileId: String,
    visible: Boolean,
) {
    if (!visible) return

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        tonalElevation = StreamCoreDimens.Elevation.Low,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(
                x = -StreamCoreDimens.Tv.Profiles.BadgeOffsetX,
                y = StreamCoreDimens.Tv.Profiles.BadgeOffsetY,
            )
            .testTag(ProfilesTestTags.KidsChipPrefix + profileId),
    ) {
        Text(
            text = "Kids",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = StreamCoreDimens.Spacing.Medium,
                vertical = StreamCoreDimens.Spacing.Tiny,
            ),
        )
    }
}

@Composable
private fun BoxScope.TvProfileEditBadge(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(
                x = -StreamCoreDimens.Tv.Profiles.BadgeOffsetX,
                y = -StreamCoreDimens.Tv.Profiles.BadgeOffsetY,
            ),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            tonalElevation = StreamCoreDimens.Elevation.Low,
            modifier = Modifier.size(StreamCoreDimens.Tv.Profiles.BadgeSize),
        ) {
            Box(contentAlignment = Alignment.Center) {
                StreamCoreEditIcon(
                    modifier = Modifier.size(StreamCoreDimens.Tv.Profiles.EditIconSize),
                )
            }
        }
    }
}

@Preview
@Composable
private fun TvProfileTilePreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface {
            TvProfileTile(
                profile = ProfilesPreviewData.profiles.first(),
                mode = ProfilesMode.Selection,
                isSelecting = false,
                enabled = true,
                onClick = {},
            )
        }
    }
}
