package com.pampoukidis.streamcoretv.feature.profiles.tv.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreCloseIcon
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvActionSurface
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvIconButton
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvSettingsSwitchRow
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreTvTextButton
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.utils.PreviewTV
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorContent
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorFormUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorLayout
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorModifiers
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorScreenUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.AndroidProfilesBackdrop
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TvProfileEditorScreen(
    state: ProfileEditorScreenUiState,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val avatarFocusRequester = remember { FocusRequester() }
    val displayNameFocusRequester = remember { FocusRequester() }
    val kidsFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }
    val saveFocusRequester = remember { FocusRequester() }
    val deleteFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.isImeVisible
    var isDisplayNameFocused by remember { mutableStateOf(false) }
    var avatarPickerVisible by rememberSaveable { mutableStateOf(false) }
    var restoreAvatarFocus by remember { mutableStateOf(false) }
    var restoreDeleteFocus by remember { mutableStateOf(false) }
    val editor = state.editor
    val options = state.editorOptions
    val isEditorReady = !state.isLoading && editor != null && options != null
    val canDelete = state.mode == ProfileEditorMode.Edit && state.profile?.canDelete == true
    val canSave = editor?.hasChanges == true && editor.validation.isValid && !state.isSaving
    val headerActionFocusRequester = if (canSave) saveFocusRequester else closeFocusRequester
    val isDeleteConfirmationVisible = state.pendingDeleteProfile != null

    LaunchedEffect(isEditorReady) {
        if (isEditorReady && !avatarPickerVisible && !isDeleteConfirmationVisible) {
            displayNameFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(avatarPickerVisible, state.isSaving) {
        if (avatarPickerVisible) {
            restoreAvatarFocus = true
        } else if (restoreAvatarFocus && isEditorReady && !state.isSaving) {
            avatarFocusRequester.requestFocus()
            restoreAvatarFocus = false
        }
    }
    LaunchedEffect(isDeleteConfirmationVisible, canDelete, state.isSaving) {
        if (isDeleteConfirmationVisible) {
            restoreDeleteFocus = true
        } else if (restoreDeleteFocus && canDelete && !state.isSaving) {
            deleteFocusRequester.requestFocus()
            restoreDeleteFocus = false
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize().testTag(ProfilesTestTags.EditorRoot),
    ) {
        AndroidProfilesBackdrop(modifier = Modifier.matchParentSize())
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .padding(
                    horizontal = StreamCoreDimens.Tv.Screen.HorizontalPadding,
                    vertical = StreamCoreDimens.Tv.Screen.VerticalPadding,
                )
                .widthIn(max = StreamCoreDimens.Tv.Profiles.EditorPanelWidth)
                .fillMaxWidth()
                .heightIn(max = StreamCoreDimens.Tv.Profiles.EditorPanelMaxHeight)
                .fillMaxHeight(),
        ) {
            ProfileEditorContent(
                state = state,
                onAction = onAction,
                onAvatarClick = { avatarPickerVisible = true },
                onDisplayNameDone = {
                    keyboardController?.hide()
                    kidsFocusRequester.requestFocus()
                },
                layout = ProfileEditorLayout(
                    headerHeight = StreamCoreDimens.Tv.Profiles.HeaderHeight,
                    headerSideWidth = StreamCoreDimens.Tv.Profiles.EditorTopBarSideWidth,
                    contentHorizontalPadding = StreamCoreDimens.Tv.Panel.Padding,
                    contentVerticalPadding = StreamCoreDimens.Spacing.Large,
                    contentSpacing = StreamCoreDimens.Spacing.Large,
                    avatarContainerSize = StreamCoreDimens.Tv.Profiles.EditorAvatarContainerSize,
                    avatarSize = StreamCoreDimens.Tv.Profiles.EditorAvatarSize,
                    avatarBadgeSize = StreamCoreDimens.Tv.Profiles.BadgeSize,
                    avatarBadgeOffset = StreamCoreDimens.Tv.Profiles.BadgeOffsetY,
                    avatarCaption = "Select to change",
                ),
                modifiers = ProfileEditorModifiers(
                    close = Modifier
                        .focusRequester(closeFocusRequester)
                        .focusProperties {
                            right = if (canSave) saveFocusRequester else avatarFocusRequester
                            down = avatarFocusRequester
                        }
                        .testTag(ProfilesTestTags.EditorCancelButton),
                    save = Modifier
                        .focusRequester(saveFocusRequester)
                        .focusProperties {
                            left = closeFocusRequester
                            down = avatarFocusRequester
                        },
                    avatar = Modifier
                        .focusRequester(avatarFocusRequester)
                        .focusProperties {
                            up = headerActionFocusRequester
                            down = displayNameFocusRequester
                        },
                    displayName = Modifier
                        .focusRequester(displayNameFocusRequester)
                        .onFocusChanged { isDisplayNameFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            val target = when (event.key) {
                                Key.DirectionUp -> avatarFocusRequester
                                Key.DirectionDown -> kidsFocusRequester
                                else -> null
                            }
                            // Preserve caret movement while editing; otherwise traverse the form.
                            if (isDisplayNameFocused && !isImeVisible && target != null) {
                                if (event.type == KeyEventType.KeyDown) {
                                    target.requestFocus()
                                }
                                true
                            } else {
                                false
                            }
                        }
                        .focusProperties {
                            up = avatarFocusRequester
                            down = kidsFocusRequester
                        },
                    kids = Modifier
                        .focusRequester(kidsFocusRequester)
                        .focusProperties {
                            up = displayNameFocusRequester
                            down = if (canDelete) deleteFocusRequester else headerActionFocusRequester
                        },
                    delete = Modifier
                        .focusRequester(deleteFocusRequester)
                        .focusProperties {
                            up = kidsFocusRequester
                            down = headerActionFocusRequester
                        },
                ),
                closeControl = { onClick, enabled, controlModifier ->
                    StreamCoreTvIconButton(
                        onClick = onClick,
                        enabled = enabled,
                        modifier = controlModifier.semantics { contentDescription = "Close" },
                    ) {
                        StreamCoreCloseIcon()
                    }
                },
                saveControl = { text, onClick, enabled, controlModifier ->
                    StreamCoreTvTextButton(
                        text = text,
                        onClick = onClick,
                        enabled = enabled,
                        modifier = controlModifier,
                    )
                },
                avatarControl = { onClick, enabled, controlModifier, content ->
                    StreamCoreTvActionSurface(
                        onClick = onClick,
                        enabled = enabled,
                        modifier = controlModifier,
                        content = content,
                    )
                },
                kidsControl = { title, supportingText, checked, enabled, onCheckedChange, controlModifier ->
                    StreamCoreTvSettingsSwitchRow(
                        title = title,
                        supportingText = supportingText,
                        checked = checked,
                        enabled = enabled,
                        onCheckedChange = onCheckedChange,
                        modifier = controlModifier,
                    )
                },
                deleteControl = { text, onClick, enabled, controlModifier ->
                    StreamCoreTvTextButton(
                        text = text,
                        onClick = onClick,
                        enabled = enabled,
                        contentColor = MaterialTheme.colorScheme.error,
                        contentAlignment = Alignment.CenterHorizontally,
                        modifier = controlModifier,
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (avatarPickerVisible && editor != null && options != null) {
        TvAvatarPickerDialog(
            avatars = options.avatars,
            selectedAvatarId = editor.draft.avatarId,
            onAvatarSelected = { avatarId ->
                onAction(ProfileEditorAction.AvatarChanged(avatarId))
                avatarPickerVisible = false
            },
            onDismissRequest = { avatarPickerVisible = false },
        )
    }

    TvProfileDeleteConfirmationDialog(
        profile = state.pendingDeleteProfile,
        isSaving = state.isSaving,
        onConfirmDelete = { onAction(ProfileEditorAction.ConfirmDeleteProfile) },
        onDismiss = { onAction(ProfileEditorAction.DismissDeleteConfirmation) },
    )
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
