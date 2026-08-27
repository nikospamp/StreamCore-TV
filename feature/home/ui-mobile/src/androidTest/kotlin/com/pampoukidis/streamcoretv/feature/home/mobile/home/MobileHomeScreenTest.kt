package com.pampoukidis.streamcoretv.feature.home.mobile.home

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
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
import java.io.File

class MobileHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun heroHandlesFeaturedListGrowingAfterInitialLoad() {
        composeRule.mainClock.autoAdvance = false
        val featured = testRows().first { it.type == RowType.Featured }
        val state = mutableStateOf(
            HomeUiState(isLoading = false, rows = listOf(featured.copy(content = featured.content.take(1)))),
        )
        composeRule.setContent {
            StreamCoreTheme {
                MobileHomeScreen(state = state.value, onAction = {}, onProfileSelected = {})
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag(HomeTestTags.HeroDetails).assertIsDisplayed()
        composeRule.runOnIdle {
            state.value = HomeUiState(isLoading = false, rows = listOf(featured))
        }
        composeRule.mainClock.advanceTimeBy(800)
        composeRule.onNodeWithContentDescription("Slide 1 of ${featured.content.size}").assertExists()
        composeRule.mainClock.advanceTimeBy(5_600)
        composeRule.onNodeWithContentDescription("Slide 2 of ${featured.content.size}").assertExists()
        composeRule.runOnIdle {
            state.value = HomeUiState(
                isLoading = false,
                rows = listOf(featured.copy(content = featured.content.take(1))),
            )
        }
        composeRule.mainClock.advanceTimeBy(800)
        composeRule.onNodeWithTag(HomeTestTags.HeroDetails).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Slide 1 of 1").assertDoesNotExist()
    }

    @Test
    fun longHeroTitleKeepsDetailsAndIndicatorVisible() {
        composeRule.mainClock.autoAdvance = false
        val featured = testRows().first { it.type == RowType.Featured }
        val rows = listOf(featured.copy(content = featured.content.map {
            it.copy(title = "Spider-Man: Brand New Day")
        }))
        composeRule.setContent {
            StreamCoreTheme {
                MobileHomeScreen(
                    state = HomeUiState(isLoading = false, rows = rows),
                    onAction = {},
                    onProfileSelected = {},
                )
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        val details = composeRule.onAllNodesWithTag(HomeTestTags.HeroDetails)[0]
        val indicator = composeRule.onNodeWithContentDescription("Slide 1 of ${featured.content.size}")
        details.assertIsDisplayed()
        indicator.assertIsDisplayed()
        assertTrue(details.getUnclippedBoundsInRoot().bottom < indicator.getUnclippedBoundsInRoot().top)
        val screenshot = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "carousel-long-title.png",
        )
        screenshot.outputStream().use { output ->
            composeRule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    @Test
    fun heroSwipeKeepsStandaloneIndicatorInSync() {
        composeRule.mainClock.autoAdvance = false
        val rows = testRows().filter { it.type == RowType.Featured }
        val count = rows.single().content.size
        composeRule.setContent {
            StreamCoreTheme {
                MobileHomeScreen(
                    state = HomeUiState(isLoading = false, rows = rows),
                    onAction = {},
                    onProfileSelected = {},
                )
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithContentDescription("Slide 1 of $count").assertExists()
        composeRule.onNodeWithTag(HomeTestTags.Hero).performTouchInput {
            swipeLeft(startX = width * 0.8f, endX = width * 0.3f, durationMillis = 500)
        }
        composeRule.mainClock.advanceTimeBy(800)
        composeRule.onNodeWithContentDescription("Slide 2 of $count").assertExists()
    }

    @Test
    fun chromeAndHeroActionsExposeExpectedBehavior() {
        val actions = mutableListOf<HomeAction>()
        var profileSelected = false

        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
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

        composeRule.onNodeWithContentDescription("Search").assertDoesNotExist()
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
            StreamCoreTheme {
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
            StreamCoreTheme {
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
            StreamCoreTheme {
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
