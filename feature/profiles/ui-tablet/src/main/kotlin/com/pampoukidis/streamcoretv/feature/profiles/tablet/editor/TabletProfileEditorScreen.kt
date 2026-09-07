package com.pampoukidis.streamcoretv.feature.profiles.tablet.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTablet
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.AvatarPickerContent
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.AvatarPickerLayout
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorContent
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorFormUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorScreenUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.AndroidProfilesBackdrop
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesDeleteConfirmationDialog
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode

@Composable
fun TabletProfileEditorScreen(
    state: ProfileEditorScreenUiState,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var avatarPickerVisible by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().testTag(ProfilesTestTags.EditorRoot)) {
        AndroidProfilesBackdrop(modifier = Modifier.matchParentSize())
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(
                    horizontal = StreamCoreDimens.Tablet.Screen.HorizontalPadding,
                    vertical = StreamCoreDimens.Tablet.Screen.VerticalPadding,
                ),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .widthIn(max = StreamCoreDimens.Tablet.Profiles.EditorPanelWidth)
                    .fillMaxWidth()
                    .heightIn(max = StreamCoreDimens.Tablet.Profiles.EditorPanelMaxHeight)
                    .fillMaxHeight(),
            ) {
                ProfileEditorContent(
                    state = state,
                    onAction = onAction,
                    onAvatarClick = { avatarPickerVisible = true },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    val editor = state.editor
    val options = state.editorOptions
    if (avatarPickerVisible && editor != null && options != null) {
        Dialog(
            onDismissRequest = { avatarPickerVisible = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AvatarPickerContent(
                avatars = options.avatars,
                selectedAvatarId = editor.draft.avatarId,
                onAvatarSelected = { avatarId ->
                    onAction(ProfileEditorAction.AvatarChanged(avatarId))
                    avatarPickerVisible = false
                },
                onDismissRequest = { avatarPickerVisible = false },
                layout = AvatarPickerLayout(
                    gridMaxHeight = StreamCoreDimens.Tablet.Profiles.AvatarPickerGridMaxHeight,
                ),
                modifier = Modifier
                    .widthIn(max = StreamCoreDimens.Tablet.Profiles.AvatarPickerMaxWidth)
                    .fillMaxWidth(),
            )
        }
    }

    ProfilesDeleteConfirmationDialog(
        profile = state.pendingDeleteProfile,
        isSaving = state.isSaving,
        onConfirmDelete = { onAction(ProfileEditorAction.ConfirmDeleteProfile) },
        onDismiss = { onAction(ProfileEditorAction.DismissDeleteConfirmation) },
    )
}

@PreviewTablet
@Composable
private fun TabletProfileEditorScreenPreview() {
    val profile = ProfilesPreviewData.profiles.first()
    StreamCoreTheme(darkTheme = true) {
        TabletProfileEditorScreen(
            state = ProfileEditorScreenUiState(
                mode = ProfileEditorMode.Edit,
                isLoading = false,
                profile = profile,
                editorOptions = ProfilesPreviewData.editorOptions,
                editor = ProfileEditorFormUiState(
                    mode = ProfileEditorMode.Edit,
                    draft = ProfileDraftModel(
                        profileId = profile.id,
                        displayName = profile.displayName,
                        avatarId = profile.avatar.id,
                        parentalLevelId = profile.parentalLevel.id,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
