package com.pampoukidis.streamcoretv.feature.profiles.web.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
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
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileValidationResult

@Composable
fun WebProfileEditorScreen(
    state: ProfileEditorScreenUiState,
    onAction: (ProfileEditorAction) -> Unit,
    modifier: Modifier = Modifier,
    backendErrorMessage: String? = null,
) {
    val nameFocus = remember { FocusRequester() }
    val firstAvatarFocus = remember { FocusRequester() }
    val ready = !state.isLoading && state.editor != null && state.editorOptions != null
    val canDelete = state.mode == ProfileEditorMode.Edit && state.profile?.canDelete == true
    val deleteConfirmationVisible = state.pendingDeleteProfile != null
    var actionFocusRequest by remember { mutableStateOf<WebProfileEditorActionTarget?>(null) }

    LaunchedEffect(ready) {
        if (ready) {
            androidx.compose.runtime.withFrameNanos { }
            firstAvatarFocus.requestFocus()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .webEscape {
                onAction(
                    if (deleteConfirmationVisible) {
                        ProfileEditorAction.DismissDeleteConfirmation
                    } else {
                        ProfileEditorAction.Cancel
                    },
                )
            }
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
                    initialAvatarFocus = firstAvatarFocus,
                    actionFocusRequest = actionFocusRequest,
                    onActionFocusRequestConsumed = { actionFocusRequest = null },
                    onActionFocusRequested = { target -> actionFocusRequest = target },
                    onAction = onAction,
                    backendErrorMessage = backendErrorMessage,
                )
            }
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

@Composable
private fun WebProfileEditorContent(
    state: ProfileEditorScreenUiState,
    editor: ProfileEditorFormUiState,
    options: ProfileEditorOptionsModel,
    nameFocus: FocusRequester,
    initialAvatarFocus: FocusRequester,
    actionFocusRequest: WebProfileEditorActionTarget?,
    onActionFocusRequestConsumed: () -> Unit,
    onActionFocusRequested: (WebProfileEditorActionTarget) -> Unit,
    onAction: (ProfileEditorAction) -> Unit,
    backendErrorMessage: String?,
) {
    val canDelete = state.mode == ProfileEditorMode.Edit && state.profile?.canDelete == true
    val avatarFocus = remember(options.avatars.map(ProfileAvatarModel::id), initialAvatarFocus) {
        options.avatars.mapIndexed { index, avatar ->
            avatar.id to if (index == 0) initialAvatarFocus else FocusRequester()
        }.toMap()
    }
    val maturityFocus = remember(options.parentalLevels.map { level -> level.id }) {
        options.parentalLevels.associate { level -> level.id to FocusRequester() }
    }
    val firstAvatarFocus = options.avatars.firstOrNull()?.let { avatarFocus[it.id] } ?: nameFocus
    val firstMaturityFocus = options.parentalLevels.firstOrNull()?.let { maturityFocus[it.id] } ?: nameFocus
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
            backendErrorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { error(message) },
                )
            }
            val displayNameError = editor.validation.displayNameError?.message()
            WebProfileNameField(
                value = editor.draft.displayName,
                onValueChange = { onAction(ProfileEditorAction.DisplayNameChanged(it)) },
                onSubmit = { onAction(ProfileEditorAction.Submit) },
                enabled = !state.isSaving,
                errorMessage = displayNameError,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (displayNameError == null) {
                            StreamCoreWebDimens.ControlHeight
                        } else {
                            StreamCoreWebDimens.ControlHeight + StreamCoreWebDimens.HtmlInputErrorHeight
                        },
                    )
                    .focusRequester(nameFocus)
                    .focusProperties { down = firstAvatarFocus }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                            firstAvatarFocus.requestFocus()
                            true
                        } else {
                            false
                        }
                    }
                    .then(if (displayNameError != null) Modifier.semantics { error(displayNameError) } else Modifier)
                    .testTag(ProfilesTestTags.EditorDisplayNameField),
            )
            OptionSection(title = "Avatar") {
                options.avatars.forEachIndexed { index, avatar ->
                    val requester = requireNotNull(avatarFocus[avatar.id])
                    val previous = options.avatars.getOrNull(index - 1)?.let { avatarFocus[it.id] }
                    val next = options.avatars.getOrNull(index + 1)?.let { avatarFocus[it.id] }
                    AvatarOption(
                        avatar = avatar,
                        selected = editor.draft.avatarId == avatar.id,
                        enabled = !state.isSaving,
                        onClick = { onAction(ProfileEditorAction.AvatarChanged(avatar.id)) },
                        modifier = Modifier
                            .focusRequester(requester)
                            .focusProperties {
                                up = nameFocus
                                down = firstMaturityFocus
                                if (previous != null) left = previous
                                if (next != null) right = next
                            }
                            .editorDirectionalFocus(
                                up = nameFocus,
                                down = firstMaturityFocus,
                                left = previous,
                                right = next,
                            ),
                    )
                }
            }
            editor.validation.avatarError?.let { error -> ValidationError(error.message()) }
            OptionSection(title = "Maturity") {
                options.parentalLevels.forEachIndexed { index, level ->
                    val requester = requireNotNull(maturityFocus[level.id])
                    val previous = options.parentalLevels.getOrNull(index - 1)?.let { maturityFocus[it.id] }
                    val next = options.parentalLevels.getOrNull(index + 1)?.let { maturityFocus[it.id] }
                    StreamCoreWebButton(
                        text = level.label,
                        onClick = { onAction(ProfileEditorAction.ParentalLevelChanged(level.id)) },
                        enabled = !state.isSaving,
                        variant = if (editor.draft.parentalLevelId == level.id) {
                            StreamCoreWebButtonVariant.Primary
                        } else {
                            StreamCoreWebButtonVariant.Secondary
                        },
                        modifier = Modifier
                            .focusRequester(requester)
                            .focusProperties {
                                up = firstAvatarFocus
                                if (previous != null) left = previous
                                if (next != null) right = next
                            }
                            .editorDirectionalFocus(
                                up = firstAvatarFocus,
                                down = null,
                                left = previous,
                                right = next,
                            )
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                                    onActionFocusRequested(WebProfileEditorActionTarget.Cancel)
                                    true
                                } else {
                                    false
                                }
                            }
                            .semantics { selected = editor.draft.parentalLevelId == level.id }
                            .testTag(ProfilesTestTags.EditorParentalLevelOptionPrefix + level.id),
                    )
                }
            }
            editor.validation.parentalLevelError?.let { error -> ValidationError(error.message()) }
            WebProfileEditorActionStrip(
                isSaving = state.isSaving,
                canDelete = canDelete,
                focusRequest = actionFocusRequest,
                onFocusRequestConsumed = onActionFocusRequestConsumed,
                onMoveUp = { firstMaturityFocus.requestFocus() },
                onDisplayNameCommit = { value ->
                    onAction(ProfileEditorAction.DisplayNameChanged(value))
                },
                onCancel = { onAction(ProfileEditorAction.Cancel) },
                onSave = { onAction(ProfileEditorAction.Submit) },
                onDelete = { onAction(ProfileEditorAction.RequestDeleteProfile) },
                modifier = Modifier
                    .align(Alignment.End)
                    .fillMaxWidth()
                    .height(StreamCoreWebDimens.ControlHeight),
            )
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
    modifier: Modifier = Modifier,
) {
    StreamCoreWebProfileCard(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        modifier = modifier
            .size(StreamCoreWebDimens.AvatarOptionSize)
            .semantics {
                role = Role.Button
                contentDescription = "Avatar ${avatar.id.substringAfterLast('-')}"
                this.selected = selected
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

private fun Modifier.editorDirectionalFocus(
    up: FocusRequester? = null,
    down: FocusRequester? = null,
    left: FocusRequester? = null,
    right: FocusRequester? = null,
): Modifier {
    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) {
            return@onPreviewKeyEvent false
        }
        val requester = when (event.key) {
            Key.DirectionUp -> up
            Key.DirectionDown -> down
            Key.DirectionLeft -> left
            Key.DirectionRight -> right
            else -> null
        }
        if (requester == null) {
            false
        } else {
            requester.requestFocus()
            true
        }
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
