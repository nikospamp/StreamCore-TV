package com.pampoukidis.streamcoretv.feature.library.common.testing

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.PlaybackProgressModel
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState

object LibraryPreviewData {
    private val continueWatching = content(
        id = "continue-1",
        title = "Northern Signal",
        row = "library:continue-watching",
    ).copy(
        playbackProgress = PlaybackProgressModel(
            positionMillis = 42_000L,
            durationMillis = 100_000L,
        ),
    )

    private val liked = content(
        id = "liked-1",
        title = "The Last Horizon",
        row = "library:liked",
    )

    private val myList = content(
        id = "list-1",
        title = "Quiet Orbit",
        row = "library:my-list",
    )

    val contentState = LibraryUiState(
        isLoading = false,
        continueWatching = listOf(continueWatching, continueWatching.copy(id = "continue-2")),
        likedContent = listOf(liked, liked.copy(id = "liked-2"), liked.copy(id = "liked-3")),
        myListContent = listOf(myList, myList.copy(id = "list-2"), myList.copy(id = "list-3")),
    )

    private fun content(
        id: String,
        title: String,
        row: String,
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
            row = row,
        )
    }
}
