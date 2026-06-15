package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbDetailsRepositoryTest {

    private val subject = TmdbDetailsRepository(TmdbCatalogSource())

    @Test
    fun `returns details for profile-visible content`() {
        runTest {
            val result = subject.getDetails(
                profileId = "profile-1",
                contentId = "tmdb-orbit-fall",
            )

            assertTrue(result is AppResult.Success)
            assertEquals("Orbit Fall", (result as AppResult.Success).value.title)
        }
    }

    @Test
    fun `recommendations exclude selected content`() {
        runTest {
            val result = subject.getRecommendations(
                profileId = "profile-1",
                contentId = "tmdb-orbit-fall",
            )

            assertTrue(result is AppResult.Success)
            assertFalse(
                (result as AppResult.Success).value.any { it.id == "tmdb-orbit-fall" },
            )
        }
    }

    @Test
    fun `kids profile cannot load mature content`() {
        runTest {
            val result = subject.getDetails(
                profileId = "tmdb-profile-kids",
                contentId = "tmdb-orbit-fall",
            )

            assertTrue(result is AppResult.Failure)
        }
    }
}
