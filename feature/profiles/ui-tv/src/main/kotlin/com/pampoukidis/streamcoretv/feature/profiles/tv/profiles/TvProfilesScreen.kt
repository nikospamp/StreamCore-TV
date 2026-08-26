package com.pampoukidis.streamcoretv.feature.profiles.tv.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesDeleteConfirmationDialog
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
fun TvProfilesScreen(
    state: ProfilesUiState,
    onAction: (ProfilesAction) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionsEnabled = !state.isLoading &&
            !state.isSaving &&
            state.pendingSelectionProfileId == null

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(ProfilesTestTags.Root),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                    vertical = StreamCoreDimens.Tv.Screen.VerticalPadding,
                ),
        ) {
            ProfilesHeader(
                mode = state.mode,
                showAction = !state.isLoading &&
                        state.loadError == null &&
                        state.profiles.isNotEmpty(),
                actionEnabled = interactionsEnabled,
                onAction = onAction,
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    state.isLoading -> TvProfilesLoadingRow()
                    state.loadError != null -> TvProfilesLoadError(
                        onRetry = { onAction(ProfilesAction.Refresh) },
                    )

                    else -> TvProfilesRow(
                        profiles = state.profiles,
                        mode = state.mode,
                        pendingSelectionProfileId = state.pendingSelectionProfileId,
                        interactionsEnabled = interactionsEnabled,
                        onSelectProfile = { profileId ->
                            onAction(ProfilesAction.SelectProfile(profileId))
                        },
                        onCreateProfile = onCreateProfile,
                        onEditProfile = onEditProfile,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    ProfilesDeleteConfirmationDialog(
        profile = state.pendingDeleteProfile,
        isSaving = state.isSaving,
        onConfirmDelete = { onAction(ProfilesAction.ConfirmDeleteProfile) },
        onDismiss = { onAction(ProfilesAction.DismissDeleteConfirmation) },
    )
}

@Composable
private fun ProfilesHeader(
    mode: ProfilesMode,
    showAction: Boolean,
    actionEnabled: Boolean,
    onAction: (ProfilesAction) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        Text(
            text = if (mode == ProfilesMode.Selection) {
                "Who's watching?"
            } else {
                "Manage profiles"
            },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 160.dp),
        )
        if (showAction) {
            StreamCoreTvButton(
                text = if (mode == ProfilesMode.Selection) "Manage" else "Done",
                onClick = {
                    onAction(
                        if (mode == ProfilesMode.Selection) {
                            ProfilesAction.ManageProfiles
                        } else {
                            ProfilesAction.DoneManaging
                        },
                    )
                },
                enabled = actionEnabled,
                variant = StreamCoreTvButtonVariant.Tertiary,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .testTag(ProfilesTestTags.ManageProfilesButton),
            )
        }
    }
}

@Composable
private fun TvProfilesLoadingRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                modifier = Modifier.size(
                    width = StreamCoreDimens.Tv.Profiles.TileWidth,
                    height = 176.dp,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(StreamCoreDimens.Tv.Profiles.AvatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Box(
                    modifier = Modifier
                        .size(width = 88.dp, height = 14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        }
    }
}

@Composable
private fun TvProfilesLoadError(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
    ) {
        Text(
            text = "We couldn't load profiles.",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        StreamCoreTvButton(
            text = "Try again",
            onClick = onRetry,
            enabled = true,
            variant = StreamCoreTvButtonVariant.Primary,
        )
    }
}

@PreviewTV
@Composable
private fun TvProfilesScreenPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvProfilesScreen(
            state = ProfilesUiState(
                isLoading = false,
                profiles = ProfilesPreviewData.profiles,
            ),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvProfilesManageScreenPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvProfilesScreen(
            state = ProfilesUiState(
                isLoading = false,
                profiles = ProfilesPreviewData.profiles,
                mode = ProfilesMode.Manage,
            ),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
        )
    }
}
