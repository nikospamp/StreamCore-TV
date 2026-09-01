package com.pampoukidis.streamcoretv.core.ui.components

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.PlaybackProgressModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StreamCoreMobileLibraryComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun floatingNavigationExposesSelectionAndDispatchesClick() {
        val selections = mutableListOf<Int>()

        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                StreamCoreFloatingBottomNavigation(
                    selectedIndex = 0,
                    itemCount = 3,
                ) {
                    StreamCoreFloatingBottomNavigationItem(
                        selected = true,
                        label = "Home",
                        onClick = { selections += 0 },
                        icon = { StreamCoreHomeIcon() },
                    )
                    StreamCoreFloatingBottomNavigationItem(
                        selected = false,
                        label = "Search",
                        onClick = { selections += 1 },
                        icon = { StreamCoreSearchIcon() },
                    )
                    StreamCoreFloatingBottomNavigationItem(
                        selected = false,
                        label = "Library",
                        onClick = { selections += 2 },
                        icon = { StreamCoreLibraryIcon() },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Home").assertIsSelected()
        composeRule.onNodeWithText("Library").assertIsNotSelected().performClick()

        assertEquals(listOf(2), selections)
    }

    @Test
    fun posterAndContinueWatchingCardsDispatchClicks() {
        val selectedContent = mutableListOf<String>()
        val poster = content(id = "poster", title = "Poster title")
        val continueWatching = content(
            id = "continue",
            title = "Continue title",
        ).copy(
            playbackProgress = PlaybackProgressModel(
                positionMillis = 40L,
                durationMillis = 100L,
            ),
        )

        composeRule.setContent {
            StreamCoreTheme(darkTheme = true) {
                androidx.compose.foundation.layout.Row {
                    StreamCoreMobilePosterCard(
                        content = poster,
                        onClick = { selectedContent += poster.id },
                    )
                    StreamCoreMobileContinueWatchingCard(
                        content = continueWatching,
                        onClick = { selectedContent += continueWatching.id },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Poster title").performClick()
        composeRule.onNodeWithText("Continue title").performClick()

        assertEquals(listOf("poster", "continue"), selectedContent)
    }

    private fun content(
        id: String,
        title: String,
    ): ContentModel {
        return ContentModel(
            id = id,
            title = title,
            description = "",
            rating = 8,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
        )
    }
}
