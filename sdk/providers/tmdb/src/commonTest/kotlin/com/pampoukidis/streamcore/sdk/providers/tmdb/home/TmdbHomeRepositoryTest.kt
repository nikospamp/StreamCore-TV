package com.pampoukidis.streamcore.sdk.providers.tmdb.home

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPurpose
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
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
import kotlinx.io.IOException

class TmdbHomeRepositoryTest {

    private val api = FakeTmdbApi()
    private val subject = TmdbHomeRepository(
        tmdbApi = api,
        referenceDataSource = TmdbReferenceDataSource(tmdbApi = api),
        callExecutor = TmdbCallExecutor(errorMapper = TmdbErrorMapper()),
        profileRepository = policyTestProfileRepository(),
    )

    @Test
    fun `returns TMDB backed home rows`() {
        runTest {
            val result = subject.getCollections("tmdb-profile-owner")

            assertTrue(result is StreamCoreResult.Success)
            val rows = (result as StreamCoreResult.Success).value
            assertEquals(4, rows.size)
            assertEquals(StreamCoreCollectionPurpose.Featured, rows[0].purpose)
            assertEquals(StreamCoreCollectionPurpose.Ranked, rows[1].purpose)
            assertEquals("Orbit Fall", rows[0].content.first().title)
            assertEquals("Science Fiction", rows[0].content.first().genres.first().name)
            assertEquals("https://image.tmdb.test/t/p/w500/orbit-poster.jpg", rows[0].content.first().poster)
        }
    }

    @Test
    fun `kids profile filters adult TMDB content`() {
        runTest {
            val result = subject.getCollections("tmdb-profile-kids")

            assertTrue(result is StreamCoreResult.Success)
            val rows = (result as StreamCoreResult.Success).value
            assertFalse(rows.flatMap { row -> row.content }.any { content -> content.id == "99" })
        }
    }

    @Test
    fun `blank profile id returns failure`() {
        runTest {
            assertTrue(subject.getCollections("") is StreamCoreResult.Failure)
        }
    }

    @Test
    fun `network failures map to app network error`() {
        runTest {
            api.failure = IOException("offline")

            val result = subject.getCollections("tmdb-profile-owner")

            assertTrue(result is StreamCoreResult.Failure)
            assertTrue((result as StreamCoreResult.Failure).error is StreamCoreError.Network)
        }
    }
}
