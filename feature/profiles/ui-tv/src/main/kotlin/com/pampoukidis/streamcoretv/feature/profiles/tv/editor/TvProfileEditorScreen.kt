package com.pampoukidis.streamcoretv.feature.profiles.tv.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButtonVariant
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorFormUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorScreenUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileFieldError

@Composable
fun TvProfileEditorScreen(
    state: ProfileEditorScreenUiState,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayNameFocusRequester = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }
    val deleteFocusRequester = remember { FocusRequester() }
    val isEditorReady = !state.isLoading && state.editor != null && state.editorOptions != null
    val canDelete = state.mode == ProfileEditorMode.Edit && state.profile?.canDelete == true
    val isDeleteConfirmationVisible = state.pendingDeleteProfile != null
    var restoreDeleteFocus by remember { mutableStateOf(false) }

    LaunchedEffect(isEditorReady) {
        if (isEditorReady) {
            displayNameFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(isDeleteConfirmationVisible, canDelete) {
        if (isDeleteConfirmationVisible) {
            restoreDeleteFocus = true
        } else if (restoreDeleteFocus && canDelete) {
            deleteFocusRequester.requestFocus()
            restoreDeleteFocus = false
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(ProfilesTestTags.EditorRoot),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.editor == null || state.editorOptions == null -> Text(
                    text = "Unable to load profile editor.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> TvProfileEditorLoadedContent(
                    state = state,
                    canDelete = canDelete,
                    displayNameFocusRequester = displayNameFocusRequester,
                    cancelFocusRequester = cancelFocusRequester,
                    saveFocusRequester = saveFocusRequester,
                    deleteFocusRequester = deleteFocusRequester,
                    onAction = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = StreamCoreDimens.Form.WideContentMaxWidth)
                        .padding(StreamCoreDimens.Spacing.ExtraLarge),
                )
            }
        }
    }

    TvProfileDeleteConfirmationDialog(
        profile = state.pendingDeleteProfile,
        isSaving = state.isSaving,
        onConfirmDelete = { onAction(ProfileEditorAction.ConfirmDeleteProfile) },
        onDismiss = { onAction(ProfileEditorAction.DismissDeleteConfirmation) },
    )
}

@Composable
private fun TvProfileEditorLoadedContent(
    state: ProfileEditorScreenUiState,
    canDelete: Boolean,
    displayNameFocusRequester: FocusRequester,
    cancelFocusRequester: FocusRequester,
    saveFocusRequester: FocusRequester,
    deleteFocusRequester: FocusRequester,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editor = requireNotNull(state.editor)
    val options = requireNotNull(state.editorOptions)

    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = if (state.mode == ProfileEditorMode.Create) "Create profile" else "Edit profile",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
        )
        TvProfileEditorFields(
            editor = editor,
            options = options,
            isSaving = state.isSaving,
            displayNameFocusRequester = displayNameFocusRequester,
            cancelFocusRequester = cancelFocusRequester,
            onAction = onAction,
            modifier = Modifier.fillMaxWidth(),
        )
        TvProfileEditorActions(
            isSaving = state.isSaving,
            canDelete = canDelete,
            cancelFocusRequester = cancelFocusRequester,
            saveFocusRequester = saveFocusRequester,
            deleteFocusRequester = deleteFocusRequester,
            onAction = onAction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TvProfileEditorFields(
    editor: ProfileEditorFormUiState,
    options: ProfileEditorOptionsModel,
    isSaving: Boolean,
    displayNameFocusRequester: FocusRequester,
    cancelFocusRequester: FocusRequester,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Large),
        modifier = modifier.widthIn(
            min = StreamCoreDimens.Form.FieldMinWidth,
            max = StreamCoreDimens.Form.FieldMaxWidth,
        ),
    ) {
        OutlinedTextField(
            value = editor.draft.displayName,
            onValueChange = { onAction(ProfileEditorAction.DisplayNameChanged(it)) },
            label = { Text(text = "Display name") },
            singleLine = true,
            enabled = !isSaving,
            isError = editor.validation.displayNameError != null,
            supportingText = {
                editor.validation.displayNameError?.let { error ->
                    Text(text = error.message())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(displayNameFocusRequester)
                .testTag(ProfilesTestTags.EditorDisplayNameField),
        )
        TvOptionSection(title = "Avatar") {
            options.avatars.forEach { avatar ->
                FilterChip(
                    selected = editor.draft.avatarId == avatar.id,
                    onClick = { onAction(ProfileEditorAction.AvatarChanged(avatar.id)) },
                    enabled = !isSaving,
                    label = { Text(text = avatar.id.readableId()) },
                )
            }
        }
        editor.validation.avatarError?.let { error ->
            TvFieldErrorText(error = error)
        }
        TvOptionSection(title = "Parental level") {
            options.parentalLevels.forEach { parentalLevel ->
                FilterChip(
                    selected = editor.draft.parentalLevelId == parentalLevel.id,
                    onClick = {
                        onAction(ProfileEditorAction.ParentalLevelChanged(parentalLevel.id))
                    },
                    enabled = !isSaving,
                    label = { Text(text = parentalLevel.label) },
                    modifier = Modifier
                        .focusProperties { down = cancelFocusRequester }
                        .testTag(
                            ProfilesTestTags.EditorParentalLevelOptionPrefix + parentalLevel.id,
                        ),
                )
            }
        }
        editor.validation.parentalLevelError?.let { error ->
            TvFieldErrorText(error = error)
        }
    }
}

@Composable
private fun TvProfileEditorActions(
    isSaving: Boolean,
    canDelete: Boolean,
    cancelFocusRequester: FocusRequester,
    saveFocusRequester: FocusRequester,
    deleteFocusRequester: FocusRequester,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(
            space = StreamCoreDimens.Spacing.Medium,
            alignment = Alignment.End,
        ),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        StreamCoreTvButton(
            text = "Cancel",
            onClick = { onAction(ProfileEditorAction.Cancel) },
            enabled = !isSaving,
            variant = StreamCoreTvButtonVariant.Tertiary,
            modifier = Modifier
                .focusRequester(cancelFocusRequester)
                .focusProperties { right = saveFocusRequester }
                .testTag(ProfilesTestTags.EditorCancelButton),
        )
        StreamCoreTvButton(
            text = "Save",
            onClick = { onAction(ProfileEditorAction.Submit) },
            enabled = !isSaving,
            loading = isSaving,
            modifier = Modifier
                .focusRequester(saveFocusRequester)
                .focusProperties {
                    left = cancelFocusRequester
                    if (canDelete) {
                        right = deleteFocusRequester
                    }
                }
                .testTag(ProfilesTestTags.EditorSubmitButton),
        )
        if (canDelete) {
            StreamCoreTvButton(
                text = "Delete",
                onClick = { onAction(ProfileEditorAction.RequestDeleteProfile) },
                enabled = !isSaving,
                variant = StreamCoreTvButtonVariant.Secondary,
                modifier = Modifier
                    .focusRequester(deleteFocusRequester)
                    .focusProperties { left = saveFocusRequester }
                    .testTag(ProfilesTestTags.EditorDeleteButton),
            )
        }
    }
}

@Composable
private fun TvOptionSection(
    title: String,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Small),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            content = content,
        )
    }
}

@Composable
private fun TvFieldErrorText(error: ProfileFieldError) {
    Text(
        text = error.message(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

private fun ProfileFieldError.message(): String {
    return when (this) {
        ProfileFieldError.Blank -> "Required"
        ProfileFieldError.TooLong -> "Maximum 32 characters"
        ProfileFieldError.MissingSelection -> "Select an option"
        ProfileFieldError.UnknownSelection -> "Selection is unavailable"
    }
}

private fun String.readableId(): String {
    return substringAfterLast("-").replaceFirstChar { it.uppercase() }
}

private fun previewState(
    profile: ProfileModel,
    isSaving: Boolean = false,
    showDeleteConfirmation: Boolean = false,
): ProfileEditorScreenUiState {
    return ProfileEditorScreenUiState(
        mode = ProfileEditorMode.Edit,
        isLoading = false,
        isSaving = isSaving,
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
        profile = profile,
        pendingDeleteProfile = profile.takeIf { showDeleteConfirmation },
    )
}

@PreviewTV
@Composable
private fun TvProfileEditorDeletablePreview() {
    StreamCoreTheme(darkTheme = true) {
        TvProfileEditorScreen(
            state = previewState(ProfilesPreviewData.profiles.first { it.canDelete }),
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvProfileEditorProtectedPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvProfileEditorScreen(
            state = previewState(ProfilesPreviewData.profiles.first { !it.canDelete }),
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvProfileEditorConfirmationPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvProfileEditorScreen(
            state = previewState(
                profile = ProfilesPreviewData.profiles.first { it.canDelete },
                showDeleteConfirmation = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvProfileEditorSavingPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvProfileEditorScreen(
            state = previewState(
                profile = ProfilesPreviewData.profiles.first { it.canDelete },
                isSaving = true,
                showDeleteConfirmation = true,
            ),
            onAction = {},
        )
    }
}

@PreviewTV
@Composable
private fun TvProfileEditorDeleteFailureRecoveryPreview() {
    StreamCoreTheme(darkTheme = true) {
        TvProfileEditorScreen(
            state = previewState(ProfilesPreviewData.profiles.last { it.canDelete }),
            onAction = {},
        )
    }
}
