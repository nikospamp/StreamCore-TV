package com.pampoukidis.streamcore.sdk.providers.tmdb.details

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.providers.tmdb.catalog.TmdbReferenceDataSource
import com.pampoukidis.streamcore.sdk.providers.tmdb.network.FakeTmdbApi
import com.pampoukidis.streamcore.sdk.providers.tmdb.network.TmdbCallExecutor
import com.pampoukidis.streamcore.sdk.providers.tmdb.network.TmdbErrorMapper
import com.pampoukidis.streamcore.sdk.providers.tmdb.profile.policyTestProfileRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class TmdbDetailsRepositoryTest {

    private val api = FakeTmdbApi()
    private val subject = TmdbDetailsRepository(
        tmdbApi = api,
        referenceDataSource = TmdbReferenceDataSource(tmdbApi = api),
        callExecutor = TmdbCallExecutor(errorMapper = TmdbErrorMapper()),
        profileRepository = policyTestProfileRepository(),
    )

    @Test
    fun `requests videos with details and maps the trailer`() {
        runTest {
            api.movieVideosResponse = TmdbVideosResponseDto(
                results = listOf(
                    TmdbVideoDto(
                        id = "trailer", name = "Official Trailer", key = "abcdefghijk",
                        site = "YouTube", type = "Trailer", official = true,
                    ),
                ),
            )
            val result = subject.getDetails(profileId = "tmdb-profile-owner", contentId = "1")
            assertTrue(result is StreamCoreResult.Success)
            assertTrue(api.lastDetailsAppendToResponse.contains("videos"))
            assertEquals(
                "https://www.youtube.com/watch?v=abcdefghijk",
                (result as StreamCoreResult.Success).value.trailers.single().url,
            )
        }
    }

    @Test
    fun `returns details for TMDB movie id`() {
        runTest {
            val result = subject.getDetails(
                profileId = "tmdb-profile-owner",
                contentId = "1",
            )

            assertTrue(result is StreamCoreResult.Success)
            val content = (result as StreamCoreResult.Success).value
            assertEquals("Orbit Fall", content.title)
            assertEquals("PG-13", content.pgRatingName)
            assertEquals("Lead Actor", content.cast.first().name)
        }
    }

    @Test
    fun `recommendations exclude selected content`() {
        runTest {
            val result = subject.getRecommendations(
                profileId = "tmdb-profile-owner",
                contentId = "1",
            )

            assertTrue(result is StreamCoreResult.Success)
            assertFalse((result as StreamCoreResult.Success).value.any { content -> content.id == "1" })
        }
    }

    @Test
    fun `kids recommendations filter adult content`() {
        runTest {
            val result = subject.getRecommendations(
                profileId = "tmdb-profile-kids",
                contentId = "1",
            )

            assertTrue(result is StreamCoreResult.Success)
            assertFalse((result as StreamCoreResult.Success).value.any { content -> content.id == "99" })
        }
    }

    @Test
    fun `invalid content id returns failure`() {
        runTest {
            val result = subject.getDetails(
                profileId = "tmdb-profile-owner",
                contentId = "tmdb-orbit-fall",
            )

            assertTrue(result is StreamCoreResult.Failure)
        }
    }
}
