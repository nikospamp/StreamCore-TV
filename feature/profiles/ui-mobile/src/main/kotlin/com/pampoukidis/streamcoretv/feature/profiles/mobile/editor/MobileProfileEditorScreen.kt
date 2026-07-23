package com.pampoukidis.streamcoretv.feature.profiles.mobile.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCloseButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreEditIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSettingsSwitchRow
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewMobile
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorFormUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorScreenUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesDeleteConfirmationDialog
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileFieldError

@Composable
fun MobileProfileEditorScreen(
    state: ProfileEditorScreenUiState,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var avatarPickerVisible by rememberSaveable { mutableStateOf(false) }
    val editor = state.editor
    val options = state.editorOptions
    val title = when (state.mode) {
        ProfileEditorMode.Create -> "Create profile"
        ProfileEditorMode.Edit -> "Edit profile"
    }
    val saveEnabled = editor != null &&
            editor.hasChanges &&
            editor.validation.isValid &&
            !state.isSaving

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(ProfilesTestTags.EditorRoot),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MobileProfileEditorTopBar(
                title = title,
                saveEnabled = saveEnabled,
                isSaving = state.isSaving,
                onClose = { onAction(ProfileEditorAction.Cancel) },
                onSave = { onAction(ProfileEditorAction.Submit) },
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    state.isLoading -> CircularProgressIndicator()
                    editor == null || options == null -> Text(
                        text = "Unable to load profile editor.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(StreamCoreDimens.Spacing.ExtraLarge),
                    )

                    else -> MobileProfileEditorContent(
                        editor = editor,
                        options = options,
                        canDelete = state.profile?.canDelete == true,
                        isSaving = state.isSaving,
                        onAvatarClick = { avatarPickerVisible = true },
                        onAction = onAction,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
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

@Composable
private fun MobileProfileEditorTopBar(
    title: String,
    saveEnabled: Boolean,
    isSaving: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = EditorTopBarHeight)
            .padding(horizontal = StreamCoreDimens.Spacing.Small),
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.width(EditorTopBarSideWidth),
        ) {
            StreamCoreCloseButton(
                onClick = onClose,
                enabled = !isSaving,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier.width(EditorTopBarSideWidth),
        ) {
            StreamCoreTextButton(
                text = if (isSaving) "Saving" else "Save",
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.testTag(ProfilesTestTags.EditorSubmitButton),
            )
        }
    }
}

@Composable
private fun MobileProfileEditorContent(
    editor: ProfileEditorFormUiState,
    options: ProfileEditorOptionsModel,
    canDelete: Boolean,
    isSaving: Boolean,
    onAvatarClick: () -> Unit,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val selectedAvatar = options.avatars.firstOrNull { it.id == editor.draft.avatarId }
        ?: options.avatars.firstOrNull()
    val selectedParentalLevel = options.parentalLevels.firstOrNull {
        it.id == editor.draft.parentalLevelId
    }
    val isKidsProfile = selectedParentalLevel?.isKids == true

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.ExtraLarge),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                start = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
                top = StreamCoreDimens.Spacing.ExtraLarge,
                end = StreamCoreDimens.Mobile.Screen.HorizontalPadding,
                bottom = StreamCoreDimens.Spacing.ExtraLarge,
            ),
    ) {
        if (selectedAvatar != null) {
            MobileProfileEditorAvatar(
                avatar = selectedAvatar,
                enabled = !isSaving,
                onClick = onAvatarClick,
            )
        }
        OutlinedTextField(
            value = editor.draft.displayName,
            onValueChange = { onAction(ProfileEditorAction.DisplayNameChanged(it)) },
            label = { Text(text = "Username") },
            singleLine = true,
            enabled = !isSaving,
            isError = editor.validation.displayNameError != null,
            supportingText = editor.validation.displayNameError?.let { error ->
                { Text(text = error.message()) }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ProfilesTestTags.EditorDisplayNameField),
        )
        StreamCoreSettingsSwitchRow(
            title = "Kids profile",
            supportingText = "Only age-appropriate content",
            checked = isKidsProfile,
            enabled = !isSaving,
            onCheckedChange = {
                onAction(ProfileEditorAction.KidsProfileChanged(it))
            },
            modifier = Modifier.testTag(ProfilesTestTags.EditorKidsSwitch),
        )
        if (canDelete) {
            StreamCoreTextButton(
                text = "Delete profile",
                onClick = { onAction(ProfileEditorAction.RequestDeleteProfile) },
                enabled = !isSaving,
                contentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ProfilesTestTags.EditorDeleteButton),
            )
        }
    }
}

@Composable
private fun MobileProfileEditorAvatar(
    avatar: ProfileAvatarModel,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(StreamCoreDimens.Spacing.Small)
            .testTag(ProfilesTestTags.EditorAvatarButton),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(EditorAvatarContainerSize),
        ) {
            Box(
                modifier = Modifier
                    .size(EditorAvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                StreamCoreProfileArtwork(
                    avatar = avatar,
                    contentDescription = "Selected profile avatar",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = StreamCoreDimens.Elevation.Low,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = EditorBadgeOffset, y = EditorBadgeOffset)
                    .size(EditorBadgeSize),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    StreamCoreEditIcon(modifier = Modifier.size(StreamCoreDimens.Icon.Medium))
                }
            }
        }
        Text(
            text = "Tap to change",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

private val EditorTopBarHeight = 56.dp
private val EditorTopBarSideWidth = 72.dp
private val EditorAvatarSize = 120.dp
private val EditorAvatarContainerSize = 132.dp
private val EditorBadgeSize = 36.dp
private val EditorBadgeOffset = 2.dp
