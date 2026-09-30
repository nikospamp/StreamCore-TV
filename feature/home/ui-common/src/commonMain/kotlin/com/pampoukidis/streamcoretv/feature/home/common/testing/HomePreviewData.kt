package com.pampoukidis.streamcoretv.feature.home.common.testing

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgress
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCastMember
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreGenre

object HomePreviewData {

    private val drama = StreamCoreGenre(id = "drama", name = "Drama")
    private val thriller = StreamCoreGenre(id = "thriller", name = "Thriller")
    private val scienceFiction = StreamCoreGenre(id = "science-fiction", name = "Science Fiction")
    private val family = StreamCoreGenre(id = "family", name = "Family")

    private val leadCast = listOf(
        StreamCoreCastMember(
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
            type = RowType.Featured,
        ),
        RowModel(
            id = "continue-watching",
            title = "Bookmarks",
            subtitle = "Continue where you left off",
            content = content.take(3).mapIndexed { index, item ->
                item.copy(
                    row = "continue-watching",
                    playbackProgress = StreamCorePlaybackProgress(
                        positionMillis = (index + 2L) * 12L * 60L * 1000L,
                        durationMillis = 90L * 60L * 1000L,
                    ),
                )
            },
            type = RowType.ContinueWatching,
        ),
        RowModel(
            id = "top-ten",
            title = "Top 10 today",
            subtitle = "Most watched right now",
            content = content.withRow(row = "top-ten"),
            type = RowType.TopTen,
        ),
        RowModel(
            id = "recommended",
            title = "Recommended for you",
            subtitle = "Based on your viewing profile",
            content = content.withRow(row = "recommended"),
            type = RowType.Poster,
        ),
        RowModel(
            id = "new-releases",
            title = "New releases",
            subtitle = "Recently added",
            content = content.reversed().withRow(row = "new-releases"),
            type = RowType.Landscape,
        ),
    )

    private fun List<StreamCoreContent>.withRow(row: String): List<StreamCoreContent> {
        return map { content -> content.copy(row = row) }
    }

    private fun content(
        id: String,
        title: String,
        description: String,
        rating: Int,
        pgRatingName: String,
        pgRatingLevel: Int,
        genres: List<StreamCoreGenre>,
    ): StreamCoreContent {
        return StreamCoreContent(
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
