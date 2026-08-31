package com.pampoukidis.streamcoretv.feature.home.tv.home

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.ui.motion.StreamCoreSharedKey
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TvHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun focusedHeroPausesAutoAdvanceAndLeftReturnsToPreviousSlide() {
        val featured = HomePreviewData.rows.first { it.type == RowType.Featured }
        setScreen(state = HomeUiState(isLoading = false, rows = listOf(featured)))
        composeRule.onNodeWithTag(HomeTestTags.HeroDetails).assertIsFocused()
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(6_000)
        composeRule.onNodeWithContentDescription("Slide 1 of ${featured.content.size}").assertExists()
        composeRule.onNodeWithTag(HomeTestTags.HeroDetails)
            .performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.mainClock.advanceTimeBy(800)
        composeRule.onNodeWithContentDescription("Slide 2 of ${featured.content.size}").assertExists()
        composeRule.onNodeWithTag(HomeTestTags.HeroDetails)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.mainClock.advanceTimeBy(800)
        composeRule.onNodeWithContentDescription("Slide 1 of ${featured.content.size}").assertExists()
        composeRule.onNodeWithTag(HomeTestTags.HeroDetails).assertIsFocused()
    }

    @Test
    fun heroDetailsReceivesInitialFocusAndDispatchesSelection() {
        val actions = mutableListOf<HomeAction>()
        val content = HomePreviewData.rows
            .first { row -> row.type == RowType.Featured }
            .content
            .first().copy(poster = "", backdrop = null)

        setScreen(onAction = actions::add)

        composeRule
            .onNodeWithTag(HomeTestTags.HeroDetails)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        assertEquals(listOf(HomeAction.ContentSelected(content)), actions)
    }

    @Test
    fun firstShelfCardReceivesFocusWhenFeaturedIsMissing() {
        val row = HomePreviewData.rows.first { it.type == RowType.ContinueWatching }
        val content = row.content.first()

        setScreen(state = HomeUiState(isLoading = false, rows = listOf(row)))

        composeRule
            .onNodeWithTag(HomeTestTags.ContentCardPrefix + row.id + ":" + content.id)
            .assertIsFocused()
    }

    @Test
    fun dpadRightMovesHeroToNextSlide() {
        val featured = HomePreviewData.rows.first { it.type == RowType.Featured }
        val second = featured.content[1]
        setScreen(state = HomeUiState(isLoading = false, rows = listOf(featured)))

        composeRule.onNodeWithContentDescription("Slide 1 of ${featured.content.size}").assertExists()

        composeRule
            .onNodeWithTag(HomeTestTags.HeroDetails)
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule.onNodeWithText(second.title).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.HeroDetails).assertIsFocused()
        composeRule.onNodeWithContentDescription("Slide 2 of ${featured.content.size}").assertExists()
    }

    @Test
    fun returnFocusKeyRestoresFocusToExactShelfCard() {
        val row = HomePreviewData.rows.first { it.type == RowType.TopTen }
        val content = row.content[2]
        val returnFocusKey = StreamCoreSharedKey.content(
            contentId = content.id,
            row = content.row,
        )

        setScreen(returnFocusKey = returnFocusKey)

        composeRule
            .onNodeWithTag(HomeTestTags.ContentCardPrefix + row.id + ":" + content.id)
            .assertIsFocused()
    }

    @Test
    fun missingReturnFocusKeyUsesStableHeroFallbackAndConsumesKey() {
        val consumedKeys = mutableListOf<String>()

        setScreen(
            returnFocusKey = "missing:content",
            onReturnFocusConsumed = consumedKeys::add,
        )

        composeRule.onNodeWithTag(HomeTestTags.HeroDetails).assertIsFocused()
        assertEquals(listOf("missing:content"), consumedKeys)
    }

    @Test
    fun focusedContinueWatchingCardKeepsSectionTitleVisible() {
        val row = HomePreviewData.rows.first { it.type == RowType.ContinueWatching }
        val content = row.content.first()
        setScreen(state = HomeUiState(isLoading = false, rows = listOf(row)))

        composeRule
            .onNodeWithTag(HomeTestTags.ContentCardPrefix + row.id + ":" + content.id)
            .assertIsFocused()
            .assertIsDisplayed()
        composeRule.onNodeWithText(row.title).assertIsDisplayed()
    }

    @Test
    fun horizontalFocusMoveDoesNotShiftShelfVertically() {
        val row = HomePreviewData.rows.first { it.type == RowType.ContinueWatching }
        val firstContent = row.content[0]
        val secondContent = row.content[1]
        setScreen(state = HomeUiState(isLoading = false, rows = listOf(row)))
        val rowNode = composeRule.onNodeWithTag(HomeTestTags.RowPrefix + row.id)
        val initialTop = rowNode.getUnclippedBoundsInRoot().top

        composeRule
            .onNodeWithTag(HomeTestTags.ContentCardPrefix + row.id + ":" + firstContent.id)
            .performKeyInput { pressKey(Key.DirectionRight) }

        composeRule
            .onNodeWithTag(HomeTestTags.ContentCardPrefix + row.id + ":" + secondContent.id)
            .assertIsFocused()
        assertEquals(initialTop, rowNode.getUnclippedBoundsInRoot().top)
    }

    @Test
    fun refreshButtonDispatchesRefresh() {
        val actions = mutableListOf<HomeAction>()
        setScreen(onAction = actions::add)

        composeRule.onNodeWithTag(HomeTestTags.RefreshButton)
            .performSemanticsAction(SemanticsActions.RequestFocus) { requestFocus ->
                requestFocus()
            }
            .performKeyInput { pressKey(Key.Enter) }

        assertEquals(listOf(HomeAction.Refresh), actions)
    }

    private fun setScreen(
        state: HomeUiState = HomeUiState(
            isLoading = false,
            rows = HomePreviewData.rows,
        ),
        onAction: (HomeAction) -> Unit = {},
        selectedContentKey: String? = null,
        returnFocusKey: String? = null,
        onReturnFocusConsumed: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                TvHomeScreen(
                    state = state.copy(
                        rows = state.rows.map { row ->
                            row.copy(content = row.content.map { it.copy(poster = "", backdrop = null) })
                        },
                    ),
                    onAction = onAction,
                    selectedContentKey = selectedContentKey,
                    returnFocusKey = returnFocusKey,
                    onReturnFocusConsumed = onReturnFocusConsumed,
                )
            }
        }
    }
}
