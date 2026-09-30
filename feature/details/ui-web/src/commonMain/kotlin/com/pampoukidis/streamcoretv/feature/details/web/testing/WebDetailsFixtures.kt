package com.pampoukidis.streamcoretv.feature.details.web.testing

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreTrailer
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCastMember
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreGenre
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureIds
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState

internal object WebDetailsFixtures {
    private val cast = listOf(
        StreamCoreCastMember(
            id = "cast-alex",
            name = "Alex Morgan",
            characterName = "Commander Vale",
            image = null,
        ),
        StreamCoreCastMember(
            id = "cast-jordan",
            name = "Jordan Lee",
            characterName = "Dr. Ilya Chen",
            image = null,
        ),
    )

    private val genres = listOf(
        StreamCoreGenre(id = "science-fiction", name = "Science Fiction"),
        StreamCoreGenre(id = "thriller", name = "Thriller"),
    )

    val content = StreamCoreContent(
        id = WebBrowseFixtureIds.ContentId,
        title = "Orbit Fall",
        description = "A rescue crew races to stabilize a failing orbital station while a hidden signal pulls them deeper into the debris field.",
        rating = 9,
        pgRatingName = "PG-13",
        pgRatingLevel = 13,
        poster = "",
        backdrop = null,
        cast = cast,
        releaseDate = 1_711_929_600_000L,
        genres = genres,
        trailers = listOf(
            StreamCoreTrailer(
                id = "official-trailer",
                title = "Official Trailer",
                url = "https://example.invalid/orbit-fall/trailer",
            ),
        ),
    )

    val recommendations = listOf(
        recommendation("northern-line", "Northern Line"),
        recommendation("last-archive", "The Last Archive"),
        recommendation("deep-current", "Deep Current"),
        recommendation("quiet-orbit", "Quiet Orbit"),
    )

    val contentState = DetailsUiState(
        isLoading = false,
        content = content,
        recommendations = recommendations,
        hasResumableProgress = true,
        isLibraryAvailable = true,
        isLiked = true,
        isInMyList = false,
    )

    val longTextState = contentState.copy(
        content = content.copy(
            title = "Orbit Fall: A Chronicle of the Final Rescue Beyond the Last Known Horizon",
            description = LongDescription,
            cast = cast + List(5) { index ->
                StreamCoreCastMember(
                    id = "additional-cast-$index",
                    name = "Additional Cast Member ${index + 1}",
                    characterName = "Mission Specialist ${index + 1}",
                    image = null,
                )
            },
        ),
        recommendations = recommendations.map { item ->
            item.copy(title = "${item.title}: The Extended Story of an Uncertain Future")
        },
    )

    fun state(scenario: WebBrowseFixtureScenario): DetailsUiState {
        return when (scenario) {
            WebBrowseFixtureScenario.Loading -> DetailsUiState(isLoading = true)
            WebBrowseFixtureScenario.Content -> contentState
            WebBrowseFixtureScenario.Empty -> contentState.copy(recommendations = emptyList())
            WebBrowseFixtureScenario.Offline -> contentState.copy(isLibraryAvailable = false)
            WebBrowseFixtureScenario.Error -> DetailsUiState(isLoading = false)
            WebBrowseFixtureScenario.LongText -> longTextState
        }
    }

    private fun recommendation(
        id: String,
        title: String,
    ): StreamCoreContent {
        return content.copy(
            id = id,
            title = title,
            description = "A deterministic recommendation fixture.",
            trailers = emptyList(),
        )
    }

    private const val LongDescription =
        "A rescue crew crosses an unstable debris field after a distant signal reveals that the " +
            "station’s failure may not be accidental. Every decision reshapes the mission, the crew’s " +
            "fragile alliances, and the future of the settlements waiting below."
}
