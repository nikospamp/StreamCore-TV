package com.pampoukidis.streamcoretv.feature.profiles.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.presentation.ProfilesDeleteConfirmationDialog
import com.pampoukidis.streamcoretv.feature.profiles.common.presentation.ProfilesEditorDialog
import com.pampoukidis.streamcoretv.feature.profiles.common.presentation.ProfilesGrid
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
fun TvProfilesScreen(
    state: ProfilesUiState,
    onAction: (ProfilesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(ProfilesTestTags.Root),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = StreamCoreDimens.Tv.ScreenHorizontalPadding,
                    vertical = StreamCoreDimens.Tv.ScreenVerticalPadding,
                ),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Who's watching?",
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Text(
                        text = "Choose the profile for this session.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StreamCoreTvButton(
                    text = "Add profile",
                    onClick = { onAction(ProfilesAction.StartCreateProfile) },
                    enabled = !state.isLoading && !state.isSaving,
                    modifier = Modifier.testTag(ProfilesTestTags.AddProfileButton),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f),
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator()
                    state.profiles.isEmpty() -> Text(
                        text = "No profiles yet.",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    else -> ProfilesGrid(
                        profiles = state.profiles,
                        columns = GridCells.Adaptive(220.dp),
                        pendingSelectionProfileId = state.pendingSelectionProfileId,
                        onAction = onAction,
                        avatarSize = 96.dp,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    ProfilesEditorDialog(
        editor = state.editor,
        options = state.editorOptions,
        isSaving = state.isSaving,
        onAction = onAction,
    )
    ProfilesDeleteConfirmationDialog(
        profile = state.pendingDeleteProfile,
        isSaving = state.isSaving,
        onAction = onAction,
    )
}

@PreviewTV
@Composable
private fun TvProfilesScreenPreview() {
    StreamCoreTVTheme {
        TvProfilesScreen(
            state = ProfilesUiState(
                isLoading = false,
                profiles = ProfilesPreviewData.profiles,
                editorOptions = ProfilesPreviewData.editorOptions,
            ),
            onAction = {},
        )
    }
}
