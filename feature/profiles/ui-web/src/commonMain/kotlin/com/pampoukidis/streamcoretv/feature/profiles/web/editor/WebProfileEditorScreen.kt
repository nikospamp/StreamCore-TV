package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButton
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebButtonVariant
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebPanel
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebProfileCard
import com.pampoukidis.streamcoretv.core.ui.web.webEscape
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorFormUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorScreenUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileFieldError

@Composable
fun WebProfileEditorScreen(
    state: ProfileEditorScreenUiState,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nameFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }
    val saveFocus = remember { FocusRequester() }
    val deleteFocus = remember { FocusRequester() }
    val ready = !state.isLoading && state.editor != null && state.editorOptions != null

    LaunchedEffect(ready) {
        if (ready) nameFocus.requestFocus()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .webEscape { onAction(ProfileEditorAction.Cancel) }
            .testTag(ProfilesTestTags.EditorRoot),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = StreamCoreWebDimens.ScreenHorizontal,
                    vertical = StreamCoreWebDimens.ScreenVertical,
                ),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.editor == null || state.editorOptions == null -> Text(
                    text = "Unable to load profile editor.",
                    style = MaterialTheme.typography.headlineSmall,
                )
                else -> WebProfileEditorContent(
                    state = state,
                    editor = requireNotNull(state.editor),
                    options = requireNotNull(state.editorOptions),
                    nameFocus = nameFocus,
                    cancelFocus = cancelFocus,
                    saveFocus = saveFocus,
                    deleteFocus = deleteFocus,
                    onAction = onAction,
                )
            }
        }
    }

    state.pendingDeleteProfile?.let { profile ->
        WebEditorDeleteDialog(
            profile = profile,
            isSaving = state.isSaving,
            onConfirm = { onAction(ProfileEditorAction.ConfirmDeleteProfile) },
            onDismiss = { onAction(ProfileEditorAction.DismissDeleteConfirmation) },
        )
    }
}

@Composable
private fun WebProfileEditorContent(
    state: ProfileEditorScreenUiState,
    editor: ProfileEditorFormUiState,
    options: ProfileEditorOptionsModel,
    nameFocus: FocusRequester,
    cancelFocus: FocusRequester,
    saveFocus: FocusRequester,
    deleteFocus: FocusRequester,
    onAction: (ProfileEditorAction) -> Unit,
) {
    val canDelete = state.mode == ProfileEditorMode.Edit && state.profile?.canDelete == true
    StreamCoreWebPanel(
        modifier = Modifier.widthIn(max = StreamCoreWebDimens.WidePanelWidth),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = if (state.mode == ProfileEditorMode.Create) "Create profile" else "Edit profile",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            val displayNameError = editor.validation.displayNameError?.message()
            OutlinedTextField(
                value = editor.draft.displayName,
                onValueChange = { onAction(ProfileEditorAction.DisplayNameChanged(it)) },
                label = { Text("Display name") },
                singleLine = true,
                enabled = !state.isSaving,
                isError = displayNameError != null,
                supportingText = displayNameError?.let { message -> { Text(message) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocus)
                    .then(if (displayNameError != null) Modifier.semantics { error(displayNameError) } else Modifier)
                    .testTag(ProfilesTestTags.EditorDisplayNameField),
            )
            OptionSection(title = "Avatar") {
                options.avatars.forEach { avatar ->
                    AvatarOption(
                        avatar = avatar,
                        selected = editor.draft.avatarId == avatar.id,
                        enabled = !state.isSaving,
                        onClick = { onAction(ProfileEditorAction.AvatarChanged(avatar.id)) },
                    )
                }
            }
            editor.validation.avatarError?.let { error -> ValidationError(error.message()) }
            OptionSection(title = "Maturity") {
                options.parentalLevels.forEach { level ->
                    StreamCoreWebButton(
                        text = level.label,
                        onClick = { onAction(ProfileEditorAction.ParentalLevelChanged(level.id)) },
                        enabled = !state.isSaving,
                        variant = if (editor.draft.parentalLevelId == level.id) {
                            StreamCoreWebButtonVariant.Primary
                        } else {
                            StreamCoreWebButtonVariant.Secondary
                        },
                        modifier = Modifier.testTag(
                            ProfilesTestTags.EditorParentalLevelOptionPrefix + level.id,
                        ),
                    )
                }
            }
            editor.validation.parentalLevelError?.let { error -> ValidationError(error.message()) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.End),
            ) {
                StreamCoreWebButton(
                    text = "Cancel",
                    onClick = { onAction(ProfileEditorAction.Cancel) },
                    enabled = !state.isSaving,
                    variant = StreamCoreWebButtonVariant.Tertiary,
                    modifier = Modifier
                        .focusRequester(cancelFocus)
                        .focusProperties { right = saveFocus }
                        .testTag(ProfilesTestTags.EditorCancelButton),
                )
                StreamCoreWebButton(
                    text = "Save",
                    onClick = { onAction(ProfileEditorAction.Submit) },
                    loading = state.isSaving,
                    modifier = Modifier
                        .focusRequester(saveFocus)
                        .focusProperties {
                            left = cancelFocus
                            if (canDelete) right = deleteFocus
                        }
                        .testTag(ProfilesTestTags.EditorSubmitButton),
                )
                if (canDelete) {
                    StreamCoreWebButton(
                        text = "Delete",
                        onClick = { onAction(ProfileEditorAction.RequestDeleteProfile) },
                        enabled = !state.isSaving,
                        variant = StreamCoreWebButtonVariant.Destructive,
                        modifier = Modifier
                            .focusRequester(deleteFocus)
                            .focusProperties { left = saveFocus }
                            .testTag(ProfilesTestTags.EditorDeleteButton),
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) { content() }
    }
}

@Composable
private fun AvatarOption(
    avatar: ProfileAvatarModel,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    StreamCoreWebProfileCard(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier
            .size(StreamCoreWebDimens.AvatarOptionSize)
            .semantics {
                role = Role.Button
                contentDescription = "Avatar ${avatar.id.substringAfterLast('-')}"
            }
            .testTag(ProfilesTestTags.EditorAvatarOptionPrefix + avatar.id),
    ) {
        StreamCoreProfileArtwork(
            avatar = avatar,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ValidationError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { error(message) },
    )
}

@Composable
private fun WebEditorDeleteDialog(
    profile: ProfileModel,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocus = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocus.requestFocus() }
    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = StreamCoreDimens.Elevation.Medium,
            modifier = Modifier
                .webEscape { if (!isSaving) onDismiss() }
                .testTag(ProfilesTestTags.EditorDeleteConfirmation)
                .semantics { contentDescription = "Delete ${profile.displayName} profile confirmation" },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
                modifier = Modifier.padding(StreamCoreWebDimens.PanelPadding),
            ) {
                Text("Delete ${profile.displayName}?", style = MaterialTheme.typography.headlineMedium)
                Text("This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            .testTag(ProfilesTestTags.ConfirmDeleteButton),
                    )
                }
            }
        }
    }
}

private fun ProfileFieldError.message(): String {
    return when (this) {
        ProfileFieldError.Blank -> "Required"
        ProfileFieldError.TooLong -> "Maximum 32 characters"
        ProfileFieldError.MissingSelection -> "Select an option"
        ProfileFieldError.UnknownSelection -> "Selection is unavailable"
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfileEditorScreenPreview() {
    val profile = ProfilesPreviewData.profiles[1]
    StreamCoreTheme(darkTheme = true) {
        WebProfileEditorScreen(
            state = ProfileEditorScreenUiState(
                mode = ProfileEditorMode.Edit,
                isLoading = false,
                editorOptions = ProfilesPreviewData.editorOptions.copy(
                    avatars = ProfilesPreviewData.avatars.take(6),
                ),
                editor = ProfileEditorFormUiState(
                    mode = ProfileEditorMode.Edit,
                    draft = ProfileDraftModel(
                        profileId = profile.id,
                        displayName = profile.displayName,
                        avatarId = profile.avatar.id,
                        parentalLevelId = profile.parentalLevel.id,
                    ),
                ),
                profile = profile,
            ),
            onAction = {},
        )
    }
}
