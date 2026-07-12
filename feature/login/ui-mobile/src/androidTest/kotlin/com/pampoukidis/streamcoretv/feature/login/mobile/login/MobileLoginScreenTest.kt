package com.pampoukidis.streamcoretv.feature.login.mobile.login

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginUiState
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import org.junit.Rule
import org.junit.Test

class MobileLoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentStateShowsForm() {
        composeRule.setContent {
            StreamCoreTheme {
                MobileLoginScreen(
                    state = LoginUiState(
                        identifier = "lead@streamcore.tv",
                        password = "password",
                        isSubmitEnabled = true,
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithTag(LoginTestTags.IdentifierField).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.PasswordField).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.PasswordVisibilityToggle).performClick()
        composeRule.onNodeWithContentDescription("Hide password").assertExists()
        composeRule.onNodeWithTag(LoginTestTags.SubmitButton).assertIsEnabled()
    }
}
