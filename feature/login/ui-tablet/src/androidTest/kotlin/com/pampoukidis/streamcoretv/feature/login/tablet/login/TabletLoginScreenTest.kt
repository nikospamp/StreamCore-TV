package com.pampoukidis.streamcoretv.feature.login.tablet.login

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
import com.pampoukidis.streamcoretv.feature.login.common.testing.LoginTestTags
import com.pampoukidis.streamcoretv.feature.login.common.login.LoginUiState
import org.junit.Rule
import org.junit.Test

class TabletLoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateDisablesSubmit() {
        composeRule.setContent {
            StreamCoreTVTheme {
                TabletLoginScreen(
                    state = LoginUiState(
                        email = "lead@streamcore.tv",
                        password = "password",
                        isLoading = true,
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithTag(LoginTestTags.Root).assertExists()
        composeRule.onNodeWithTag(LoginTestTags.SubmitButton).assertIsNotEnabled()
    }
}
