package com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedElementScope
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
fun MobileProfilesScreen(
    state: ProfilesUiState,
    onAction: (ProfilesAction) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
    sharedElementScope: StreamCoreSharedElementScope? = null,
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
        Box(modifier = Modifier.fillMaxSize()) {
            MobileProfilesBackdrop(modifier = Modifier.matchParentSize())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                ProfilesTopBar(
                    mode = state.mode,
                    showAction = !state.isLoading &&
                            state.loadError == null &&
                            state.profiles.isNotEmpty(),
                    actionEnabled = interactionsEnabled,
                    onAction = onAction,
                )
                Spacer(modifier = Modifier.height(StreamCoreDimens.Spacing.Large))
                when {
                    state.isLoading -> ProfilesLoadingGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding),
                    )

                    state.loadError != null -> ProfilesLoadError(
                        onRetry = { onAction(ProfilesAction.Refresh) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> MobileProfilesGrid(
                        profiles = state.profiles,
                        mode = state.mode,
                        pendingSelectionProfileId = state.pendingSelectionProfileId,
                        interactionsEnabled = interactionsEnabled,
                        onSelectProfile = { profileId ->
                            onAction(ProfilesAction.SelectProfile(profileId))
                        },
                        onCreateProfile = onCreateProfile,
                        onEditProfile = onEditProfile,
                        sharedElementScope = sharedElementScope,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilesTopBar(
    mode: ProfilesMode,
    showAction: Boolean,
    actionEnabled: Boolean,
    onAction: (ProfilesAction) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = StreamCoreDimens.Mobile.Screen.HorizontalPadding),
    ) {
        Text(
            text = if (mode == ProfilesMode.Selection) {
                "Who's watching?"
            } else {
                "Manage profiles"
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 76.dp),
        )
        if (showAction) {
            StreamCoreTextButton(
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
                contentColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .testTag(ProfilesTestTags.ManageProfilesButton),
            )
        }
    }
}

@Composable
private fun ProfilesLoadingGrid(
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            top = StreamCoreDimens.Spacing.Small,
            bottom = 32.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        userScrollEnabled = false,
        modifier = modifier,
    ) {
        items(
            items = List(4) { it },
            key = { it },
            contentType = { "profile-placeholder" },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(StreamCoreDimens.Mobile.Profiles.AvatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        }
    }
}

@Composable
private fun ProfilesLoadError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = modifier.padding(StreamCoreDimens.Mobile.Screen.HorizontalPadding),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "We couldn't load profiles.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        StreamCoreButton(
            text = "Try again",
            onClick = onRetry,
            enabled = true,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun previewProfiles(count: Int): List<ProfileModel> {
    return List(count) { index ->
        val source = ProfilesPreviewData.profiles[index % ProfilesPreviewData.profiles.size]
        source.copy(
            id = "${source.id}-$index",
            displayName = if (index < ProfilesPreviewData.profiles.size) {
                source.displayName
            } else {
                "Profile ${index + 1}"
            },
        )
    }
}

@PreviewMobile
@Composable
private fun MobileProfilesScreenDarkPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileProfilesScreen(
            state = ProfilesUiState(isLoading = false, profiles = previewProfiles(2)),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileProfilesScreenLightPreview() {
    StreamCoreTheme(darkTheme = false) {
        MobileProfilesScreen(
            state = ProfilesUiState(isLoading = false, profiles = previewProfiles(2)),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileProfilesScreenFiveProfilesPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileProfilesScreen(
            state = ProfilesUiState(isLoading = false, profiles = previewProfiles(5)),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileProfilesScreenEmptyPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileProfilesScreen(
            state = ProfilesUiState(isLoading = false),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileProfilesScreenLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileProfilesScreen(
            state = ProfilesUiState(),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileProfilesScreenPendingSelectionPreview() {
    val profiles = previewProfiles(2)
    StreamCoreTheme(darkTheme = true) {
        MobileProfilesScreen(
            state = ProfilesUiState(
                isLoading = false,
                profiles = profiles,
                pendingSelectionProfileId = profiles.first().id,
            ),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileProfilesScreenManagePreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileProfilesScreen(
            state = ProfilesUiState(
                isLoading = false,
                profiles = previewProfiles(3),
                mode = ProfilesMode.Manage,
            ),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileProfilesScreenLargeTextPreview() {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = 1.5f,
        ),
    ) {
        StreamCoreTheme(darkTheme = true) {
            MobileProfilesScreen(
                state = ProfilesUiState(
                    isLoading = false,
                    profiles = previewProfiles(2).mapIndexed { index, profile ->
                        profile.copy(displayName = "A very long profile name ${index + 1}")
                    },
                ),
                onAction = {},
                onCreateProfile = {},
                onEditProfile = {},
            )
        }
    }
}
