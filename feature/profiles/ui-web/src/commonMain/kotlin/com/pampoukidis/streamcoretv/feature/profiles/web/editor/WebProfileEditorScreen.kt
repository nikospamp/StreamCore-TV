package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.components.StreamCoreSettingsSwitchRow
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreDimens
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebActionSurface
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebDimens
import com.pampoukidis.streamcoretv.core.ui.web.webEscape
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorContent
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorFormUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorLayout
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorModifiers
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorScreenUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesBackdrop
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileFieldError
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileValidationResult

@Composable
fun WebProfileEditorScreen(
    state: ProfileEditorScreenUiState,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
    backendErrorMessage: String? = null,
) {
    val avatarFocus = remember { FocusRequester() }
    var avatarPickerVisible by rememberSaveable { mutableStateOf(false) }
    var restoreAvatarFocus by remember { mutableStateOf(false) }
    var restoreDeleteFocus by remember { mutableStateOf(false) }
    LaunchedEffect(state.pendingDeleteProfile) {
        if (state.pendingDeleteProfile != null) restoreDeleteFocus = true
    }
    val editor = state.editor
    val options = state.editorOptions
    val ready = !state.isLoading && editor != null && options != null
    val modalVisible = avatarPickerVisible || state.pendingDeleteProfile != null

    LaunchedEffect(ready, avatarPickerVisible) {
        if (avatarPickerVisible) {
            restoreAvatarFocus = true
        } else if (ready && restoreAvatarFocus) {
            avatarFocus.requestFocus()
            restoreAvatarFocus = false
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
            .webEscape {
                if (avatarPickerVisible) {
                    avatarPickerVisible = false
                } else if (state.pendingDeleteProfile != null) {
                    onAction(ProfileEditorAction.DismissDeleteConfirmation)
                } else {
                    onAction(ProfileEditorAction.Cancel)
                }
            }
            .testTag(ProfilesTestTags.EditorRoot),
    ) {
        ProfilesBackdrop(modifier = Modifier.matchParentSize())
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .padding(StreamCoreDimens.Tv.Screen.HorizontalPadding)
                .widthIn(max = StreamCoreDimens.Tv.Profiles.EditorPanelWidth)
                .fillMaxWidth()
                .heightIn(max = StreamCoreDimens.Tv.Profiles.EditorPanelMaxHeight)
                .fillMaxHeight()
                .then(if (avatarPickerVisible) Modifier.clearAndSetSemantics { } else Modifier),
        ) {
            ProfileEditorContent(
                state = state,
                onAction = onAction,
                onAvatarClick = { avatarPickerVisible = true },
                errorMessage = backendErrorMessage,
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
                modifiers = ProfileEditorModifiers(avatar = Modifier.focusRequester(avatarFocus)),
                displayNameControl = { value, onValueChange, enabled, errorMessage, controlModifier ->
                    val fieldModifier = controlModifier.height(
                        StreamCoreWebDimens.ControlHeight + StreamCoreDimens.Spacing.Medium +
                            if (errorMessage == null) 0.dp else StreamCoreWebDimens.HtmlInputErrorHeight,
                    )
                    if (avatarPickerVisible) {
                        Box(modifier = fieldModifier)
                    } else {
                        WebProfileNameField(
                            value = value,
                            onValueChange = onValueChange,
                            onSubmit = { onAction(ProfileEditorAction.Submit) },
                            enabled = enabled && !modalVisible,
                            errorMessage = errorMessage,
                            modifier = fieldModifier,
                        )
                    }
                },
                closeControl = { onClick, enabled, controlModifier ->
                    if (!avatarPickerVisible) WebProfileEditorActionButton(
                        target = WebProfileEditorActionTarget.Cancel,
                        text = "Close",
                        enabled = enabled && !modalVisible,
                        onClick = onClick,
                        onDisplayNameCommit = {},
                        modifier = controlModifier.fillMaxWidth().height(StreamCoreWebDimens.ControlHeight),
                    )
                },
                saveControl = { text, onClick, enabled, controlModifier ->
                    if (!avatarPickerVisible) WebProfileEditorActionButton(
                        target = WebProfileEditorActionTarget.Save,
                        text = text,
                        enabled = enabled && !modalVisible,
                        onClick = onClick,
                        onDisplayNameCommit = { onAction(ProfileEditorAction.DisplayNameChanged(it)) },
                        modifier = controlModifier.fillMaxWidth().height(StreamCoreWebDimens.ControlHeight),
                    )
                },
                avatarControl = { onClick, enabled, controlModifier, content ->
                    StreamCoreWebActionSurface(
                        onClick = onClick,
                        enabled = enabled && !modalVisible,
                        modifier = controlModifier.semantics { contentDescription = "Change profile avatar" },
                        content = content,
                    )
                },
                kidsControl = { title, supportingText, checked, enabled, onCheckedChange, controlModifier ->
                    StreamCoreSettingsSwitchRow(
                        title = title,
                        supportingText = supportingText,
                        checked = checked,
                        enabled = enabled && !modalVisible,
                        onCheckedChange = onCheckedChange,
                        modifier = controlModifier,
                    )
                },
                deleteControl = { text, onClick, enabled, controlModifier ->
                    if (!avatarPickerVisible) WebProfileEditorActionButton(
                        target = WebProfileEditorActionTarget.Delete,
                        requestFocus = restoreDeleteFocus && !modalVisible,
                        text = text,
                        enabled = enabled && !modalVisible,
                        onClick = onClick,
                        onDisplayNameCommit = {},
                        modifier = controlModifier.height(StreamCoreWebDimens.ControlHeight),
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (avatarPickerVisible && editor != null && options != null) {
            WebAvatarPickerOverlay(
                avatars = options.avatars,
                selectedAvatarId = editor.draft.avatarId,
                onAvatarSelected = {
                    onAction(ProfileEditorAction.AvatarChanged(it))
                    avatarPickerVisible = false
                },
                onDismissRequest = { avatarPickerVisible = false },
            )
        }
    }
    state.pendingDeleteProfile?.let { profile ->
        WebProfileEditorDeleteDialog(
            profile = profile,
            isSaving = state.isSaving,
            onConfirm = { onAction(ProfileEditorAction.ConfirmDeleteProfile) },
            onDismiss = { onAction(ProfileEditorAction.DismissDeleteConfirmation) },
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfileEditorScreenPreview() {
    val profile = ProfilesPreviewData.profiles[1]
    StreamCoreTheme(darkTheme = true) {
        WebProfileEditorScreen(
            state = editorPreviewState(profile = profile),
            onAction = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfileEditorLoadingPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebProfileEditorScreen(
            state = ProfileEditorScreenUiState(isLoading = true),
            onAction = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfileEditorValidationErrorPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebProfileEditorScreen(
            state = editorPreviewState(
                profile = null,
                mode = ProfileEditorMode.Create,
                displayName = "",
                validation = ProfileValidationResult(
                    displayNameError = ProfileFieldError.Blank,
                    avatarError = ProfileFieldError.MissingSelection,
                    parentalLevelError = ProfileFieldError.MissingSelection,
                ),
            ),
            onAction = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfileEditorBackendErrorPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebProfileEditorScreen(
            state = editorPreviewState(profile = ProfilesPreviewData.profiles[1]),
            onAction = {},
            backendErrorMessage = "The profile could not be saved. Check the connection and retry.",
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfileEditorDeleteConfirmationPreview() {
    val profile = ProfilesPreviewData.profiles.first { it.canDelete }
    StreamCoreTheme(darkTheme = true) {
        WebProfileEditorScreen(
            state = editorPreviewState(profile = profile, pendingDeleteProfile = profile),
            onAction = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun WebProfileEditorLongTextPreview() {
    StreamCoreTheme(darkTheme = true) {
        WebProfileEditorScreen(
            state = editorPreviewState(
                profile = ProfilesPreviewData.profiles[1],
                displayName = "A very long localized profile display name that verifies safe field overflow",
            ),
            onAction = {},
        )
    }
}

private fun editorPreviewState(
    profile: ProfileModel?,
    mode: ProfileEditorMode = ProfileEditorMode.Edit,
    displayName: String = profile?.displayName.orEmpty(),
    validation: ProfileValidationResult = ProfileValidationResult(),
    pendingDeleteProfile: ProfileModel? = null,
): ProfileEditorScreenUiState {
    val avatarId = profile?.avatar?.id ?: ProfilesPreviewData.avatars.first().id
    val parentalLevelId = profile?.parentalLevel?.id ?: ProfilesPreviewData.parentalLevels.first().id
    return ProfileEditorScreenUiState(
        mode = mode,
        isLoading = false,
        editorOptions = ProfilesPreviewData.editorOptions.copy(
            avatars = ProfilesPreviewData.avatars.take(6),
        ),
        editor = ProfileEditorFormUiState(
            mode = mode,
            draft = ProfileDraftModel(
                profileId = profile?.id,
                displayName = displayName,
                avatarId = avatarId,
                parentalLevelId = parentalLevelId,
            ),
            validation = validation,
        ),
        profile = profile,
        pendingDeleteProfile = pendingDeleteProfile,
    )
}
