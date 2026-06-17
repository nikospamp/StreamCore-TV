package com.pampoukidis.streamcoretv.feature.login.tv.login

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginUiState
import org.junit.Rule
import org.junit.Test

class TvLoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun identifierReceivesInitialFocus() {
        composeRule.setContent {
            StreamCoreTVTheme {
                TvLoginScreen(
                    state = LoginUiState(
                        identifier = "lead@streamcore.tv",
                        password = "password",
                        isSubmitEnabled = true,
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(LoginTestTags.Root).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.IdentifierField).assertIsFocused()
        composeRule.onNodeWithTag(LoginTestTags.PasswordVisibilityToggle).performClick()
        composeRule.onNodeWithContentDescription("Hide password").assertExists()
    }
}
