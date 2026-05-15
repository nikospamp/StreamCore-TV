package com.pampoukidis.streamcoretv.feature.profiles.common.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorScreenUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileFieldError

@Composable
fun ProfileEditorScreen(
    state: ProfileEditorScreenUiState,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
    useTvControls: Boolean = false,
) {
    val title = when (state.mode) {
        ProfileEditorMode.Create -> "Create profile"
        ProfileEditorMode.Edit -> "Edit profile"
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

                else -> ProfileEditorLoadedContent(
                    title = title,
                    editor = state.editor,
                    options = state.editorOptions,
                    isSaving = state.isSaving,
                    onAction = onAction,
                    useTvControls = useTvControls,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = if (useTvControls) 720.dp else 520.dp)
                        .padding(24.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileEditorLoadedContent(
    title: String,
    editor: ProfileEditorUiState,
    options: ProfileEditorOptionsModel,
    isSaving: Boolean,
    onAction: (ProfileEditorAction) -> Unit,
    useTvControls: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = title,
            style = if (useTvControls) {
                MaterialTheme.typography.displaySmall
            } else {
                MaterialTheme.typography.headlineMedium
            },
            fontWeight = FontWeight.SemiBold,
        )
        ProfileEditorContent(
            editor = editor,
            options = options,
            isSaving = isSaving,
            onAction = onAction,
            modifier = Modifier.fillMaxWidth(),
        )
        ProfileEditorActions(
            isSaving = isSaving,
            onAction = onAction,
            useTvControls = useTvControls,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ProfileEditorActions(
    isSaving: Boolean,
    onAction: (ProfileEditorAction) -> Unit,
    useTvControls: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        if (useTvControls) {
            StreamCoreTvButton(
                text = "Cancel",
                onClick = { onAction(ProfileEditorAction.Cancel) },
                enabled = !isSaving,
            )
            StreamCoreTvButton(
                text = "Save",
                onClick = { onAction(ProfileEditorAction.Submit) },
                enabled = !isSaving,
                loading = isSaving,
                modifier = Modifier.testTag(ProfilesTestTags.EditorSubmitButton),
            )
        } else {
            StreamCoreTextButton(
                text = "Cancel",
                onClick = { onAction(ProfileEditorAction.Cancel) },
                enabled = !isSaving,
            )
            StreamCoreButton(
                text = "Save",
                onClick = { onAction(ProfileEditorAction.Submit) },
                enabled = !isSaving,
                loading = isSaving,
                modifier = Modifier.testTag(ProfilesTestTags.EditorSubmitButton),
            )
        }
    }
}

@Composable
private fun ProfileEditorContent(
    editor: ProfileEditorUiState,
    options: ProfileEditorOptionsModel,
    isSaving: Boolean,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.widthIn(min = 280.dp, max = 460.dp),
    ) {
        OutlinedTextField(
            value = editor.draft.displayName,
            onValueChange = { onAction(ProfileEditorAction.DisplayNameChanged(it)) },
            label = { Text(text = "Display name") },
            singleLine = true,
            enabled = !isSaving,
            isError = editor.validation.displayNameError != null,
            supportingText = {
                editor.validation.displayNameError?.let {
                    Text(text = it.message())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ProfilesTestTags.EditorDisplayNameField),
        )
        OptionSection(title = "Avatar") {
            options.avatars.forEach { avatar ->
                FilterChip(
                    selected = editor.draft.avatarId == avatar.id,
                    onClick = { onAction(ProfileEditorAction.AvatarChanged(avatar.id)) },
                    enabled = !isSaving,
                    label = { Text(text = avatar.id.readableId()) },
                )
            }
        }
        editor.validation.avatarError?.let {
            FieldErrorText(error = it)
        }
        OptionSection(title = "Parental level") {
            options.parentalLevels.forEach { parentalLevel ->
                FilterChip(
                    selected = editor.draft.parentalLevelId == parentalLevel.id,
                    onClick = {
                        onAction(ProfileEditorAction.ParentalLevelChanged(parentalLevel.id))
                    },
                    enabled = !isSaving,
                    label = { Text(text = parentalLevel.label) },
                )
            }
        }
        editor.validation.parentalLevelError?.let {
            FieldErrorText(error = it)
        }
    }
}

@Composable
private fun OptionSection(
    title: String,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            content = content,
        )
    }
}

@Composable
private fun FieldErrorText(error: ProfileFieldError) {
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
    return substringAfterLast("-")
        .replaceFirstChar { it.uppercase() }
}

@PreviewMobile
@Composable
private fun ProfileEditorContentPreview() {
    StreamCoreTVTheme {
        Surface {
            ProfileEditorContent(
                editor = ProfileEditorUiState(
                    mode = ProfileEditorMode.Edit,
                    draft = ProfileDraftModel(
                        profileId = "profile-1",
                        displayName = "Nikos",
                        avatarId = "avatar-default",
                        parentalLevelId = "all",
                    ),
                ),
                options = ProfilesPreviewData.editorOptions,
                isSaving = false,
                onAction = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewMobile
@Composable
private fun ProfileEditorScreenPreview() {
    StreamCoreTVTheme {
        ProfileEditorScreen(
            state = ProfileEditorScreenUiState(
                isLoading = false,
                editorOptions = ProfilesPreviewData.editorOptions,
                editor = ProfileEditorUiState(
                    mode = ProfileEditorMode.Create,
                    draft = ProfileDraftModel(
                        avatarId = "avatar-default",
                        parentalLevelId = "all",
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
