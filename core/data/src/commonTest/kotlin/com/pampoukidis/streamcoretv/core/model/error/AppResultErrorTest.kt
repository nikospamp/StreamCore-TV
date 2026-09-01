package com.pampoukidis.streamcoretv.core.model.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AppResultErrorTest {
    @Test
    fun successRetainsValue() {
        val result: AppResult<String> = AppResult.Success("value")

        assertEquals("value", assertIs<AppResult.Success<String>>(result).value)
    }

    @Test
    fun failureRemainsCovariant() {
        val result: AppResult<String> = AppResult.Failure(AppError.Network())

        assertIs<AppError.Network>(assertIs<AppResult.Failure>(result).error)
    }

    @Test
    fun errorSourceRetainsBackendDiagnostics() {
        val source = ErrorSource(
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
        assertNull(AppError.Unknown().source)
    }
}
