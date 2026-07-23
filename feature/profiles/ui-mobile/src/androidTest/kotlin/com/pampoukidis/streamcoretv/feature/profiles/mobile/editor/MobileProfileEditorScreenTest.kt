package com.pampoukidis.streamcoretv.feature.profiles.mobile.editor

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

class MobileProfileEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun avatarClick_opensPopupAndSelectionDispatchesAvatarChange() {
        val actions = mutableListOf<ProfileEditorAction>()
        val selectedAvatar = ProfilesPreviewData.avatars[1]
        setScreen(onAction = actions::add)

        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorAvatarButton)
            .performClick()
        composeRule
            .onAllNodesWithTag(ProfilesTestTags.EditorAvatarDialog)
            .assertCountEquals(1)
        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorAvatarOptionPrefix + selectedAvatar.id)
            .performClick()

        assertEquals(listOf(ProfileEditorAction.AvatarChanged(selectedAvatar.id)), actions)
        composeRule
            .onAllNodesWithTag(ProfilesTestTags.EditorAvatarDialog)
            .assertCountEquals(0)
    }

    @Test
    fun kidsSwitch_dispatchesKidsProfileChange() {
        val actions = mutableListOf<ProfileEditorAction>()
        setScreen(onAction = actions::add)

        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorKidsSwitch)
            .performClick()

        assertEquals(listOf(ProfileEditorAction.KidsProfileChanged(true)), actions)
    }

    @Test
    fun deletableEditProfile_deleteButtonDispatchesRequest() {
        val actions = mutableListOf<ProfileEditorAction>()
        setScreen(onAction = actions::add)

        composeRule
            .onNodeWithTag(ProfilesTestTags.EditorDeleteButton)
            .performScrollTo()
            .performClick()

        assertEquals(listOf(ProfileEditorAction.RequestDeleteProfile), actions)
    }

    @Test
    fun pendingDeleteProfile_confirmationDispatchesConfirm() {
        val actions = mutableListOf<ProfileEditorAction>()
        setScreen(
            state = contentState.copy(pendingDeleteProfile = deletableProfile),
            onAction = actions::add,
        )

        composeRule
            .onNodeWithTag(ProfilesTestTags.ConfirmDeleteButton)
            .performClick()

        assertEquals(listOf(ProfileEditorAction.ConfirmDeleteProfile), actions)
    }

    @Test
    fun createProfile_doesNotExposeDeleteAction() {
        setScreen(
            state = contentState.copy(
                mode = ProfileEditorMode.Create,
                profile = null,
            ),
        )

        composeRule
            .onAllNodesWithTag(ProfilesTestTags.EditorDeleteButton)
            .assertCountEquals(0)
    }

    private fun setScreen(
        state: ProfileEditorScreenUiState = contentState,
        onAction: (ProfileEditorAction) -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                MobileProfileEditorScreen(
                    state = state,
                    onAction = onAction,
                )
            }
        }
    }

    private companion object {
        val deletableProfile = ProfilesPreviewData.profiles.first { it.canDelete }
        val draft = ProfileDraftModel(
            profileId = deletableProfile.id,
            displayName = deletableProfile.displayName,
            avatarId = deletableProfile.avatar.id,
            parentalLevelId = deletableProfile.parentalLevel.id,
        )
        val contentState = ProfileEditorScreenUiState(
            mode = ProfileEditorMode.Edit,
            isLoading = false,
            editorOptions = ProfilesPreviewData.editorOptions,
            profile = deletableProfile,
            editor = ProfileEditorFormUiState(
                mode = ProfileEditorMode.Edit,
                draft = draft,
            ),
        )
    }
}
