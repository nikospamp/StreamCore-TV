package com.pampoukidis.streamcore.sdk.model.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AppResultErrorTest {
    @Test
    fun successRetainsValue() {
        val result: StreamCoreResult<String> = StreamCoreResult.Success("value")

        assertEquals("value", assertIs<StreamCoreResult.Success<String>>(result).value)
    }

    @Test
    fun failureRemainsCovariant() {
        val result: StreamCoreResult<String> = StreamCoreResult.Failure(StreamCoreError.Network())

        assertIs<StreamCoreError.Network>(assertIs<StreamCoreResult.Failure>(result).error)
    }

    @Test
    fun errorSourceRetainsBackendDiagnostics() {
        val source = StreamCoreErrorSource(
            client = "tmdb",
            operation = "details",
            httpCode = 503,
            backendCode = "temporarily_unavailable",
            backendMessage = "retry",
            requestId = "request-1",
        )

        assertEquals(503, source.httpCode)
        assertEquals("request-1", source.requestId)
    }

    @Test
    fun errorsDefaultToNoSource() {
        assertNull(StreamCoreError.Unknown().source)
    }
}
