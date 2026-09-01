package com.pampoukidis.streamcoretv.client.tmdb.data.network

import com.pampoukidis.streamcoretv.core.model.error.AppError
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TmdbErrorMapperTest {

    private val mapper = TmdbErrorMapper()

    @Test
    fun commonTimeoutNetworkAndParsingFailuresRetainTheirMappings() {
        assertTrue(
            mapper.map("load", HttpRequestTimeoutException("https://tmdb.test", 15_000L)) is AppError.Timeout,
        )
        assertTrue(mapper.map("load", IOException("offline")) is AppError.Network)
        assertTrue(mapper.map("load", SerializationException("bad payload")) is AppError.Parsing)
    }

    @Test
    fun providerAuthenticationFailureRetainsBackendDetails() {
        val result = mapper.map(
            operation = "loginUser",
            throwable = TmdbAuthenticationFailureException(
                backendCode = "AUTH_FAILED",
                message = "Rejected",
            ),
        )

        assertTrue(result is AppError.Authentication)
        assertTrue(result.source?.backendCode == "AUTH_FAILED")
    }

    @Test
    fun callExecutorAlwaysRethrowsCancellation() = runTest {
        val executor = TmdbCallExecutor(errorMapper = mapper)

        assertFailsWith<CancellationException> {
            executor.execute<Unit>(operation = "cancel") {
                throw CancellationException("cancelled")
            }
        }
    }
}
