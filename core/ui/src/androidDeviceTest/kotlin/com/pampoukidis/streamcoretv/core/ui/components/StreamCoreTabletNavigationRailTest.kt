package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.platform.testTag
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StreamCoreTabletNavigationRailTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun destinationsKeepExpectedOrderAndSelectedSemantics() {
        setRail()

        val home = composeRule.onNodeWithTag(destinationTag("home"))
            .fetchSemanticsNode().boundsInRoot
        val search = composeRule.onNodeWithTag(destinationTag("search"))
            .fetchSemanticsNode().boundsInRoot
        val library = composeRule.onNodeWithTag(destinationTag("library"))
            .fetchSemanticsNode().boundsInRoot

        assertTrue(home.top < search.top)
        assertTrue(search.top < library.top)
        composeRule.onNodeWithTag(destinationTag("search")).assertIsSelected()
    }

    @Test
    fun destinationClickDispatchesOnlyRequestedIndex() {
        val selections = mutableListOf<Int>()
        setRail(onSelected = selections::add)

        composeRule.onNodeWithTag(destinationTag("library")).performClick()

        assertEquals(listOf(2), selections)
    }

    private fun setRail(onSelected: (Int) -> Unit = {}) {
        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                StreamCoreTabletNavigationRail {
                    StreamCoreTabletNavigationRailItem(
                        selected = false,
                        label = "Home",
                        onClick = { onSelected(0) },
                        icon = { StreamCoreHomeIcon() },
                        modifier = androidx.compose.ui.Modifier.testTag(destinationTag("home")),
                    )
                    StreamCoreTabletNavigationRailItem(
                        selected = true,
                        label = "Search",
                        onClick = { onSelected(1) },
                        icon = { StreamCoreSearchIcon() },
                        modifier = androidx.compose.ui.Modifier.testTag(destinationTag("search")),
                    )
                    StreamCoreTabletNavigationRailItem(
                        selected = false,
                        label = "Library",
                        onClick = { onSelected(2) },
                        icon = { StreamCoreLibraryIcon() },
                        modifier = androidx.compose.ui.Modifier.testTag(destinationTag("library")),
                    )
                }
            }
        }
    }

    private fun destinationTag(name: String): String {
        return StreamCoreTabletNavigationTestTags.destination(name)
    }
}
