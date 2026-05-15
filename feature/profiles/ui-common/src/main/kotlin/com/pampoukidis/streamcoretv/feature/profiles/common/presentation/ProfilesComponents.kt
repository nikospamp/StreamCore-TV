package com.pampoukidis.streamcoretv.feature.profiles.common.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfileEditorUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.contract.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileFieldError

@Composable
fun ProfilesGrid(
    profiles: List<ProfileModel>,
    columns: GridCells,
    pendingSelectionProfileId: String?,
    onAction: (ProfilesAction) -> Unit,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 72.dp,
) {
    LazyVerticalGrid(
        columns = columns,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        items(
            items = profiles,
            key = { it.id },
            contentType = { "profile" },
        ) { profile ->
            ProfileCard(
                profile = profile,
                isSelecting = pendingSelectionProfileId == profile.id,
                onAction = onAction,
                avatarSize = avatarSize,
            )
        }
    }
}

@Composable
fun ProfilesEditorDialog(
    editor: ProfileEditorUiState?,
    options: ProfileEditorOptionsModel?,
    isSaving: Boolean,
    onAction: (ProfilesAction) -> Unit,
) {
    if (editor == null || options == null) return

    val title = when (editor.mode) {
        ProfileEditorMode.Create -> "Create profile"
        ProfileEditorMode.Edit -> "Edit profile"
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onAction(ProfilesAction.DismissEditor)
        },
        title = {
            Text(text = title)
        },
        text = {
            ProfileEditorContent(
                editor = editor,
                options = options,
                isSaving = isSaving,
                onAction = onAction,
            )
        },
        confirmButton = {
            StreamCoreButton(
                text = "Save",
                onClick = { onAction(ProfilesAction.SubmitEditor) },
                enabled = !isSaving,
                loading = isSaving,
                modifier = Modifier.testTag(ProfilesTestTags.EditorSubmitButton),
            )
        },
        dismissButton = {
            StreamCoreTextButton(
                text = "Cancel",
                onClick = { onAction(ProfilesAction.DismissEditor) },
                enabled = !isSaving,
            )
        },
    )
}

@Composable
fun ProfilesDeleteConfirmationDialog(
    profile: ProfileModel?,
    isSaving: Boolean,
    onAction: (ProfilesAction) -> Unit,
) {
    if (profile == null) return

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onAction(ProfilesAction.DismissDeleteConfirmation)
        },
        title = {
            Text(text = "Delete profile")
        },
        text = {
            Text(text = "Delete ${profile.displayName}? This cannot be undone.")
        },
        confirmButton = {
            StreamCoreButton(
                text = "Delete",
                onClick = { onAction(ProfilesAction.ConfirmDeleteProfile) },
                enabled = !isSaving,
                loading = isSaving,
            )
        },
        dismissButton = {
            StreamCoreTextButton(
                text = "Cancel",
                onClick = { onAction(ProfilesAction.DismissDeleteConfirmation) },
                enabled = !isSaving,
            )
        },
    )
}

@Composable
private fun ProfileCard(
    profile: ProfileModel,
    isSelecting: Boolean,
    onAction: (ProfilesAction) -> Unit,
    avatarSize: Dp,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !isSelecting) {
                onAction(ProfilesAction.SelectProfile(profile.id))
            }
            .testTag(ProfilesTestTags.ProfileCardPrefix + profile.id),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            ProfileAvatarCircle(
                profile = profile,
                size = avatarSize,
            )
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = profile.parentalLevel.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isSelecting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StreamCoreTextButton(
                        text = "Edit",
                        onClick = { onAction(ProfilesAction.StartEditProfile(profile.id)) },
                        enabled = true,
                        modifier = Modifier.testTag(
                            ProfilesTestTags.EditProfileButtonPrefix + profile.id,
                        ),
                    )
                    if (profile.canDelete) {
                        StreamCoreTextButton(
                            text = "Delete",
                            onClick = {
                                onAction(ProfilesAction.RequestDeleteProfile(profile.id))
                            },
                            enabled = true,
                            modifier = Modifier.testTag(
                                ProfilesTestTags.DeleteProfileButtonPrefix + profile.id,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatarCircle(
    profile: ProfileModel,
    size: Dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (profile.isKidsProfile) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            ),
    ) {
        Text(
            text = profile.displayName.initials(),
            style = MaterialTheme.typography.headlineMedium,
            color = if (profile.isKidsProfile) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
        )
    }
}

@Composable
private fun ProfileEditorContent(
    editor: ProfileEditorUiState,
    options: ProfileEditorOptionsModel,
    isSaving: Boolean,
    onAction: (ProfilesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.widthIn(min = 280.dp, max = 460.dp),
    ) {
        OutlinedTextField(
            value = editor.draft.displayName,
            onValueChange = { onAction(ProfilesAction.DisplayNameChanged(it)) },
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
                    onClick = { onAction(ProfilesAction.AvatarChanged(avatar.id)) },
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
                        onAction(ProfilesAction.ParentalLevelChanged(parentalLevel.id))
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

private fun String.initials(): String {
    return trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(separator = "") { it.first().uppercase() }
        .ifBlank { "?" }
}

private fun String.readableId(): String {
    return substringAfterLast("-")
        .replaceFirstChar { it.uppercase() }
}

@PreviewMobile
@Composable
private fun ProfilesGridPreview() {
    StreamCoreTVTheme {
        ProfilesGrid(
            profiles = ProfilesPreviewData.profiles,
            columns = GridCells.Fixed(2),
            pendingSelectionProfileId = null,
            onAction = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .padding(16.dp),
        )
    }
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
