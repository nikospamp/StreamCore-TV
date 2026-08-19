package com.pampoukidis.streamcoretv.client.tmdb.data.search

import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.FakeTmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbMovieSummaryDto
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbErrorMapper
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbReferenceDataSource
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TmdbSearchRepositoryTest {

    private val api = FakeTmdbApi()
    private val subject = TmdbSearchRepository(
        tmdbApi = api,
        referenceDataSource = TmdbReferenceDataSource(tmdbApi = api),
        callExecutor = TmdbCallExecutor(errorMapper = TmdbErrorMapper()),
    )

    @Test
    fun `search preserves provider order and maps reference data`() {
        runTest {
            val result = subject.search(
                profileId = "profile-1",
                query = "orbit",
            )

            assertTrue(result is AppResult.Success)
            val content = (result as AppResult.Success).value
            assertEquals(listOf("1", "99", "2", "3"), content.map { item -> item.id })
            assertEquals("orbit", api.lastSearchQuery)
            assertEquals(true, api.lastSearchIncludeAdult)
            assertEquals("Science Fiction", content.first().genres.first().name)
            assertEquals("https://image.tmdb.test/t/p/w500/orbit-poster.jpg", content.first().poster)
        }
    }

    @Test
    fun `search caps results at twenty`() {
        runTest {
            api.searchMoviesResults = (1..25).map { index -> movie(index = index) }

            val result = subject.search(
                profileId = "profile-1",
                query = "movie",
            )

            assertTrue(result is AppResult.Success)
            assertEquals(20, (result as AppResult.Success).value.size)
            assertEquals("1", result.value.first().id)
            assertEquals("20", result.value.last().id)
        }
    }

    @Test
    fun `kids search excludes adult results and disables adult API content`() {
        runTest {
            val result = subject.search(
                profileId = "tmdb-profile-kids",
                query = "hours",
            )

            assertTrue(result is AppResult.Success)
            assertFalse((result as AppResult.Success).value.any { content -> content.id == "99" })
            assertEquals(false, api.lastSearchIncludeAdult)
        }
    }

    @Test
    fun `trending preserves provider order and caps visible content at six`() {
        runTest {
            api.trendingWeekResults = (1..8).map { index -> movie(index = index) }

            val result = subject.loadTrending(profileId = "profile-1")

            assertTrue(result is AppResult.Success)
            assertEquals(listOf("1", "2", "3", "4", "5", "6"), (result as AppResult.Success).value.map { it.id })
        }
    }

    @Test
    fun `kids trending excludes adult results`() {
        runTest {
            val result = subject.loadTrending(profileId = "tmdb-profile-kids")

            assertTrue(result is AppResult.Success)
            assertFalse((result as AppResult.Success).value.any { content -> content.id == "99" })
        }
    }

    @Test
    fun `blank inputs return failures without calling search endpoint`() {
        runTest {
            assertTrue(subject.search(profileId = "", query = "orbit") is AppResult.Failure)
            assertTrue(subject.search(profileId = "profile-1", query = " ") is AppResult.Failure)
            assertTrue(subject.loadTrending(profileId = "") is AppResult.Failure)
            assertEquals(0, api.searchMoviesCalls)
        }
    }

    @Test
    fun `network failures map to app network error`() {
        runTest {
            api.failure = IOException("offline")

            val result = subject.search(
                profileId = "profile-1",
                query = "orbit",
            )

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Network)
        }
    }

    private fun movie(index: Int): TmdbMovieSummaryDto {
        return TmdbMovieSummaryDto(
            id = index,
            title = "Movie $index",
            overview = "Overview $index",
            posterPath = "/poster-$index.jpg",
            backdropPath = "/backdrop-$index.jpg",
            genreIds = listOf(18),
            releaseDate = "2026-01-01",
            voteAverage = 7.0,
        )
    }
}
