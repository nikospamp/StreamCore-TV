package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StreamCoreCarouselTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun advancesAfterFiveSecondsAndWrapsToFirstPage() {
        setTouchCarousel()
        composeRule.mainClock.advanceTimeBy(4_800)
        assertSlide(1, 3)
        composeRule.mainClock.advanceTimeBy(800)
        assertSlide(2, 3)
        composeRule.mainClock.advanceTimeBy(5_600)
        assertSlide(3, 3)
        composeRule.mainClock.advanceTimeBy(5_600)
        assertSlide(1, 3)
    }

    @Test
    fun swipeUpdatesOneStationaryIndicatorAndRestartsTimer() {
        setTouchCarousel()
        composeRule.mainClock.advanceTimeBy(4_000)
        val before = composeRule.onNodeWithContentDescription("Slide 1 of 3")
            .getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag(CarouselTag).performTouchInput { swipeLeft() }
        composeRule.mainClock.advanceTimeBy(800)
        assertSlide(2, 3)
        val after = composeRule.onNodeWithContentDescription("Slide 2 of 3")
            .getUnclippedBoundsInRoot()
        assertEquals(before, after)
        composeRule.mainClock.advanceTimeBy(3_500)
        assertSlide(2, 3)
        composeRule.mainClock.advanceTimeBy(1_800)
        assertSlide(3, 3)
    }

    @Test
    fun holdingTouchPausesAutoAdvance() {
        setTouchCarousel()
        composeRule.onNodeWithTag(CarouselTag).performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(6_000)
        assertSlide(1, 3)
        composeRule.onNodeWithTag(CarouselTag).performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(5_600)
        assertSlide(2, 3)
    }

    @Test
    fun backgroundingPausesAndResumingStartsFreshInterval() {
        val owner = CarouselTestLifecycleOwner()
        setTouchCarousel(owner = owner)
        composeRule.mainClock.advanceTimeBy(4_000)
        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        composeRule.mainClock.advanceTimeBy(10_000)
        assertSlide(1, 3)
        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        composeRule.mainClock.advanceTimeBy(4_000)
        assertSlide(1, 3)
        composeRule.mainClock.advanceTimeBy(1_600)
        assertSlide(2, 3)
    }

    @Test
    fun shrinkingToOneOrZeroItemsStopsTimerAndHidesIndicator() {
        val count = mutableIntStateOf(3)
        setTouchCarousel(count = count)
        composeRule.mainClock.advanceTimeBy(11_200)
        assertSlide(3, 3)
        composeRule.runOnIdle { count.intValue = 1 }
        composeRule.mainClock.advanceTimeBy(6_000)
        composeRule.onNodeWithTag("page:0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Slide 1 of 1").assertDoesNotExist()
        composeRule.runOnIdle { count.intValue = 0 }
        composeRule.mainClock.advanceTimeBy(6_000)
        composeRule.onNodeWithTag("page:0").assertDoesNotExist()
    }

    @Test
    fun tvAutoAdvancesAndPausesWhileBackgrounded() {
        val owner = CarouselTestLifecycleOwner()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                StreamCoreTheme {
                    StreamCoreTvCarousel(itemCount = 3, modifier = Modifier.height(280.dp)) { page ->
                        Text("Page $page")
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(4_800)
        assertSlide(1, 3)
        composeRule.mainClock.advanceTimeBy(800)
        assertSlide(2, 3)
        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        composeRule.mainClock.advanceTimeBy(10_000)
        assertSlide(2, 3)
        composeRule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        composeRule.mainClock.advanceTimeBy(5_600)
        assertSlide(3, 3)
    }

    private fun setTouchCarousel(
        count: androidx.compose.runtime.MutableIntState = mutableIntStateOf(3),
        owner: CarouselTestLifecycleOwner = CarouselTestLifecycleOwner(),
    ) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                StreamCoreTheme {
                    StreamCorePagerCarousel(
                        state = rememberPagerState { count.intValue },
                        modifier = Modifier.height(280.dp).testTag(CarouselTag),
                        key = { it },
                    ) { page ->
                        Box(Modifier.fillMaxSize().testTag("page:$page")) { Text("Page $page") }
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun assertSlide(page: Int, count: Int) {
        composeRule.onAllNodesWithContentDescription("Slide $page of $count").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("Slide $page of $count").assertIsDisplayed()
    }
}

private class CarouselTestLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry.createUnsafe(this).apply {
        currentState = Lifecycle.State.RESUMED
    }
    override val lifecycle: Lifecycle = registry
}

private const val CarouselTag = "carousel"
