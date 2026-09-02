package com.pampoukidis.streamcoretv.client.tmdb.data.network

import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TmdbHttpClientFactoryTest {

    @Test
    fun requestPolicyPreservesBaseUrlAuthorizationAcceptAndTimeouts(): TestResult {
        return runTest {
            val engine = MockEngine { request ->
                assertEquals("https://api.tmdb.test/3/configuration", request.url.toString())
                assertEquals("Bearer token", request.headers[HttpHeaders.Authorization])
                assertEquals(ContentType.Application.Json.toString(), request.headers[HttpHeaders.Accept])
                respond(
                    content = "{}",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
            val client = createTmdbHttpClient(
                engine = engine,
                config = TmdbRuntimeConfig(
                    baseUrl = "https://api.tmdb.test",
                    readAccessToken = "token",
                    accountId = "42",
                ),
                json = Json { ignoreUnknownKeys = true },
            )

            val responseBody = withContext(Dispatchers.Default) {
                client.get("3/configuration").bodyAsText()
            }
            assertEquals("{}", responseBody)
            assertEquals(10_000L, CONNECT_TIMEOUT_MILLIS)
            assertEquals(15_000L, REQUEST_TIMEOUT_MILLIS)
            assertEquals(15_000L, SOCKET_TIMEOUT_MILLIS)
            client.close()
        }
    }

    @Test
    fun emptyRuntimeCredentialsDoNotCrashClientCreationOrAddAuthorization(): TestResult {
        return runTest {
            val engine = MockEngine { request ->
                assertNull(request.headers[HttpHeaders.Authorization])
                respond(content = "{}")
            }
            val client = createTmdbHttpClient(
                engine = engine,
                config = TmdbRuntimeConfig(
                    baseUrl = "",
                    readAccessToken = "",
                    accountId = "",
                ),
                json = Json,
            )

            withContext(Dispatchers.Default) {
                client.get("https://fallback.test/ping")
            }
            client.close()
        }
    }
}
