package com.pampoukidis.streamcore.sdk.providers.clientb.home

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.providers.clientb.catalog.ClientBCatalogSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ClientBHomeRepositoryTest {

    private val subject = ClientBHomeRepository(ClientBCatalogSource())

    @Test
    fun `kids profile receives maturity-filtered catalog`() {
        runTest {
            val result = subject.getCollections("client-b-profile-kids")

            assertTrue(result is StreamCoreResult.Success)
            assertEquals(4, (result as StreamCoreResult.Success).value.size)
            assertTrue(result.value.flatMap { it.content }.all { it.pgRatingLevel <= 7 })
        }
    }

    @Test
    fun `blank profile id returns failure`() {
        runTest {
            assertTrue(subject.getCollections("") is StreamCoreResult.Failure)
        }
    }
}
