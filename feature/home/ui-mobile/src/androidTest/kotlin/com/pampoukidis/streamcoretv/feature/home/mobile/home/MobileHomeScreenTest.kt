package com.pampoukidis.streamcoretv.feature.home.mobile.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MobileHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chromeAndHeroActionsExposeExpectedBehavior() {
        val actions = mutableListOf<HomeAction>()
        var profileSelected = false
        var searchSelected = false

        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                MobileHomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        rows = testRows(),
                    ),
                    onAction = actions::add,
                    onSearchSelected = { searchSelected = true },
                    onProfileSelected = { profileSelected = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Search").performClick()
        composeRule.onNodeWithContentDescription("Choose profile").performClick()
        composeRule.onAllNodesWithTag(HomeTestTags.HeroDetails)[0].performClick()

        assertTrue(profileSelected)
        assertTrue(searchSelected)
        assertTrue(actions.last() is HomeAction.ContentSelected)
    }

    @Test
    fun continueWatchingIsOmittedWithoutProgressData() {
        val rows = testRows().filterNot { row ->
            row.type == RowType.ContinueWatching
        }

        composeRule.setContent {
            StreamCoreTheme {
                MobileHomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        rows = rows,
                    ),
                    onAction = {},
                    onSearchSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule
            .onNodeWithTag(HomeTestTags.RowPrefix + "continue-watching")
            .assertDoesNotExist()
    }

    @Test
    fun pullToRefreshDispatchesRefreshAction() {
        val actions = mutableListOf<HomeAction>()

        composeRule.setContent {
            StreamCoreTheme {
                MobileHomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        rows = testRows(),
                    ),
                    onAction = actions::add,
                    onSearchSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule
            .onNodeWithTag(HomeTestTags.PullToRefresh)
            .performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertTrue(actions.contains(HomeAction.Refresh))
    }

    @Test
    fun continueWatchingAndShelvesAreRendered() {
        composeRule.setContent {
            StreamCoreTheme {
                MobileHomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        rows = testRows(),
                    ),
                    onAction = {},
                    onSearchSelected = {},
                    onProfileSelected = {},
                )
            }
        }

        composeRule
            .onNodeWithTag(HomeTestTags.RowPrefix + "continue-watching")
            .assertExists()
        composeRule
            .onNodeWithTag(HomeTestTags.RowPrefix + "recommended")
            .assertExists()
    }

    private fun testRows(): List<RowModel> {
        return HomePreviewData.rows.map { row ->
            row.copy(
                content = row.content.map { content ->
                    content.copy(
                        poster = "",
                        backdrop = null,
                    )
                },
            )
        }
    }
}
