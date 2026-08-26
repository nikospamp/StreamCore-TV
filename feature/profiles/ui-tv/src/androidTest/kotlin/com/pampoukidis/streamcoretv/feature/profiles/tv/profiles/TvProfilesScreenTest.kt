package com.pampoukidis.streamcoretv.feature.profiles.tv.profiles

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TvProfilesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectionMode_initialProfileIsFocusedAndClickDispatchesSelection() {
        val actions = mutableListOf<ProfilesAction>()
        val profile = ProfilesPreviewData.profiles.first()
        setScreen(onAction = actions::add)

        composeRule
            .onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + profile.id)
            .assertIsFocused()
            .performClick()

        assertEquals(listOf(ProfilesAction.SelectProfile(profile.id)), actions)
    }

    @Test
    fun manageMode_profileClickOpensEditor() {
        val editedProfiles = mutableListOf<String>()
        val profile = ProfilesPreviewData.profiles.first()
        setScreen(
            state = contentState.copy(mode = ProfilesMode.Manage),
            onEditProfile = editedProfiles::add,
        )

        composeRule
            .onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + profile.id)
            .assertContentDescriptionEquals("Edit ${profile.displayName} profile")
            .performClick()

        assertEquals(listOf(profile.id), editedProfiles)
    }

    @Test
    fun manageButtonDispatchesModeAction() {
        val actions = mutableListOf<ProfilesAction>()
        setScreen(onAction = actions::add)

        composeRule
            .onNodeWithTag(ProfilesTestTags.ManageProfilesButton)
            .performClick()

        assertEquals(listOf(ProfilesAction.ManageProfiles), actions)
    }

    @Test
    fun addTileClickOpensCreateFlow() {
        var createClicks = 0
        setScreen(onCreateProfile = { createClicks++ })

        composeRule
            .onNodeWithTag(ProfilesTestTags.AddProfileButton)
            .performClick()

        assertEquals(1, createClicks)
    }

    @Test
    fun pendingSelectionDisablesAllPrimaryInteractions() {
        val profile = ProfilesPreviewData.profiles.first()
        setScreen(
            state = contentState.copy(pendingSelectionProfileId = profile.id),
        )

        composeRule
            .onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + profile.id)
            .assertIsNotEnabled()
        composeRule
            .onNodeWithTag(ProfilesTestTags.AddProfileButton)
            .assertIsNotEnabled()
        composeRule
            .onNodeWithTag(ProfilesTestTags.ManageProfilesButton)
            .assertIsNotEnabled()
    }

    private fun setScreen(
        state: ProfilesUiState = contentState,
        onAction: (ProfilesAction) -> Unit = {},
        onCreateProfile: () -> Unit = {},
        onEditProfile: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvProfilesScreen(
                    state = state,
                    onAction = onAction,
                    onCreateProfile = onCreateProfile,
                    onEditProfile = onEditProfile,
                )
            }
        }
    }

    private companion object {
        val contentState = ProfilesUiState(
            isLoading = false,
            profiles = ProfilesPreviewData.profiles,
        )
    }
}
