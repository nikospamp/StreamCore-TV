package com.pampoukidis.streamcore.sdk.providers.clientb.details

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.providers.clientb.catalog.ClientBCatalogSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ClientBDetailsRepositoryTest {

    private val subject = ClientBDetailsRepository(ClientBCatalogSource())

    @Test
    fun `returns details for profile-visible content`() {
        runTest {
            val result = subject.getDetails(
                profileId = "profile-1",
                contentId = "client-b-last-archive",
            )

            assertTrue(result is StreamCoreResult.Success)
            assertEquals("The Last Archive", (result as StreamCoreResult.Success).value.title)
        }
    }

    @Test
    fun `recommendations exclude selected content`() {
        runTest {
            val result = subject.getRecommendations(
                profileId = "profile-1",
                contentId = "client-b-last-archive",
            )

            assertTrue(result is StreamCoreResult.Success)
            assertFalse(
                (result as StreamCoreResult.Success).value.any { it.id == "client-b-last-archive" },
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

            assertTrue(result is StreamCoreResult.Failure)
        }
    }
}