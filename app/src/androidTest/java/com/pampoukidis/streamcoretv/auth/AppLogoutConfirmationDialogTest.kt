package com.pampoukidis.streamcoretv.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.pressKey
import com.pampoukidis.streamcoretv.core.model.general.Platform
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.tv.profiles.TvProfilesScreen
import androidx.test.espresso.Espresso.pressBack
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppLogoutConfirmationDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hiddenDialogDoesNotRender() {
        setDialog(visible = false)

        composeRule
            .onNodeWithTag(AppAuthTestTags.LogoutConfirmation)
            .assertDoesNotExist()
    }

    @Test
    fun confirmationDispatchesAction() {
        var confirms = 0
        setDialog(
            visible = true,
            onConfirm = { confirms++ },
        )

        composeRule
            .onNodeWithTag(AppAuthTestTags.LogoutConfirmation)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(AppAuthTestTags.ConfirmLogoutButton).performClick()

        assertEquals(1, confirms)
    }

    @Test
    fun cancellationDispatchesAction() {
        var dismisses = 0
        setDialog(
            visible = true,
            onDismiss = { dismisses++ },
        )

        composeRule.onNodeWithTag(AppAuthTestTags.CancelLogoutButton).performClick()

        assertEquals(1, dismisses)
    }

    @Test
    fun loadingDisablesDialogActions() {
        setDialog(
            visible = true,
            isLogoutInProgress = true,
        )

        composeRule
            .onNodeWithTag(AppAuthTestTags.ConfirmLogoutButton)
            .assertIsNotEnabled()
            .assertContentDescriptionEquals("Sign out")
        composeRule
            .onNodeWithTag(AppAuthTestTags.CancelLogoutButton)
            .assertIsNotEnabled()
    }

    @Test
    fun tvDialogStartsOnCancelAndHasDeterministicHorizontalOrder() {
        setDialog(
            visible = true,
            platform = Platform.Tv,
        )

        composeRule.onNodeWithTag(AppAuthTestTags.CancelLogoutButton).assertIsFocused()
        composeRule
            .onNodeWithTag(AppAuthTestTags.CancelLogoutButton)
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag(AppAuthTestTags.ConfirmLogoutButton).assertIsFocused()
    }

    @Test
    fun tvCancelAndBackRestoreFocusToInvokingSignOutAction() {
        var isConfirmationVisible by mutableStateOf(false)
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                Box {
                    TvProfilesScreen(
                        state = ProfilesUiState(
                            isLoading = false,
                            profiles = ProfilesPreviewData.profiles,
                        ),
                        onAction = {},
                        onCreateProfile = {},
                        onEditProfile = {},
                        isLogoutConfirmationVisible = isConfirmationVisible,
                        onLogoutRequested = { isConfirmationVisible = true },
                    )
                    AppLogoutConfirmationDialog(
                        visible = isConfirmationVisible,
                        isLogoutInProgress = false,
                        onConfirm = {},
                        onDismiss = { isConfirmationVisible = false },
                        platform = Platform.Tv,
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + ProfilesPreviewData.profiles.first().id)
            .performKeyInput { pressKey(Key.DirectionUp) }
        composeRule
            .onNodeWithTag(ProfilesTestTags.SignOutButton)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag(AppAuthTestTags.CancelLogoutButton).assertIsFocused()
        composeRule
            .onNodeWithTag(AppAuthTestTags.CancelLogoutButton)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag(ProfilesTestTags.SignOutButton).assertIsFocused()

        composeRule
            .onNodeWithTag(ProfilesTestTags.SignOutButton)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag(AppAuthTestTags.CancelLogoutButton).assertIsFocused()
        pressBack()
        composeRule.onNodeWithTag(ProfilesTestTags.SignOutButton).assertIsFocused()
    }

    private fun setDialog(
        visible: Boolean,
        isLogoutInProgress: Boolean = false,
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {},
        platform: Platform = Platform.Mobile,
    ) {
        composeRule.setContent {
            StreamCoreTheme {
                AppLogoutConfirmationDialog(
                    visible = visible,
                    isLogoutInProgress = isLogoutInProgress,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss,
                    platform = platform,
                )
            }
        }
    }
}
