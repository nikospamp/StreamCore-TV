package com.pampoukidis.streamcoretv.feature.login.mobile

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pampoukidis.streamcoretv.common.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import com.pampoukidis.streamcoretv.feature.login.common.contract.LoginUiState
import org.junit.Rule
import org.junit.Test

class MobileLoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contentStateShowsForm() {
        composeRule.setContent {
            StreamCoreTVTheme {
                MobileLoginScreen(
                    state = LoginUiState(
                        email = "lead@streamcore.tv",
                        password = "password",
                        isSubmitEnabled = true,
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithTag(LoginTestTags.EmailField).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.PasswordField).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.SubmitButton).assertIsEnabled()
    }
}
