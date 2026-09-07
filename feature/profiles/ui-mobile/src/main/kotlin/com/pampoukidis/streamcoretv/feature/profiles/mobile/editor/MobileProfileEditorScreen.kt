package com.pampoukidis.streamcoretv.feature.profiles.mobile.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorContent
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorFormUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorScreenUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesDeleteConfirmationDialog
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode

@Composable
fun MobileProfileEditorScreen(
    state: ProfileEditorScreenUiState,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var avatarPickerVisible by rememberSaveable { mutableStateOf(false) }
    val editor = state.editor
    val options = state.editorOptions

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag(ProfilesTestTags.EditorRoot),
    ) {
        ProfileEditorContent(
            state = state,
            onAction = onAction,
            onAvatarClick = { avatarPickerVisible = true },
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (avatarPickerVisible && editor != null && options != null) {
        MobileAvatarPickerDialog(
            avatars = options.avatars,
            selectedAvatarId = editor.draft.avatarId,
            onAvatarSelected = { avatarId ->
                onAction(ProfileEditorAction.AvatarChanged(avatarId))
                avatarPickerVisible = false
            },
            onDismissRequest = { avatarPickerVisible = false },
        )
    }

    ProfilesDeleteConfirmationDialog(
        profile = state.pendingDeleteProfile,
        isSaving = state.isSaving,
        onConfirmDelete = { onAction(ProfileEditorAction.ConfirmDeleteProfile) },
        onDismiss = { onAction(ProfileEditorAction.DismissDeleteConfirmation) },
    )
}
@PreviewMobile
@Composable
private fun MobileProfileEditorCreatePreview() {
    StreamCoreTheme(darkTheme = true) {
        MobileProfileEditorScreen(
            state = ProfileEditorScreenUiState(
                mode = ProfileEditorMode.Create,
                isLoading = false,
                editorOptions = ProfilesPreviewData.editorOptions,
                editor = ProfileEditorFormUiState(
                    mode = ProfileEditorMode.Create,
                    draft = ProfileDraftModel(
                        avatarId = ProfilesPreviewData.avatars.first().id,
                        parentalLevelId = ProfilesPreviewData.parentalLevels.first().id,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@PreviewMobile
@Composable
private fun MobileProfileEditorEditPreview() {
    val profile = ProfilesPreviewData.profiles.first { it.canDelete }
    val draft = ProfileDraftModel(
        profileId = profile.id,
        displayName = profile.displayName,
        avatarId = profile.avatar.id,
        parentalLevelId = profile.parentalLevel.id,
    )
    StreamCoreTheme {
        MobileProfileEditorScreen(
            state = ProfileEditorScreenUiState(
                mode = ProfileEditorMode.Edit,
                isLoading = false,
                editorOptions = ProfilesPreviewData.editorOptions,
                profile = profile,
                editor = ProfileEditorFormUiState(
                    mode = ProfileEditorMode.Edit,
                    draft = draft,
                ),
            ),
            onAction = {},
        )
    }
}
