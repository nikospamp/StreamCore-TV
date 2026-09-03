package com.pampoukidis.streamcoretv.feature.details.web.testing

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.TrailerModel
import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureIds
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState

internal object WebDetailsFixtures {
    private val cast = listOf(
        Cast(
            id = "cast-alex",
            name = "Alex Morgan",
            characterName = "Commander Vale",
            image = null,
        ),
        Cast(
            id = "cast-jordan",
            name = "Jordan Lee",
            characterName = "Dr. Ilya Chen",
            image = null,
        ),
    )

    private val genres = listOf(
        Genre(id = "science-fiction", name = "Science Fiction"),
        Genre(id = "thriller", name = "Thriller"),
    )

    val content = ContentModel(
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
            TrailerModel(
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
                Cast(
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
    ): ContentModel {
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
