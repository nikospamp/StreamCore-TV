package com.pampoukidis.streamcoretv.feature.profiles.common.presentation

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfilesAction
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
    avatarSize: Dp = 72.dp,
) {
    LazyVerticalGrid(
        columns = columns,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
fun ProfilesDeleteConfirmationDialog(
    profile: ProfileModel?,
    isSaving: Boolean,
    onAction: (ProfilesAction) -> Unit,
) {
    if (profile == null) return

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onAction(ProfilesAction.DismissDeleteConfirmation)
        },
        title = {
            Text(text = "Delete profile")
        },
        text = {
            Text(text = "Delete ${profile.displayName}? This cannot be undone.")
        },
        confirmButton = {
            StreamCoreButton(
                text = "Delete",
                onClick = { onAction(ProfilesAction.ConfirmDeleteProfile) },
                enabled = !isSaving,
                loading = isSaving,
            )
        },
        dismissButton = {
            StreamCoreTextButton(
                text = "Cancel",
                onClick = { onAction(ProfilesAction.DismissDeleteConfirmation) },
                enabled = !isSaving,
            )
        },
    )
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
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !isSelecting) {
                onAction(ProfilesAction.SelectProfile(profile.id))
            }
            .testTag(ProfilesTestTags.ProfileCardPrefix + profile.id),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp),
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
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
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
    StreamCoreTVTheme {
        ProfilesGrid(
            profiles = ProfilesPreviewData.profiles,
            columns = GridCells.Fixed(2),
            pendingSelectionProfileId = null,
            onAction = {},
            onEditProfile = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .padding(16.dp),
        )
    }
}