package com.pampoukidis.streamcoretv.client.tmdb.data.catalog

import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbApiGenreDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbCastMemberDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbCreditsDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbImagesConfigurationDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbMovieDetailsDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbMovieSummaryDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbReleaseDateDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbReleaseDatesCountryDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbReleaseDatesResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbReferenceData
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbVideoDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbVideosResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogMapperTest {

    @Test
    fun `decodes appended videos and prefers latest official trailer`() {
        val details = MapperTestJson.decodeFromString<TmdbMovieDetailsDto>(
            """{
                "id": 1, "title": "Orbit Fall", "overview": "Description",
                "videos": { "results": [
                    { "id": "unofficial", "key": "abcdefghijk", "site": "YouTube",
                      "type": "Trailer", "official": false, "published_at": "2026-08-20T12:00:00Z" },
                    { "id": "old", "key": "lmnopqrstuv", "site": "YouTube",
                      "type": "Trailer", "official": true, "published_at": "2026-06-01T12:00:00Z" },
                    { "id": "new", "key": "12345678901", "site": "YouTube", "name": "Official trailer",
                      "type": "Trailer", "official": true, "published_at": "2026-08-01T12:00:00Z",
                      "iso_639_1": "en", "size": 1080 },
                    { "id": "clip", "key": "ABCDEFGHIJK", "site": "YouTube", "type": "Clip" }
                ] }
            }""",
        )
        val trailers = details.toModel(referenceData()).trailers
        assertEquals(listOf("new", "old", "unofficial"), trailers.map { it.id })
        assertEquals("https://www.youtube.com/watch?v=12345678901", trailers.first().url)
        assertEquals("Official trailer", trailers.first().title)
    }

    @Test
    fun `filters unsupported or malformed videos and deduplicates trailer links`() {
        val trailer = TmdbVideoDto(key = "abcdefghijk", site = "YouTube", type = "Trailer")
        val trailers = TmdbVideosResponseDto(
            results = listOf(
                trailer,
                trailer.copy(id = "duplicate"),
                trailer.copy(key = "", id = "blank"),
                trailer.copy(key = "abc&redirect=evil", id = "invalid"),
                trailer.copy(site = "Other", id = "unsupported"),
                trailer.copy(type = "Teaser", id = "teaser"),
                trailer.copy(site = "Vimeo", key = "123456", id = "vimeo"),
            ),
        ).toTrailers()
        assertEquals(
            listOf("https://www.youtube.com/watch?v=abcdefghijk", "https://vimeo.com/123456"),
            trailers.map { it.url },
        )
    }

    @Test
    fun `details without videos retain an empty trailer list`() {
        val details = TmdbMovieDetailsDto(id = 1, title = "Movie", overview = "")
        assertTrue(details.toModel(referenceData()).trailers.isEmpty())
        assertTrue(details.copy(videos = TmdbVideosResponseDto()).toModel(referenceData()).trailers.isEmpty())
    }

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
        ).toContentModel(referenceData = referenceData())

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

private val MapperTestJson = Json { ignoreUnknownKeys = true }
