package com.pampoukidis.streamcoretv.feature.details.common.testing

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCastMember
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreGenre

object DetailsPreviewData {

    private val drama = StreamCoreGenre(id = "drama", name = "Drama")
    private val thriller = StreamCoreGenre(id = "thriller", name = "Thriller")
    private val scienceFiction = StreamCoreGenre(id = "science-fiction", name = "Science Fiction")

    private val cast = listOf(
        StreamCoreCastMember(
            id = "cast-1",
            name = "Alex Morgan",
            characterName = "Commander Vale",
            image = null,
        ),
        StreamCoreCastMember(
            id = "cast-2",
            name = "Jordan Lee",
            characterName = "Dr. Ilya Chen",
            image = null,
        ),
    )

    val content = StreamCoreContent(
        id = "orbit-fall",
        title = "Orbit Fall",
        description = "A rescue crew races to stabilize a failing orbital station while a hidden signal pulls them deeper into the debris field.",
        rating = 9,
        pgRatingName = "PG-13",
        pgRatingLevel = 13,
        poster = "https://images.streamcore.example/orbit-fall/poster.jpg",
        backdrop = "https://images.streamcore.example/orbit-fall/backdrop.jpg",
        cast = cast,
        releaseDate = 1_711_929_600_000L,
        genres = listOf(scienceFiction, thriller),
    )

    val recommendations = listOf(
        content.copy(
            id = "northern-line",
            title = "Northern Line",
            description = "A detective follows one final lead through a frozen border town.",
            rating = 8,
            genres = listOf(drama, thriller),
        ),
        content.copy(
            id = "the-last-archive",
            title = "The Last Archive",
            description = "An archivist discovers a record that should not exist.",
            genres = listOf(drama, scienceFiction),
        ),
        content.copy(
            id = "deep-current",
            title = "Deep Current",
            description = "Researchers map an unexplored current beneath the Atlantic.",
            rating = 7,
        ),
    )
}
