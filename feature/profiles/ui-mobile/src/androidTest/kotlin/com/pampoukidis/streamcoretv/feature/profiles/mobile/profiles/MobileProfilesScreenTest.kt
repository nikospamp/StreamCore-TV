package com.pampoukidis.streamcoretv.feature.profiles.mobile.profiles

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesMode
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MobileProfilesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectionMode_profileClickDispatchesSelection() {
        val actions = mutableListOf<ProfilesAction>()
        val profile = ProfilesPreviewData.profiles.first()

        setScreen(onAction = actions::add)

        composeRule
            .onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + profile.id)
            .performClick()

        assertEquals(listOf(ProfilesAction.SelectProfile(profile.id)), actions)
    }

    @Test
    fun pendingSelection_disablesEveryInteractiveTile() {
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

    @Test
    fun manageMode_profileClickEditsAndUsesEditSemantics() {
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
    fun addTileClickOpensCreateFlow() {
        var createClicks = 0
        setScreen(onCreateProfile = { createClicks++ })

        composeRule
            .onNodeWithTag(ProfilesTestTags.AddProfileButton)
            .performClick()

        assertEquals(1, createClicks)
    }

    @Test
    fun selectorDoesNotExposeDeleteAction() {
        val deletableProfile = ProfilesPreviewData.profiles.first { it.canDelete }
        setScreen()

        composeRule
            .onAllNodesWithTag(ProfilesTestTags.DeleteProfileButtonPrefix + deletableProfile.id)
            .assertCountEquals(0)
    }

    @Test
    fun manageButtonDispatchesModeAction() {
        val actions = mutableListOf<ProfilesAction>()
        setScreen(onAction = actions::add)

        composeRule
            .onNodeWithTag(ProfilesTestTags.ManageProfilesButton)
            .performClick()

        assertTrue(actions.single() == ProfilesAction.ManageProfiles)
    }

    @Test
    fun kidsProfile_showsKidsChipOnAvatar() {
        val kidsProfile = ProfilesPreviewData.profiles.first { it.isKidsProfile }
        setScreen()

        composeRule
            .onAllNodesWithTag(ProfilesTestTags.KidsChipPrefix + kidsProfile.id)
            .assertCountEquals(1)
    }

    private fun setScreen(
        state: ProfilesUiState = contentState,
        onAction: (ProfilesAction) -> Unit = {},
        onCreateProfile: () -> Unit = {},
        onEditProfile: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                MobileProfilesScreen(
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
