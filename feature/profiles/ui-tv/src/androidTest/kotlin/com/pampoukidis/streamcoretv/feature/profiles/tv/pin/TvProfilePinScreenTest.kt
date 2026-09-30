package com.pampoukidis.streamcoretv.feature.profiles.tv.pin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.*
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesAction
import com.pampoukidis.streamcoretv.feature.profiles.common.profiles.ProfilesUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesTestTags
import com.pampoukidis.streamcoretv.feature.profiles.tv.profiles.TvProfilesScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TvProfilePinScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun keypadAndRemoteDigitsShareDraftAndBackRemainsEnabledWhenChecking() {
        var state by mutableStateOf(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4))
        var cancellations = 0
        composeRule.setContent {
            StreamCoreTheme {
                TvProfilePinScreen(
                    state,
                    { digit -> state = state.copy(draft = state.draft + digit) },
                    { state = state.copy(draft = state.draft.dropLast(1)) },
                    { cancellations++ },
                    {},
                )
            }
        }
        composeRule.onNodeWithTag(ProfilePinTestTags.DigitPrefix + "1").assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.onNodeWithTag(ProfilePinTestTags.DigitPrefix + "2").assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag(ProfilePinTestTags.DigitPrefix + "2").performKeyInput { pressKey(Key.Seven) }
        composeRule.runOnIdle {
            assertEquals("27", state.draft)
            state = state.copy(isSubmitting = true)
        }
        composeRule.onNodeWithTag(ProfilePinTestTags.Cancel).assertIsEnabled().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        composeRule.runOnIdle { assertEquals(1, cancellations) }
    }

    @Test
    fun rapidNumericEventsDeliverEveryDeltaWithoutWaitingForComposition() {
        val digits = mutableListOf<Int>()
        var deletions = 0
        composeRule.setContent {
            StreamCoreTheme {
                // Keep the rendered snapshot fixed: events must not reconstruct a draft from it.
                TvProfilePinScreen(
                    ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4),
                    { digits += it }, { deletions++ }, {}, {},
                )
            }
        }
        composeRule.onNodeWithTag(ProfilePinTestTags.DigitPrefix + "1").assertIsFocused().performKeyInput {
            repeat(4) { pressKey(Key.Seven) }
            repeat(2) { pressKey(Key.Backspace) }
        }
        composeRule.runOnIdle {
            assertEquals(listOf(7, 7, 7, 7), digits)
            assertEquals(2, deletions)
        }
    }

    @Test
    fun cancelRestoresTheOriginatingProfileTile() {
        val profile = ProfilesPreviewData.profiles[1]
        var state by mutableStateOf(ProfilesUiState(
            isLoading = false,
            profiles = ProfilesPreviewData.profiles,
            pin = ProfilePinUiState(profile, 4),
        ))
        composeRule.setContent {
            StreamCoreTheme {
                TvProfilesScreen(
                    state = state,
                    onAction = { if (it == ProfilesAction.CancelPin) state = state.copy(pin = null, restoreFocusProfileId = profile.id) },
                    onCreateProfile = {}, onEditProfile = {},
                )
            }
        }
        composeRule.onNodeWithTag(ProfilePinTestTags.Cancel).performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithTag(ProfilesTestTags.ProfileCardPrefix + profile.id).assertIsFocused()
    }
}
