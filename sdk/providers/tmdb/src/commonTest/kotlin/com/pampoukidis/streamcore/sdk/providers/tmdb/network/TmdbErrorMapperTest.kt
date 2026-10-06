package com.pampoukidis.streamcore.sdk.providers.tmdb.network

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.providers.tmdb.auth.TmdbAuthenticationFailureException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

class TmdbErrorMapperTest {

    private val mapper = TmdbErrorMapper()

    @Test
    fun commonTimeoutNetworkAndParsingFailuresRetainTheirMappings() {
        assertTrue(
            mapper.map("load", HttpRequestTimeoutException("https://tmdb.test", 15_000L)) is StreamCoreError.Timeout,
        )
        assertTrue(mapper.map("load", IOException("offline")) is StreamCoreError.Network)
        assertTrue(mapper.map("load", SerializationException("bad payload")) is StreamCoreError.Parsing)
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

        assertTrue(result is StreamCoreError.Authentication)
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
