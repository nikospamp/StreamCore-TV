package com.pampoukidis.streamcoretv.feature.profiles.tv.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorAction
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorFormUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorScreenUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TvProfileEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun eligibleProfileShowsDeleteAndDispatchesRequest() {
        val actions = mutableListOf<ProfileEditorAction>()
        setScreen(onAction = actions::add)

        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorDeleteButton)
            .assertIsDisplayed()
            .performClick()

        assertEquals(listOf(ProfileEditorAction.RequestDeleteProfile), actions)
    }

    @Test
    fun protectedProfileDoesNotExposeDelete() {
        setScreen(profile = ProfilesPreviewData.profiles.first { !it.canDelete })

        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorDeleteButton)
            .assertDoesNotExist()
    }

    @Test
    fun initialFocusStartsAtDisplayNameAndActionsTraverseCancelSaveDelete() {
        setScreen()

        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorDisplayNameField)
            .assertIsFocused()
        composeRule
            .onNodeWithTag(
                ProfilesTestTags.EditorParentalLevelOptionPrefix +
                        ProfilesPreviewData.parentalLevels.last().id,
            )
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.DirectionDown) }
        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorCancelButton)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorSubmitButton)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorDeleteButton)
            .assertIsFocused()
    }

    @Test
    fun confirmationStartsOnCancelMovesRightToDeleteAndCancelRestoresDeleteFocus() {
        val profile = ProfilesPreviewData.profiles.first { it.canDelete }
        var state by mutableStateOf(editorState(profile = profile))
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvProfileEditorScreen(
                    state = state,
                    onAction = { action ->
                        when (action) {
                            ProfileEditorAction.RequestDeleteProfile -> {
                                state = state.copy(pendingDeleteProfile = profile)
                            }

                            ProfileEditorAction.DismissDeleteConfirmation -> {
                                state = state.copy(pendingDeleteProfile = null)
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }

        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorDeleteButton)
            .performClick()
        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorCancelDeleteButton)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule
            .onNodeWithTag(ProfilesTestTags.ConfirmDeleteButton)
            .assertIsFocused()
        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorCancelDeleteButton)
            .performClick()

        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorDeleteButton)
            .assertIsFocused()
    }

    @Test
    fun backDismissesConfirmationAndRestoresDeleteFocus() {
        val profile = ProfilesPreviewData.profiles.first { it.canDelete }
        var state by mutableStateOf(editorState(profile = profile))
        setStatefulScreen(
            stateProvider = { state },
            onAction = { action ->
                when (action) {
                    ProfileEditorAction.RequestDeleteProfile -> {
                        state = state.copy(pendingDeleteProfile = profile)
                    }

                    ProfileEditorAction.DismissDeleteConfirmation -> {
                        state = state.copy(pendingDeleteProfile = null)
                    }

                    else -> Unit
                }
            },
        )

        composeRule.onNodeWithTag(ProfilesTestTags.EditorDeleteButton).performClick()
        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorCancelDeleteButton)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Back) }

        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorDeleteButton)
            .assertIsFocused()
    }

    @Test
    fun deleteFailureStateRestoresDeleteFocus() {
        val profile = ProfilesPreviewData.profiles.first { it.canDelete }
        var state by mutableStateOf(
            editorState(
                profile = profile,
                isSaving = true,
                pendingDeleteProfile = profile,
            ),
        )
        setStatefulScreen(
            stateProvider = { state },
            onAction = {},
        )

        composeRule.runOnIdle {
            state = state.copy(
                isSaving = false,
                pendingDeleteProfile = null,
            )
        }

        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorDeleteButton)
            .assertIsFocused()
    }

    @Test
    fun savingConfirmationDisablesBothActions() {
        val profile = ProfilesPreviewData.profiles.first { it.canDelete }
        setScreen(
            profile = profile,
            isSaving = true,
            pendingDeleteProfile = profile,
        )

        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorCancelDeleteButton)
            .assertIsNotEnabled()
        composeRule
            .onNodeWithTag(ProfilesTestTags.ConfirmDeleteButton)
            .assertIsNotEnabled()
    }

    private fun setScreen(
        profile: ProfileModel = ProfilesPreviewData.profiles.first { it.canDelete },
        isSaving: Boolean = false,
        pendingDeleteProfile: ProfileModel? = null,
        onAction: (ProfileEditorAction) -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvProfileEditorScreen(
                    state = editorState(
                        profile = profile,
                        isSaving = isSaving,
                        pendingDeleteProfile = pendingDeleteProfile,
                    ),
                    onAction = onAction,
                )
            }
        }
    }

    private fun setStatefulScreen(
        stateProvider: () -> ProfileEditorScreenUiState,
        onAction: (ProfileEditorAction) -> Unit,
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvProfileEditorScreen(
                    state = stateProvider(),
                    onAction = onAction,
                )
            }
        }
    }

    private fun editorState(
        profile: ProfileModel,
        isSaving: Boolean = false,
        pendingDeleteProfile: ProfileModel? = null,
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
            pendingDeleteProfile = pendingDeleteProfile,
        )
    }
}
