package com.pampoukidis.streamcoretv.client.clientb.data.catalog

import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientBDetailsRepositoryTest {

    private val subject = ClientBDetailsRepository(ClientBCatalogSource())

    @Test
    fun `returns details for profile-visible content`() {
        runTest {
            val result = subject.getDetails(
                profileId = "profile-1",
                contentId = "client-b-last-archive",
            )

            assertTrue(result is AppResult.Success)
            assertEquals("The Last Archive", (result as AppResult.Success).value.title)
        }
    }

    @Test
    fun `recommendations exclude selected content`() {
        runTest {
            val result = subject.getRecommendations(
                profileId = "profile-1",
                contentId = "client-b-last-archive",
            )

            assertTrue(result is AppResult.Success)
            assertFalse(
                (result as AppResult.Success).value.any { it.id == "client-b-last-archive" },
            )
        }
    }

    @Test
    fun `kids profile cannot load mature content`() {
        runTest {
            val result = subject.getDetails(
                profileId = "client-b-profile-kids",
                contentId = "client-b-last-archive",
            )

            assertTrue(result is AppResult.Failure)
        }
    }
}