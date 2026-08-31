package com.pampoukidis.streamcoretv.feature.profiles.tablet.profiles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TabletProfilesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun signOutActionIsVisibleAndDispatchesRequest() {
        var logoutRequests = 0
        setScreen(onLogoutRequested = { logoutRequests++ })

        composeRule
            .onNodeWithTag(ProfilesTestTags.SignOutButton)
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, logoutRequests)
    }

    @Test
    fun logoutInProgressDisablesSignOutAction() {
        setScreen(isLogoutInProgress = true)

        composeRule
            .onNodeWithTag(ProfilesTestTags.SignOutButton)
            .assertIsNotEnabled()
    }

    @Test
    fun profileLoadErrorKeepsSignOutAvailable() {
        setScreen(
            state = ProfilesUiState(
                isLoading = false,
                loadError = AppError.Network(),
            ),
        )

        composeRule
            .onNodeWithTag(ProfilesTestTags.SignOutButton)
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    private fun setScreen(
        state: ProfilesUiState = ProfilesUiState(
            isLoading = false,
            profiles = ProfilesPreviewData.profiles,
        ),
        isLogoutInProgress: Boolean = false,
        onLogoutRequested: () -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TabletProfilesScreen(
                    state = state,
                    onAction = {},
                    onCreateProfile = {},
                    onEditProfile = {},
                    isLogoutInProgress = isLogoutInProgress,
                    onLogoutRequested = onLogoutRequested,
                )
            }
        }
    }
}
