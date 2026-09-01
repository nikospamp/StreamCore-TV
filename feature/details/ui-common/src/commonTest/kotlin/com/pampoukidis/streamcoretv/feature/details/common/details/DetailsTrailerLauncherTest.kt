package com.pampoukidis.streamcoretv.feature.details.common.details

import androidx.compose.ui.platform.UriHandler
import com.pampoukidis.streamcoretv.core.model.content.TrailerModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

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

        openDetailsTrailer(
            trailer.copy(url = "https://example.com/path%20with%20spaces?next=%2Fcatalog"),
            handler,
        ) { error("Unexpected error: $it") }
        assertEquals(2, opened.size)
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
        val invalidUrls = listOf(
            "intent://trailer",
            "file:///private",
            "not a URL",
            "https:/missing-host",
            "https://%",
            "https://:443/path",
            "https://example.com:not-a-port/path",
            "https://example.com:70000/path",
            "https://example.com/%ZZ",
            "https:///path",
            "https://-invalid.example/path",
            "https://999.999.999.999/path",
        )
        invalidUrls.forEach { url ->
            openDetailsTrailer(trailer.copy(url = url), handler, errors::add)
        }
        assertEquals(invalidUrls.size, errors.size)
        assertTrue(errors.all { it is AppError.Unknown })
    }
}
