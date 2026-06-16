package com.pampoukidis.streamcoretv.feature.home.common.testing

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowStyle
import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre

object HomePreviewData {

    private val drama = Genre(id = "drama", name = "Drama")
    private val thriller = Genre(id = "thriller", name = "Thriller")
    private val scienceFiction = Genre(id = "science-fiction", name = "Science Fiction")
    private val family = Genre(id = "family", name = "Family")

    private val leadCast = listOf(
        Cast(
            id = "cast-1",
            name = "Alex Morgan",
            characterName = "Commander Vale",
            image = null,
        ),
    )

    private val content = listOf(
        content(
            id = "orbit-fall",
            title = "Orbit Fall",
            description = "A rescue crew races to stabilize a failing orbital station.",
            rating = 9,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            genres = listOf(scienceFiction, thriller),
        ),
        content(
            id = "northern-line",
            title = "Northern Line",
            description = "A detective follows one final lead through a frozen border town.",
            rating = 8,
            pgRatingName = "16",
            pgRatingLevel = 16,
            genres = listOf(drama, thriller),
        ),
        content(
            id = "little-comets",
            title = "Little Comets",
            description = "Young explorers build a telescope that changes their summer.",
            rating = 8,
            pgRatingName = "All",
            pgRatingLevel = 0,
            genres = listOf(family),
        ),
        content(
            id = "the-last-archive",
            title = "The Last Archive",
            description = "An archivist discovers a record that should not exist.",
            rating = 9,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            genres = listOf(drama, scienceFiction),
        ),
    )

    val rows = listOf(
        RowModel(
            id = "featured",
            title = "Featured",
            subtitle = "Selected for your profile",
            content = content.take(3).withRow(row = "featured"),
            style = RowStyle.Carousel,
        ),
        RowModel(
            id = "top-ten",
            title = "Top 10 today",
            subtitle = "Most watched right now",
            content = content.withRow(row = "top-ten"),
            style = RowStyle.TopTen,
        ),
        RowModel(
            id = "recommended",
            title = "Recommended for you",
            subtitle = "Based on your viewing profile",
            content = content.withRow(row = "recommended"),
            style = RowStyle.Poster,
        ),
        RowModel(
            id = "new-releases",
            title = "New releases",
            subtitle = "Recently added",
            content = content.reversed().withRow(row = "new-releases"),
            style = RowStyle.Landscape,
        ),
    )

    private fun List<ContentModel>.withRow(row: String): List<ContentModel> {
        return map { content -> content.copy(row = row) }
    }

    private fun content(
        id: String,
        title: String,
        description: String,
        rating: Int,
        pgRatingName: String,
        pgRatingLevel: Int,
        genres: List<Genre>,
    ): ContentModel {
        return ContentModel(
            id = id,
            title = title,
            description = description,
            rating = rating,
            pgRatingName = pgRatingName,
            pgRatingLevel = pgRatingLevel,
            poster = "https://images.streamcore.example/$id/poster.jpg",
            backdrop = "https://images.streamcore.example/$id/backdrop.jpg",
            cast = leadCast,
            releaseDate = 1_711_929_600_000L,
            genres = genres,
        )
    }
}
