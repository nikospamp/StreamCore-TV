package com.pampoukidis.streamcoretv.feature.search.common.testing

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchContentState
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchUiState

object SearchPreviewData {

    private val drama = Genre(id = "drama", name = "Drama")
    private val scienceFiction = Genre(id = "science-fiction", name = "Science Fiction")
    private val leadCast = listOf(
        Cast(
            id = "cast-1",
            name = "Alex Morgan",
            characterName = "Commander Vale",
            image = null,
        ),
    )

    val items = listOf(
        content("orbit-fall", "Orbit Fall", 9, "PG-13", listOf(scienceFiction)),
        content("northern-line", "Northern Line", 8, "16", listOf(drama)),
        content("last-archive", "The Last Archive", 9, "PG-13", listOf(drama, scienceFiction)),
        content("quiet-signal", "A Quiet Signal Beyond the Northern Lights", 8, "12", listOf(scienceFiction)),
        content("marble-city", "Marble City", 7, "PG", listOf(drama)),
        content("river-zero", "River Zero", 8, "16", listOf(drama)),
    )

    val discovery = SearchUiState(
        recentQueries = listOf("Orbit", "Northern Line", "Family adventure"),
        trending = items.take(4).map { content -> content.copy(row = "search:trending") },
    )

    val results = SearchUiState(
        query = "orbit",
        resultQuery = "orbit",
        content = SearchContentState.Results(
            items.map { content -> content.copy(row = "search:orbit") },
        ),
    )

    val loading = SearchUiState(
        query = "orbit",
        resultQuery = "orbit",
        content = SearchContentState.Loading,
    )

    val empty = SearchUiState(
        query = "unknown title",
        resultQuery = "unknown title",
        content = SearchContentState.Empty("unknown title"),
    )

    val failure = SearchUiState(
        query = "orbit",
        resultQuery = "orbit",
        content = SearchContentState.Failure(
            com.pampoukidis.streamcoretv.core.model.error.AppError.Network(),
        ),
    )

    val longLocalized = SearchUiState(
        query = "Επιστημονική φαντασία στο διάστημα",
        resultQuery = "Επιστημονική φαντασία στο διάστημα",
        content = SearchContentState.Results(
            items.mapIndexed { index, content ->
                content.copy(
                    title = if (index == 0) {
                        "Η τελευταία αποστολή πέρα από τον ορατό ορίζοντα"
                    } else {
                        content.title
                    },
                    row = "search:localized",
                )
            },
        ),
    )

    private fun content(
        id: String,
        title: String,
        rating: Int,
        pgRatingName: String,
        genres: List<Genre>,
    ): ContentModel {
        return ContentModel(
            id = id,
            title = title,
            description = "A cinematic story selected for this preview.",
            rating = rating,
            pgRatingName = pgRatingName,
            pgRatingLevel = 13,
            poster = "https://images.streamcore.example/$id/poster.jpg",
            backdrop = "https://images.streamcore.example/$id/backdrop.jpg",
            cast = leadCast,
            releaseDate = 1_711_929_600_000L,
            genres = genres,
        )
    }
}
