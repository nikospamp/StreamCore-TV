package com.pampoukidis.streamcoretv.feature.profiles.web.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebProfileCard
import com.pampoukidis.streamcoretv.core.ui.web.webEscape
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
fun WebProfilesScreen(
    state: ProfilesUiState,
    onAction: (ProfilesAction) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val manageFocus = remember { FocusRequester() }
    val addFocus = remember { FocusRequester() }
    val profileFocus = remember(state.profiles.map(ProfileModel::id)) {
        state.profiles.associate { it.id to FocusRequester() }
    }
    val firstFocus = state.profiles.firstOrNull()?.let { profileFocus[it.id] } ?: addFocus
    var deleteRestoreProfileId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.isLoading, state.loadError, state.profiles.size) {
        if (!state.isLoading && state.loadError == null) {
            androidx.compose.runtime.withFrameNanos { }
            firstFocus.requestFocus()
        }
    }

    LaunchedEffect(state.pendingDeleteProfile?.id) {
        val pendingProfileId = state.pendingDeleteProfile?.id
        if (pendingProfileId != null) {
            deleteRestoreProfileId = pendingProfileId
        } else {
            deleteRestoreProfileId?.let { profileId -> profileFocus[profileId]?.requestFocus() }
            deleteRestoreProfileId = null
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .webEscape {
                if (state.mode == ProfilesMode.Manage) {
                    onAction(ProfilesAction.DoneManaging)
                } else {
                    onBack()
                }
            }
            .testTag(ProfilesTestTags.Root),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wideLayout = maxWidth >= StreamCoreWebDimens.WideViewportThreshold
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
                modifier = Modifier
                    .then(
                        if (wideLayout) {
                            Modifier
                                .align(Alignment.TopCenter)
                                .widthIn(max = StreamCoreWebDimens.ProfilesContentMaxWidth)
                                .fillMaxWidth()
                        } else {
                            Modifier.fillMaxWidth()
                        },
                    )
                    .padding(
                        horizontal = StreamCoreWebDimens.ScreenHorizontal,
                        vertical = StreamCoreWebDimens.ScreenVertical,
                    ),
            ) {
            ProfilesHeader(
                mode = state.mode,
                hasProfiles = state.profiles.isNotEmpty(),
                manageFocus = manageFocus,
                firstFocus = firstFocus,
                onAction = onAction,
            )
            when {
                state.isLoading -> ProfilesLoading()
                state.loadError != null -> ProfilesError(onRetry = { onAction(ProfilesAction.Refresh) })
                else -> ProfilesContent(
                    state = state,
                    profileFocus = profileFocus,
                    addFocus = addFocus,
                    manageFocus = manageFocus,
                    onAction = onAction,
                    onCreateProfile = onCreateProfile,
                    onEditProfile = onEditProfile,
                )
            }
            }
        }
    }

    state.pendingDeleteProfile?.let { profile ->
        WebDeleteProfileDialog(
            profile = profile,
            isSaving = state.isSaving,
            onConfirm = { onAction(ProfilesAction.ConfirmDeleteProfile) },
            onDismiss = { onAction(ProfilesAction.DismissDeleteConfirmation) },
        )
    }
}

@Composable
private fun ProfilesHeader(
    mode: ProfilesMode,
    hasProfiles: Boolean,
    manageFocus: FocusRequester,
    firstFocus: FocusRequester,
    onAction: (ProfilesAction) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
            Text(
                text = if (mode == ProfilesMode.Selection) "Who's watching?" else "Manage profiles",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (mode == ProfilesMode.Selection) {
                    "Choose a profile to continue."
                } else {
                    "Select a profile to edit, or remove an eligible profile."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasProfiles) {
            StreamCoreWebButton(
                text = if (mode == ProfilesMode.Selection) "Manage profiles" else "Done",
                onClick = {
                    onAction(
                        if (mode == ProfilesMode.Selection) {
                            ProfilesAction.ManageProfiles
                        } else {
                            ProfilesAction.DoneManaging
                        },
                    )
                },
                variant = StreamCoreWebButtonVariant.Secondary,
                modifier = Modifier
                    .focusRequester(manageFocus)
                    .focusProperties { down = firstFocus }
                    .testTag(ProfilesTestTags.ManageProfilesButton),
            )
        }
    }
}

@Composable
private fun ProfilesContent(
    state: ProfilesUiState,
    profileFocus: Map<String, FocusRequester>,
    addFocus: FocusRequester,
    manageFocus: FocusRequester,
    onAction: (ProfilesAction) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
) {
    if (state.profiles.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
            modifier = Modifier.fillMaxSize(),
        ) {
            Text("No profiles yet", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Create a profile to personalize your viewing experience.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StreamCoreWebButton(
                text = "Create profile",
                onClick = onCreateProfile,
                modifier = Modifier
                    .focusRequester(addFocus)
                    .testTag(ProfilesTestTags.AddProfileButton),
            )
        }
        return
    }

    val profileIds = state.profiles.map(ProfileModel::id)
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsIndexed(
            items = state.profiles,
            key = { _, profile -> profile.id },
            contentType = { _, _ -> "web-profile" },
        ) { index, profile ->
            val requester = requireNotNull(profileFocus[profile.id])
            val previous = profileIds.getOrNull(index - 1)?.let(profileFocus::get)
            val next = profileIds.getOrNull(index + 1)?.let(profileFocus::get) ?: addFocus
            WebProfileTile(
                profile = profile,
                mode = state.mode,
                selecting = state.pendingSelectionProfileId == profile.id,
                enabled = !state.isSaving && state.pendingSelectionProfileId == null,
                onClick = {
                    if (state.mode == ProfilesMode.Selection) {
                        onAction(ProfilesAction.SelectProfile(profile.id))
                    } else {
                        onEditProfile(profile.id)
                    }
                },
                onDelete = { onAction(ProfilesAction.RequestDeleteProfile(profile.id)) },
                modifier = Modifier
                    .focusRequester(requester)
                    .focusProperties {
                        up = manageFocus
                        if (previous != null) left = previous
                        right = next
                    }
                    .onPreviewKeyEvent { event ->
                        when {
                            event.type != KeyEventType.KeyDown -> false
                            event.key == Key.DirectionLeft && previous != null -> {
                                previous.requestFocus()
                                true
                            }
                            event.key == Key.DirectionRight -> {
                                next.requestFocus()
                                true
                            }
                            event.key == Key.DirectionUp -> {
                                manageFocus.requestFocus()
                                true
                            }
                            else -> false
                        }
                    }
                    .testTag(ProfilesTestTags.ProfileCardPrefix + profile.id),
            )
        }
        item(key = "add-profile", contentType = "web-add-profile") {
            WebAddProfileTile(
                onClick = onCreateProfile,
                modifier = Modifier
                    .focusRequester(addFocus)
                    .focusProperties {
                        up = manageFocus
                        left = profileFocus[profileIds.last()] ?: FocusRequester.Default
                    }
                    .testTag(ProfilesTestTags.AddProfileButton),
            )
        }
    }
}

@Composable
private fun WebProfileTile(
    profile: ProfileModel,
    mode: ProfilesMode,
    selecting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
    ) {
        StreamCoreWebProfileCard(
            selected = selecting,
            enabled = enabled,
            onClick = onClick,
            modifier = modifier.semantics {
                role = Role.Button
                contentDescription = if (mode == ProfilesMode.Selection) {
                    "Select ${profile.displayName} profile"
                } else {
                    "Edit ${profile.displayName} profile"
                }
                if (!enabled) disabled()
            },
        ) {
            Box {
                StreamCoreProfileArtwork(
                    avatar = profile.avatar,
                    contentDescription = null,
                    modifier = Modifier.size(StreamCoreWebDimens.ProfileArtworkSize),
                )
                if (selecting) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    ) {
                        CircularProgressIndicator()
                    }
                }
                if (profile.isKidsProfile) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(StreamCoreDimens.Spacing.Small)
                            .testTag(ProfilesTestTags.KidsChipPrefix + profile.id),
                    ) {
                        Text(
                            text = "Kids",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(
                                horizontal = StreamCoreDimens.Spacing.Medium,
                                vertical = StreamCoreDimens.Spacing.Tiny,
                            ),
                        )
                    }
                }
            }
        }
        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (mode == ProfilesMode.Manage && profile.canDelete) {
            StreamCoreWebButton(
                text = "Delete",
                onClick = onDelete,
                variant = StreamCoreWebButtonVariant.Destructive,
                modifier = Modifier.testTag(ProfilesTestTags.DeleteProfileButtonPrefix + profile.id),
            )
        }
    }
}

@Composable
private fun WebAddProfileTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
    ) {
        StreamCoreWebProfileCard(
            selected = false,
            enabled = true,
            onClick = onClick,
            modifier = modifier.semantics {
                role = Role.Button
                contentDescription = "Add profile"
            },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(StreamCoreWebDimens.ProfileArtworkSize)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Text("+", style = MaterialTheme.typography.displayLarge)
            }
        }
        Text("Add profile", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun ProfilesLoading() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ProfilesError(onRetry: () -> Unit) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        retryFocus.requestFocus()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = Modifier.fillMaxSize(),
    ) {
        Text("Profiles are unavailable", style = MaterialTheme.typography.headlineMedium)
        Text("Check your connection and try again.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        StreamCoreWebButton(
            text = "Retry",
            onClick = onRetry,
            modifier = Modifier.focusRequester(retryFocus),
        )
    }
}

@Composable
private fun WebDeleteProfileDialog(
    profile: ProfileModel,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocus = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        cancelFocus.requestFocus()
    }
    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = StreamCoreDimens.Elevation.Medium,
            modifier = Modifier
                .webEscape { if (!isSaving) onDismiss() }
                .semantics {
                    contentDescription = "Delete ${profile.displayName} profile confirmation"
                },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                modifier = Modifier.padding(StreamCoreWebDimens.PanelPadding),
            ) {
                Text("Delete ${profile.displayName}?", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "This profile and its local preferences will be removed.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    StreamCoreWebButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        enabled = !isSaving,
                        variant = StreamCoreWebButtonVariant.Secondary,
                        modifier = Modifier
                            .focusRequester(cancelFocus)
                            .focusProperties {
                                left = confirmFocus
                                right = confirmFocus
                                up = confirmFocus
                                down = confirmFocus
                            }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                                    confirmFocus.requestFocus()
                                    true
                                } else {
                                    false
                                }
                            }
                            .testTag(ProfilesTestTags.EditorCancelDeleteButton),
                    )
                    StreamCoreWebButton(
                        text = "Delete",
                        onClick = onConfirm,
                        loading = isSaving,
                        variant = StreamCoreWebButtonVariant.Destructive,
                        modifier = Modifier
                            .focusRequester(confirmFocus)
                            .focusProperties {
                                left = cancelFocus
                                right = cancelFocus
                                up = cancelFocus
                                down = cancelFocus
                            }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                                    cancelFocus.requestFocus()
                                    true
                                } else {
                                    false
                                }
                            }
                            .testTag(ProfilesTestTags.ConfirmDeleteButton),
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfilesScreenPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebProfilesScreen(
            state = ProfilesUiState(isLoading = false, profiles = ProfilesPreviewData.profiles),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
            onBack = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfilesLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebProfilesScreen(
            state = ProfilesUiState(isLoading = true),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
            onBack = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfilesEmptyPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebProfilesScreen(
            state = ProfilesUiState(isLoading = false),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
            onBack = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfilesBackendErrorPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebProfilesScreen(
            state = ProfilesUiState(isLoading = false, loadError = AppError.Network()),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
            onBack = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfilesDeleteConfirmationPreview() {
    val profile = ProfilesPreviewData.profiles.first { it.canDelete }
    StreamCoreTheme(darkTheme = true) {
        WebProfilesScreen(
            state = ProfilesUiState(
                isLoading = false,
                profiles = ProfilesPreviewData.profiles,
                mode = ProfilesMode.Manage,
                pendingDeleteProfile = profile,
            ),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
            onBack = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfilesLongTextPreview() {
    val longProfile = ProfilesPreviewData.profiles.first().copy(
        displayName = "A very long localized subscriber profile name that must truncate safely",
    )
    StreamCoreTheme(darkTheme = true) {
        WebProfilesScreen(
            state = ProfilesUiState(isLoading = false, profiles = listOf(longProfile)),
            onAction = {},
            onCreateProfile = {},
            onEditProfile = {},
            onBack = {},
        )
    }
}
