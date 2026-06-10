package com.pampoukidis.streamcoretv.data.client.catalog

import com.pampoukidis.streamcoretv.core.model.error.AppResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientBCatalogRepositoryTest {

    private val subject = ClientBCatalogRepository()

    @Test
    fun `kids profile receives maturity-filtered catalog`() {
        runTest {
            val result = subject.getHomeRows("client-b-profile-kids")

            assertTrue(result is AppResult.Success)
            assertEquals(4, (result as AppResult.Success).value.size)
            assertTrue(result.value.flatMap { it.content }.all { it.pgRatingLevel <= 7 })
        }
    }

    @Test
    fun `blank profile id returns failure`() {
        runTest {
            assertTrue(subject.getHomeRows("") is AppResult.Failure)
        }
    }
}
