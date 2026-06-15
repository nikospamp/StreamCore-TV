package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbErrorMapper
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbReferenceDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbDetailsRepositoryTest {

    private val api = FakeTmdbApi()
    private val subject = TmdbDetailsRepository(
        tmdbApi = api,
        referenceDataSource = TmdbReferenceDataSource(tmdbApi = api),
        callExecutor = TmdbCallExecutor(errorMapper = TmdbErrorMapper()),
    )

    @Test
    fun `returns details for TMDB movie id`() {
        runTest {
            val result = subject.getDetails(
                profileId = "profile-1",
                contentId = "1",
            )

            assertTrue(result is AppResult.Success)
            val content = (result as AppResult.Success).value
            assertEquals("Orbit Fall", content.title)
            assertEquals("PG-13", content.pgRatingName)
            assertEquals("Lead Actor", content.cast.first().name)
        }
    }

    @Test
    fun `recommendations exclude selected content`() {
        runTest {
            val result = subject.getRecommendations(
                profileId = "profile-1",
                contentId = "1",
            )

            assertTrue(result is AppResult.Success)
            assertFalse((result as AppResult.Success).value.any { content -> content.id == "1" })
        }
    }

    @Test
    fun `kids recommendations filter adult content`() {
        runTest {
            val result = subject.getRecommendations(
                profileId = "tmdb-profile-kids",
                contentId = "1",
            )

            assertTrue(result is AppResult.Success)
            assertFalse((result as AppResult.Success).value.any { content -> content.id == "99" })
        }
    }

    @Test
    fun `invalid content id returns failure`() {
        runTest {
            val result = subject.getDetails(
                profileId = "profile-1",
                contentId = "tmdb-orbit-fall",
            )

            assertTrue(result is AppResult.Failure)
        }
    }
}
