package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.core.ui.web.StreamCoreWebNavigationRail
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals

class WebBrowseShellTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun navigationRailExposesSelectionVerticalFocusAndContentHandoff(): TestResult {
        val destinations = mutableListOf<WebBrowseDestination>()
        var contentHandoffs = 0
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    StreamCoreWebNavigationRail(
                        activeDestination = WebBrowseDestination.Home,
                        profileName = "Fixture profile",
                        onDestinationSelected = destinations::add,
                        onChangeProfile = {},
                        onMoveToContent = { contentHandoffs += 1 },
                    )
                }
            }

            // Collapsed rail items expose accessible names even before their visual labels expand.
            onNodeWithContentDescription("Home")
                .assertIsSelected()
                .performClick()
                .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionDown) }
            onNodeWithContentDescription("Search")
                .assertIsNotSelected()
                .assertIsFocused()
                .performKeyInput { pressKey(Key.Spacebar) }
            assertEquals(listOf(WebBrowseDestination.Home, WebBrowseDestination.Search), destinations)
            onNodeWithContentDescription("Search")
                .performKeyInput { pressKey(Key.DirectionUp) }
            onNodeWithContentDescription("Home")
                .assertIsFocused()
                .performKeyInput { pressKey(Key.DirectionRight) }
            assertEquals(1, contentHandoffs)
        }
    }
}
