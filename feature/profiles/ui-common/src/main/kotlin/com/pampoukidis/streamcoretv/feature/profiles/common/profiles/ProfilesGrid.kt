package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
fun ProfilesGrid(
    profiles: List<ProfileModel>,
    columns: GridCells,
    pendingSelectionProfileId: String?,
    onAction: (ProfilesAction) -> Unit,
    onEditProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
    avatarSize: Dp = StreamCoreDimens.Mobile.Profiles.AvatarSize,
) {
    LazyVerticalGrid(
        columns = columns,
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = modifier,
    ) {
        items(
            items = profiles,
            key = { it.id },
            contentType = { "profile" },
        ) { profile ->
            ProfileCard(
                profile = profile,
                isSelecting = pendingSelectionProfileId == profile.id,
                onAction = onAction,
                onEditProfile = onEditProfile,
                avatarSize = avatarSize,
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: ProfileModel,
    isSelecting: Boolean,
    onAction: (ProfilesAction) -> Unit,
    onEditProfile: (String) -> Unit,
    avatarSize: Dp,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = StreamCoreDimens.Elevation.Low,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = !isSelecting) {
                onAction(ProfilesAction.SelectProfile(profile.id))
            }
            .testTag(ProfilesTestTags.ProfileCardPrefix + profile.id),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier.padding(StreamCoreDimens.Spacing.Large),
        ) {
            ProfileAvatarCircle(
                profile = profile,
                size = avatarSize,
            )
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = profile.parentalLevel.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isSelecting) {
                CircularProgressIndicator(
                    strokeWidth = StreamCoreDimens.Stroke.Default,
                    modifier = Modifier.size(StreamCoreDimens.Icon.Loading),
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StreamCoreTextButton(
                        text = "Edit",
                        onClick = { onEditProfile(profile.id) },
                        enabled = true,
                        modifier = Modifier.testTag(
                            ProfilesTestTags.EditProfileButtonPrefix + profile.id,
                        ),
                    )
                    if (profile.canDelete) {
                        StreamCoreTextButton(
                            text = "Delete",
                            onClick = {
                                onAction(ProfilesAction.RequestDeleteProfile(profile.id))
                            },
                            enabled = true,
                            modifier = Modifier.testTag(
                                ProfilesTestTags.DeleteProfileButtonPrefix + profile.id,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatarCircle(
    profile: ProfileModel,
    size: Dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (profile.isKidsProfile) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            ),
    ) {
        Text(
            text = profile.displayName.initials(),
            style = MaterialTheme.typography.headlineMedium,
            color = if (profile.isKidsProfile) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
        )
    }
}

private fun String.initials(): String {
    return trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercase() }
        .ifBlank { "?" }
}

@PreviewMobile
@Composable
private fun ProfilesGridPreview() {
    StreamCoreTheme {
        ProfilesGrid(
            profiles = ProfilesPreviewData.profiles,
            columns = GridCells.Fixed(2),
            pendingSelectionProfileId = null,
            onAction = {},
            onEditProfile = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(StreamCoreDimens.Mobile.Profiles.PreviewGridHeight)
                .padding(StreamCoreDimens.Spacing.Large),
        )
    }
}
