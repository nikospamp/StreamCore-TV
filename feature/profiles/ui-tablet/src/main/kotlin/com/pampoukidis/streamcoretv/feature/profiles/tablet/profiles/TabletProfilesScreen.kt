package com.pampoukidis.streamcoretv.feature.profiles.tablet.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.AndroidProfilesBackdrop
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesDeleteConfirmationDialog
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.TouchAddProfileTile
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.TouchProfileTile
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.TouchProfilesTopBar
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
fun TabletProfilesScreen(
    state: ProfilesUiState,
    onAction: (ProfilesAction) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    isLogoutInProgress: Boolean = false,
    onLogoutRequested: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val interactionsEnabled = !state.isLoading &&
        !state.isSaving &&
        !isLogoutInProgress &&
        state.pendingSelectionProfileId == null

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(ProfilesTestTags.Root),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidProfilesBackdrop(modifier = Modifier.matchParentSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                TouchProfilesTopBar(
                    mode = state.mode,
                    showManageAction = !state.isLoading &&
                        state.loadError == null &&
                        state.profiles.isNotEmpty(),
                    showLogoutAction = state.mode == ProfilesMode.Selection,
                    profileActionEnabled = interactionsEnabled,
                    logoutEnabled = !isLogoutInProgress,
                    onLogoutRequested = onLogoutRequested,
                    onAction = onAction,
                    horizontalPadding = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                    height = StreamCoreDimens.Tablet.Profiles.HeaderHeight,
                    sideClearance = StreamCoreDimens.Tablet.Profiles.HeaderSideClearance,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    when {
                        state.isLoading -> TabletProfilesLoadingRow()
                        state.loadError != null -> TabletProfilesLoadError(
                            onRetry = { onAction(ProfilesAction.Refresh) },
                        )
                        else -> TabletProfilesRow(
                            state = state,
                            interactionsEnabled = interactionsEnabled,
                            onAction = onAction,
                            onCreateProfile = onCreateProfile,
                            onEditProfile = onEditProfile,
                        )
                    }
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
private fun TabletProfilesRow(
    state: ProfilesUiState,
    interactionsEnabled: Boolean,
    onAction: (ProfilesAction) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(
            StreamCoreDimens.Spacing.ExtraLarge,
            Alignment.CenterHorizontally,
        ),
        contentPadding = PaddingValues(
            horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
            vertical = StreamCoreDimens.Spacing.Large,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(
            items = state.profiles,
            key = { profile -> profile.id },
            contentType = { "profile" },
        ) { profile ->
            TouchProfileTile(
                profile = profile,
                mode = state.mode,
                isSelecting = state.pendingSelectionProfileId == profile.id,
                enabled = interactionsEnabled,
                onClick = {
                    if (state.mode == ProfilesMode.Selection) {
                        onAction(ProfilesAction.SelectProfile(profile.id))
                    } else {
                        onEditProfile(profile.id)
                    }
                },
                modifier = Modifier.width(StreamCoreDimens.Tablet.Profiles.TileWidth),
            )
        }
        item(key = "add-profile", contentType = "add-profile") {
            TouchAddProfileTile(
                enabled = interactionsEnabled,
                onClick = onCreateProfile,
                modifier = Modifier.width(StreamCoreDimens.Tablet.Profiles.TileWidth),
            )
        }
    }
}

@Composable
private fun TabletProfilesLoadingRow() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(
            StreamCoreDimens.Spacing.ExtraLarge,
            Alignment.CenterHorizontally,
        ),
        contentPadding = PaddingValues(StreamCoreDimens.Tablet.Screen.HorizontalPadding),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(count = 3, key = { it }, contentType = { "profile-placeholder" }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                modifier = Modifier
                    .width(StreamCoreDimens.Tablet.Profiles.TileWidth)
                    .height(StreamCoreDimens.Mobile.Profiles.TileHeight),
            ) {
                Box(
                    modifier = Modifier
                        .size(StreamCoreDimens.Mobile.Profiles.AvatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Box(
                    modifier = Modifier
                        .size(
                            width = StreamCoreDimens.Mobile.Profiles.LoadingLabelWidth,
                            height = StreamCoreDimens.Mobile.Profiles.LoadingLabelHeight,
                        )
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        }
    }
}

@Composable
private fun TabletProfilesLoadError(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = Modifier.padding(StreamCoreDimens.Tablet.Screen.HorizontalPadding),
    ) {
        Text(
            text = "We couldn't load profiles.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        StreamCoreButton(text = "Try again", onClick = onRetry, enabled = true)
    }
}

@PreviewTablet
@Composable
private fun TabletProfilesScreenPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletProfilesScreen(
            state = ProfilesUiState(isLoading = false, profiles = ProfilesPreviewData.profiles),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
        )
    }
}

@PreviewTablet
@Composable
private fun TabletProfilesScreenManagePreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletProfilesScreen(
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

@PreviewTablet
@Composable
private fun TabletProfilesScreenEmptyPreview() {
    StreamCoreTheme(darkTheme = true) {
        TabletProfilesScreen(
            state = ProfilesUiState(isLoading = false),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
        )
    }
}
