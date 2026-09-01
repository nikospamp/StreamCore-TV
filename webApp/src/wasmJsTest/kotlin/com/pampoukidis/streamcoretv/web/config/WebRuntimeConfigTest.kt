package com.pampoukidis.streamcoretv.web.config

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebRuntimeConfigTest {
    @Test
    fun validConfigMapsToTmdbRuntimeConfig() {
        val config = validConfig()

        assertIs<WebRuntimeConfigValidationResult.Valid>(config.validate())
        assertEquals(config.tmdbBaseUrl, config.toTmdbRuntimeConfig().baseUrl)
        assertEquals(config.tmdbReadAccessToken, config.toTmdbRuntimeConfig().readAccessToken)
        assertEquals(config.tmdbAccountId, config.toTmdbRuntimeConfig().accountId)
    }

    @Test
    fun nonHttpsBaseUrlIsRejected() {
        val result = validConfig().copy(tmdbBaseUrl = "http://api.example.test/3/").validate()

        assertIs<WebRuntimeConfigValidationResult.Invalid>(result)
        assertTrue(result.guidance.contains("HTTPS"))
    }

    @Test
    fun blankRequiredFieldsAreReported() {
        val result = validConfig().copy(
            tmdbReadAccessToken = " ",
            tmdbAccountId = "",
        ).validate()

        assertIs<WebRuntimeConfigValidationResult.Invalid>(result)
        assertTrue(result.guidance.contains("tmdbReadAccessToken"))
        assertTrue(result.guidance.contains("tmdbAccountId"))
    }

    @Test
    fun loaderRejectsMalformedJsonWithoutStartingGraph(): TestResult {
        return runTest {
            val loader = WebRuntimeConfigLoader(loadText = { "not-json" })

            val result = loader.load()

            assertIs<WebRuntimeConfigLoadResult.Failure>(result)
            assertTrue(result.guidance.contains("schema"))
        }
    }

    @Test
    fun loaderAcceptsValidRuntimeJson(): TestResult {
        return runTest {
            val loader = WebRuntimeConfigLoader(
                loadText = {
                    """
                    {
                      "tmdbBaseUrl": "https://api.example.test/3/",
                      "tmdbReadAccessToken": "browser-visible-token",
                      "tmdbAccountId": "42"
                    }
                    """.trimIndent()
                },
            )

            val result = loader.load()

            assertIs<WebRuntimeConfigLoadResult.Success>(result)
            assertEquals("42", result.config.tmdbAccountId)
        }
    }

    private fun validConfig(): WebRuntimeConfig {
        return WebRuntimeConfig(
            tmdbBaseUrl = "https://api.example.test/3/",
            tmdbReadAccessToken = "browser-visible-token",
            tmdbAccountId = "42",
        )
    }
}
