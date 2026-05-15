package com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesDeleteConfirmationDialog
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesGrid
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags

@Composable
fun MobileProfilesScreen(
    state: ProfilesUiState,
    onAction: (ProfilesAction) -> Unit,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(ProfilesTestTags.Root),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text(
                text = "Who's watching?",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Choose a profile or manage account profiles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StreamCoreButton(
                text = "Add profile",
                onClick = onCreateProfile,
                enabled = !state.isLoading && !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ProfilesTestTags.AddProfileButton),
            )
            ProfilesBody(
                state = state,
                onAction = onAction,
                onEditProfile = onEditProfile,
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
            )
        }
    }

    ProfilesDeleteConfirmationDialog(
        profile = state.pendingDeleteProfile,
        isSaving = state.isSaving,
        onAction = onAction,
    )
}

@Composable
private fun ProfilesBody(
    state: ProfilesUiState,
    onAction: (ProfilesAction) -> Unit,
    onEditProfile: (String) -> Unit,
    columns: GridCells,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth(),
    ) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.profiles.isEmpty() -> Text(
                text = "No profiles yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> ProfilesGrid(
                profiles = state.profiles,
                columns = columns,
                pendingSelectionProfileId = state.pendingSelectionProfileId,
                onAction = onAction,
                onEditProfile = onEditProfile,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@PreviewMobile
@Composable
private fun MobileProfilesScreenPreview() {
    StreamCoreTVTheme {
        MobileProfilesScreen(
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

