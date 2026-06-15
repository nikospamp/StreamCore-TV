package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbApiGenreDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbCastMemberDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbCreditsDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbImagesConfigurationDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieDetailsDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieSummaryDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbReleaseDateDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbReleaseDatesCountryDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbReleaseDatesResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbReferenceData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMapperTest {

    @Test
    fun `maps TMDB summary into backend agnostic content model`() {
        val model = TmdbMovieSummaryDto(
            id = 1,
            title = "Orbit Fall",
            overview = "Description",
            posterPath = "/poster.jpg",
            backdropPath = "/backdrop.jpg",
            genreIds = listOf(878),
            releaseDate = "2026-05-01",
            voteAverage = 8.6,
        ).toModel(referenceData = referenceData())

        assertEquals("1", model.id)
        assertEquals("Orbit Fall", model.title)
        assertEquals(9, model.rating)
        assertEquals("https://image.tmdb.test/t/p/w500/poster.jpg", model.poster)
        assertEquals("https://image.tmdb.test/t/p/w1280/backdrop.jpg", model.backdrop)
        assertEquals("Science Fiction", model.genres.single().name)
        assertTrue(model.releaseDate > 0L)
    }

    @Test
    fun `maps TMDB details cast and certification`() {
        val model = TmdbMovieDetailsDto(
            id = 1,
            title = "Orbit Fall",
            overview = "Description",
            posterPath = "/poster.jpg",
            backdropPath = "/backdrop.jpg",
            releaseDate = "2026-05-01",
            voteAverage = 8.6,
            genres = listOf(TmdbApiGenreDto(id = 878, name = "Science Fiction")),
            credits = TmdbCreditsDto(
                cast = listOf(
                    TmdbCastMemberDto(
                        id = 11,
                        name = "Second Actor",
                        character = "Engineer",
                        profilePath = "/second-actor.jpg",
                        order = 1,
                    ),
                    TmdbCastMemberDto(
                        id = 10,
                        name = "Lead Actor",
                        character = "Captain",
                        profilePath = "/lead-actor.jpg",
                        order = 0,
                    ),
                ),
            ),
            releaseDates = TmdbReleaseDatesResponseDto(
                results = listOf(
                    TmdbReleaseDatesCountryDto(
                        countryCode = "US",
                        releaseDates = listOf(
                            TmdbReleaseDateDto(certification = "PG-13", type = 3),
                        ),
                    ),
                ),
            ),
        ).toModel(referenceData = referenceData())

        assertEquals("PG-13", model.pgRatingName)
        assertEquals(13, model.pgRatingLevel)
        assertEquals("Lead Actor", model.cast.first().name)
        assertEquals("https://image.tmdb.test/t/p/w185/lead-actor.jpg", model.cast.first().image)
    }

    private fun referenceData(): TmdbReferenceData {
        return TmdbReferenceData(
            images = TmdbImagesConfigurationDto(
                secureBaseUrl = "https://image.tmdb.test/t/p/",
                posterSizes = listOf("w342", "w500", "original"),
                backdropSizes = listOf("w780", "w1280", "original"),
                profileSizes = listOf("w45", "w185", "h632", "original"),
            ),
            genresById = mapOf(
                878 to TmdbApiGenreDto(id = 878, name = "Science Fiction"),
            ),
        )
    }
}
