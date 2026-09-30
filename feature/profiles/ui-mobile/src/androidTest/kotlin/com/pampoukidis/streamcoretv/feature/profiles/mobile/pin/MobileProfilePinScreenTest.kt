package com.pampoukidis.streamcoretv.feature.profiles.mobile.pin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.*
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MobileProfilePinScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inputIsPasswordAndBackRemainsAvailableWhileChecking() {
        var state by mutableStateOf(ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4))
        var cancellations = 0
        composeRule.setContent {
            StreamCoreTheme {
                MobileProfilePinScreen(state, { state = state.copy(draft = it) }, { cancellations++ }, {})
            }
        }
        composeRule.onNodeWithTag(ProfilePinTestTags.Input)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).performTextInput("12")
        composeRule.runOnIdle {
            assertEquals("12", state.draft)
            state = state.copy(isSubmitting = true)
        }
        composeRule.onNodeWithTag(ProfilePinTestTags.Input).assertIsNotEnabled()
        composeRule.onNodeWithTag(ProfilePinTestTags.Cancel).assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, cancellations) }
    }

    @Test
    fun connectionFailureOffersExplicitRetry() {
        var retries = 0
        composeRule.setContent {
            StreamCoreTheme {
                MobileProfilePinScreen(
                    ProfilePinUiState(ProfilesPreviewData.profiles.first(), 4, draft = "1234", failure = ProfilePinFailure.Unavailable),
                    {}, {}, { retries++ },
                )
            }
        }
        composeRule.onNodeWithTag(ProfilePinTestTags.Retry).assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, retries) }
    }
}

