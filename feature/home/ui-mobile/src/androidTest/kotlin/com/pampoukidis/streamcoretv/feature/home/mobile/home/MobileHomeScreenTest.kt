package com.pampoukidis.streamcoretv.feature.home.mobile.home

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTVTheme
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

        composeRule.setContent {
            StreamCoreTVTheme(darkTheme = true) {
                MobileHomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        rows = testRows(),
                    ),
                    onAction = actions::add,
                    onProfileSelected = { profileSelected = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Search").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Choose profile").performClick()
        composeRule.onAllNodesWithTag(HomeTestTags.HeroDetails)[0].performClick()

        assertTrue(profileSelected)
        assertTrue(actions.last() is HomeAction.ContentSelected)
    }

    @Test
    fun continueWatchingIsOmittedWithoutProgressData() {
        val rows = testRows().filterNot { row ->
            row.type == RowType.ContinueWatching
        }

        composeRule.setContent {
            StreamCoreTVTheme {
                MobileHomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        rows = rows,
                    ),
                    onAction = {},
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
            StreamCoreTVTheme {
                MobileHomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        rows = testRows(),
                    ),
                    onAction = actions::add,
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
            StreamCoreTVTheme {
                MobileHomeScreen(
                    state = HomeUiState(
                        isLoading = false,
                        rows = testRows(),
                    ),
                    onAction = {},
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
