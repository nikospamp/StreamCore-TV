package com.pampoukidis.streamcoretv.feature.profiles.common.editor

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
import androidx.compose.ui.tooling.preview.Preview
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCloseButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreEditIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreProfileArtwork
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSettingsSwitchRow
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileFieldError

/** Shared editor rendering and action wiring. Window placement, dialogs and focus belong to callers. */
@Composable
fun ProfileEditorContent(
    state: ProfileEditorScreenUiState,
    onAction: (ProfileEditorAction) -> Unit,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
    layout: ProfileEditorLayout = ProfileEditorLayout(),
    modifiers: ProfileEditorModifiers = ProfileEditorModifiers(),
    onDisplayNameDone: (() -> Unit)? = null,
    closeControl: @Composable (onClick: () -> Unit, enabled: Boolean, modifier: Modifier) -> Unit =
        { onClick, enabled, controlModifier ->
            StreamCoreCloseButton(onClick = onClick, enabled = enabled, modifier = controlModifier)
        },
    saveControl: @Composable (text: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) -> Unit =
        { text, onClick, enabled, controlModifier ->
            StreamCoreTextButton(text = text, onClick = onClick, enabled = enabled, modifier = controlModifier)
        },
    avatarControl: @Composable (
        onClick: () -> Unit,
        enabled: Boolean,
        modifier: Modifier,
        content: @Composable () -> Unit,
    ) -> Unit = { onClick, enabled, controlModifier, content ->
        Box(
            modifier = controlModifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        ) {
            content()
        }
    },
    kidsControl: @Composable (
        title: String,
        supportingText: String,
        checked: Boolean,
        enabled: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        modifier: Modifier,
    ) -> Unit = { title, supportingText, checked, enabled, onCheckedChange, controlModifier ->
        StreamCoreSettingsSwitchRow(
            title = title,
            supportingText = supportingText,
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = controlModifier,
        )
    },
    deleteControl: @Composable (text: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) -> Unit =
        { text, onClick, enabled, controlModifier ->
            StreamCoreTextButton(
                text = text,
                onClick = onClick,
                enabled = enabled,
                contentColor = MaterialTheme.colorScheme.error,
                modifier = controlModifier,
            )
        },
) {
    val editor = state.editor
    val options = state.editorOptions
    val title = when (state.mode) {
        ProfileEditorMode.Create -> "Create profile"
        ProfileEditorMode.Edit -> "Edit profile"
    }
    val saveEnabled = editor != null && editor.hasChanges && editor.validation.isValid && !state.isSaving
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = layout.headerHeight)
                .padding(horizontal = layout.headerHorizontalPadding),
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.width(layout.headerSideWidth),
            ) {
                closeControl({ onAction(ProfileEditorAction.Cancel) }, !state.isSaving, modifiers.close)
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
                modifier = Modifier.width(layout.headerSideWidth),
            ) {
                saveControl(
                    if (state.isSaving) "Saving" else "Save",
                    { onAction(ProfileEditorAction.Submit) },
                    saveEnabled,
                    modifiers.save.testTag(ProfilesTestTags.EditorSubmitButton),
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().weight(1f),
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
                else -> {
                    val selectedAvatar = options.avatars.firstOrNull { it.id == editor.draft.avatarId }
                        ?: options.avatars.firstOrNull()
                    val isKidsProfile = options.parentalLevels.firstOrNull {
                        it.id == editor.draft.parentalLevelId
                    }?.isKids == true

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(layout.contentSpacing),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                horizontal = layout.contentHorizontalPadding,
                                vertical = layout.contentVerticalPadding,
                            ),
                    ) {
                        if (selectedAvatar != null) {
                            avatarControl(
                                onAvatarClick,
                                !state.isSaving,
                                modifiers.avatar.testTag(ProfilesTestTags.EditorAvatarButton),
                            ) {
                                ProfileEditorAvatarArtwork(avatar = selectedAvatar, layout = layout)
                            }
                        }
                        OutlinedTextField(
                            value = editor.draft.displayName,
                            onValueChange = { onAction(ProfileEditorAction.DisplayNameChanged(it)) },
                            label = { Text(text = "Username") },
                            singleLine = true,
                            enabled = !state.isSaving,
                            isError = editor.validation.displayNameError != null,
                            supportingText = editor.validation.displayNameError?.let { error ->
                                { Text(text = error.message()) }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { onDisplayNameDone?.invoke() ?: focusManager.clearFocus() },
                            ),
                            modifier = modifiers.displayName
                                .fillMaxWidth()
                                .testTag(ProfilesTestTags.EditorDisplayNameField),
                        )
                        kidsControl(
                            "Kids profile",
                            "Only age-appropriate content",
                            isKidsProfile,
                            !state.isSaving,
                            { onAction(ProfileEditorAction.KidsProfileChanged(it)) },
                            modifiers.kids.testTag(ProfilesTestTags.EditorKidsSwitch),
                        )
                        if (state.profile?.canDelete == true) {
                            deleteControl(
                                "Delete profile",
                                { onAction(ProfileEditorAction.RequestDeleteProfile) },
                                !state.isSaving,
                                modifiers.delete.fillMaxWidth().testTag(ProfilesTestTags.EditorDeleteButton),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditorAvatarArtwork(avatar: ProfileAvatarModel, layout: ProfileEditorLayout) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(StreamCoreDimens.Spacing.Medium),
        modifier = Modifier.padding(StreamCoreDimens.Spacing.Small),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(layout.avatarContainerSize)) {
            Box(
                modifier = Modifier.size(layout.avatarSize).clip(CircleShape)
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
                modifier = Modifier.align(Alignment.BottomEnd)
                    .offset(x = layout.avatarBadgeOffset, y = layout.avatarBadgeOffset)
                    .size(layout.avatarBadgeSize),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    StreamCoreEditIcon(modifier = Modifier.size(StreamCoreDimens.Icon.Medium))
                }
            }
        }
        Text(
            text = layout.avatarCaption,
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

@Preview
@Composable
private fun ProfileEditorContentPreview() {
    StreamCoreTheme(darkTheme = true) {
        Surface {
            ProfileEditorContent(
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
                onAvatarClick = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
