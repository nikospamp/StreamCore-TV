package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebTopChrome
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals

class WebBrowseShellTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun topChromeExposesSelectionAndDeterministicHorizontalFocus(): TestResult {
        val destinations = mutableListOf<WebBrowseDestination>()
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    StreamCoreWebTopChrome(
                        activeDestination = WebBrowseDestination.Home,
                        profileName = "Fixture profile",
                        logoutInProgress = false,
                        onDestinationSelected = destinations::add,
                        onChangeProfile = {},
                        onLogout = {},
                    )
                }
            }

            onNodeWithText("Home")
                .assertIsSelected()
                .performClick()
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionRight) }
            onNodeWithText("Search")
                .assertIsNotSelected()
                .assertIsFocused()
                .performKeyInput { pressKey(Key.Spacebar) }
            assertEquals(listOf(WebBrowseDestination.Home, WebBrowseDestination.Search), destinations)
        }
    }
}
