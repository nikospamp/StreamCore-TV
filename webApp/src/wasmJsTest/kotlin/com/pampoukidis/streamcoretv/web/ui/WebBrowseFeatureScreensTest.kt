package com.pampoukidis.streamcoretv.web.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.theme.StreamCoreTheme
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsPreviewData
import com.pampoukidis.streamcoretv.feature.details.common.testing.DetailsTestTags
import com.pampoukidis.streamcoretv.feature.details.web.details.WebDetailsScreen
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeAction
import com.pampoukidis.streamcoretv.feature.home.common.home.HomeUiState
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomePreviewData
import com.pampoukidis.streamcoretv.feature.home.common.testing.HomeTestTags
import com.pampoukidis.streamcoretv.feature.home.web.home.WebHomeScreen
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryAction
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState
import com.pampoukidis.streamcoretv.feature.library.common.testing.LibraryTestTags
import com.pampoukidis.streamcoretv.feature.library.web.library.WebLibraryScreen
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals

class WebBrowseFeatureScreensTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun homeContentCardDispatchesTypedSelection(): TestResult {
        val row = HomePreviewData.rows.first().copy(
            content = HomePreviewData.rows.first().content.map(ContentModel::withoutArtwork),
        )
        val actions = mutableListOf<HomeAction>()
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebHomeScreen(
                        state = HomeUiState(isLoading = false, rows = listOf(row)),
                        onAction = actions::add,
                        selectedContentKey = null,
                        returnFocusKey = null,
                        onReturnFocusConsumed = {},
                    )
                }
            }

            val content = row.content.first()
            onNodeWithTag(HomeTestTags.HeroDetails)
                .assertIsDisplayed()
                .performClick()
            assertEquals(HomeAction.ContentSelected(content), actions.last())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun libraryContentDispatchesTypedSelection(): TestResult {
        val content = DetailsPreviewData.content.withoutArtwork().copy(
            row = LibraryContinueWatchingSection,
        )
        val actions = mutableListOf<LibraryAction>()
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebLibraryScreen(
                        state = LibraryUiState(
                            isLoading = false,
                            continueWatching = listOf(content),
                        ),
                        onAction = actions::add,
                        selectedContentKey = null,
                        returnFocusKey = null,
                        onReturnFocusConsumed = {},
                    )
                }
            }

            onNodeWithTag(
                LibraryTestTags.ContentPrefix + LibraryContinueWatchingSection + ":" + content.id,
            )
                .assertIsDisplayed()
                .performClick()
            assertEquals(LibraryAction.ContentSelected(content), actions.last())
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun detailsPlayDispatchesWithoutCreatingAPlayer(): TestResult {
        val actions = mutableListOf<DetailsAction>()
        return runComposeUiTest {
            setContent {
                StreamCoreTheme(darkTheme = true) {
                    WebDetailsScreen(
                        state = DetailsUiState(
                            isLoading = false,
                            content = DetailsPreviewData.content.withoutArtwork(),
                            isLibraryAvailable = true,
                        ),
                        onAction = actions::add,
                        returnFocusKey = null,
                        onReturnFocusConsumed = {},
                    )
                }
            }

            onNodeWithTag(DetailsTestTags.PlayButton)
                .assertIsDisplayed()
                .performClick()
            assertEquals(DetailsAction.PlaySelected, actions.last())
        }
    }
}

private const val LibraryContinueWatchingSection = "library:continue-watching"

private fun ContentModel.withoutArtwork(): ContentModel {
    return copy(
        poster = "",
        backdrop = null,
    )
}
