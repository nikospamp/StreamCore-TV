package com.pampoukidis.streamcoretv.feature.library.web.testing

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.PlaybackProgressModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureIds
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState
import com.pampoukidis.streamcoretv.feature.library.web.library.WebLibraryContinueWatchingSection
import com.pampoukidis.streamcoretv.feature.library.web.library.WebLibraryLikedSection
import com.pampoukidis.streamcoretv.feature.library.web.library.WebLibraryMyListSection

internal object WebLibraryFixtures {
    private val continueWatching = content(
        id = "northern-signal",
        title = "Northern Signal",
        row = WebLibraryContinueWatchingSection,
    ).copy(
        playbackProgress = PlaybackProgressModel(
            positionMillis = 42_000L,
            durationMillis = 100_000L,
        ),
    )

    private val liked = content(
        id = "last-horizon",
        title = "The Last Horizon",
        row = WebLibraryLikedSection,
    )

    private val saved = content(
        id = WebBrowseFixtureIds.ContentId,
        title = "Orbit Fall",
        row = WebLibraryMyListSection,
    )

    val contentState = LibraryUiState(
        isLoading = false,
        continueWatching = listOf(
            continueWatching,
            continueWatching.copy(id = "quiet-current", title = "Quiet Current"),
        ),
        likedContent = listOf(
            liked,
            liked.copy(id = "glass-city", title = "Glass City"),
            liked.copy(id = "deep-current", title = "Deep Current"),
        ),
        myListContent = listOf(
            saved,
            saved.copy(id = "archive-seven", title = "Archive Seven"),
            saved.copy(id = "after-the-static", title = "After the Static"),
        ),
    )

    val longTextState = contentState.copy(
        continueWatching = contentState.continueWatching.map { item ->
            item.copy(
                title = "Northern Signal: The Long Journey Beyond the Last Known Horizon",
                description = LongDescription,
            )
        },
        likedContent = contentState.likedContent.map { item ->
            item.copy(
                title = "The Last Horizon and the Cities We Left Behind",
                description = LongDescription,
            )
        },
        myListContent = contentState.myListContent.map { item ->
            item.copy(
                title = "Orbit Fall: A Chronicle of the Final Rescue Mission",
                description = LongDescription,
            )
        },
    )

    fun state(scenario: WebBrowseFixtureScenario): LibraryUiState {
        return when (scenario) {
            WebBrowseFixtureScenario.Loading -> LibraryUiState(isLoading = true)
            WebBrowseFixtureScenario.Content -> contentState
            WebBrowseFixtureScenario.Empty -> LibraryUiState(isLoading = false)
            WebBrowseFixtureScenario.Offline -> contentState.copy(error = AppError.Network())
            WebBrowseFixtureScenario.Error -> LibraryUiState(
                isLoading = false,
                error = AppError.Unknown(),
            )

            WebBrowseFixtureScenario.LongText -> longTextState
        }
    }

    private fun content(
        id: String,
        title: String,
        row: String,
    ): ContentModel {
        return ContentModel(
            id = id,
            title = title,
            description = "A deterministic browser-only fixture with no provider or network dependency.",
            rating = 8,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
            row = row,
        )
    }

    private const val LongDescription =
        "A deliberately long localized-style description verifies that dense copy remains readable " +
            "without changing card geometry or obscuring keyboard focus."
}
