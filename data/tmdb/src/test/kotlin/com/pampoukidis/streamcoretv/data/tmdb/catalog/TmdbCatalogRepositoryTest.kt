package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbErrorMapper
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbReferenceDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TmdbCatalogRepositoryTest {

    private val api = FakeTmdbApi()
    private val subject = TmdbCatalogRepository(
        tmdbApi = api,
        referenceDataSource = TmdbReferenceDataSource(tmdbApi = api),
        callExecutor = TmdbCallExecutor(errorMapper = TmdbErrorMapper()),
    )

    @Test
    fun `returns TMDB backed home rows`() {
        runTest {
            val result = subject.getHomeRows("profile-1")

            assertTrue(result is AppResult.Success)
            val rows = (result as AppResult.Success).value
            assertEquals(4, rows.size)
            assertEquals(RowType.Featured, rows[0].type)
            assertEquals(RowType.TopTen, rows[1].type)
            assertEquals("Orbit Fall", rows[0].content.first().title)
            assertEquals("Science Fiction", rows[0].content.first().genres.first().name)
            assertEquals("https://image.tmdb.test/t/p/w500/orbit-poster.jpg", rows[0].content.first().poster)
        }
    }

    @Test
    fun `kids profile filters adult TMDB content`() {
        runTest {
            val result = subject.getHomeRows("tmdb-profile-kids")

            assertTrue(result is AppResult.Success)
            val rows = (result as AppResult.Success).value
            assertFalse(rows.flatMap { row -> row.content }.any { content -> content.id == "99" })
        }
    }

    @Test
    fun `blank profile id returns failure`() {
        runTest {
            assertTrue(subject.getHomeRows("") is AppResult.Failure)
        }
    }

    @Test
    fun `network failures map to app network error`() {
        runTest {
            api.failure = IOException("offline")

            val result = subject.getHomeRows("profile-1")

            assertTrue(result is AppResult.Failure)
            assertTrue((result as AppResult.Failure).error is AppError.Network)
        }
    }
}
