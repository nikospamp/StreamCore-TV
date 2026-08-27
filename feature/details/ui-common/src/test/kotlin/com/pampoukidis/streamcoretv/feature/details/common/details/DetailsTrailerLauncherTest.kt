package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.ui.platform.UriHandler
import com.pampoukidis.streamcoretv.core.model.content.TrailerModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailsTrailerLauncherTest {
    private val trailer = TrailerModel("trailer", "Trailer", "https://www.youtube.com/watch?v=abcdefghijk")

    @Test
    fun `opens trailer HTTPS link using the platform URI handler`() {
        val opened = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        openDetailsTrailer(trailer, handler) { error("Unexpected error: $it") }
        assertEquals(listOf(trailer.url), opened)
    }

    @Test
    fun `unavailable or blocked handlers report an error instead of crashing`() {
        listOf(IllegalArgumentException("No handler"), SecurityException("Blocked")).forEach { failure ->
            val errors = mutableListOf<AppError>()
            val handler = object : UriHandler {
                override fun openUri(uri: String) {
                    throw failure
                }
            }
            openDetailsTrailer(trailer, handler, errors::add)
            assertEquals("TRAILER_LAUNCH_UNAVAILABLE", errors.single().source?.backendCode)
        }
    }

    @Test
    fun `rejects malformed and non HTTPS links before invoking the handler`() {
        val errors = mutableListOf<AppError>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                error("Must not open $uri")
            }
        }
        listOf("intent://trailer", "file:///private", "not a URL", "https:/missing-host").forEach { url ->
            openDetailsTrailer(trailer.copy(url = url), handler, errors::add)
        }
        assertEquals(4, errors.size)
        assertTrue(errors.all { it is AppError.Unknown })
    }
}
