package com.pampoukidis.streamcoretv.client.tmdb.data.network

import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
import com.pampoukidis.streamcoretv.core.model.error.AppError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class KtorTmdbApiAuthenticationTest {

    @Test
    fun http401Code30MapsToAuthenticationWithoutParsingFallback() = runTest {
        val failure = executeValidation(
            status = HttpStatusCode.Unauthorized,
            body = """{"success":false,"status_code":30,"status_message":"fixture rejection"}""",
        )

        assertEquals("30", failure.backendCode)
        assertIs<AppError.Authentication>(
            TmdbErrorMapper().map(operation = "loginUser", throwable = failure),
        )
    }

    @Test
    fun code30PayloadMapsToAuthenticationEvenWhenHttpStatusIsSuccessful() = runTest {
        val failure = executeValidation(
            status = HttpStatusCode.OK,
            body = """{"success":false,"status_code":30,"status_message":"fixture rejection"}""",
        )

        assertEquals("30", failure.backendCode)
        assertIs<AppError.Authentication>(
            TmdbErrorMapper().map(operation = "loginUser", throwable = failure),
        )
    }

    @Test
    fun http401MapsToAuthenticationEvenWhenProviderBodyCannotBeParsed() = runTest {
        val failure = executeValidation(
            status = HttpStatusCode.Unauthorized,
            body = "not-json",
        )

        assertEquals("HTTP_401", failure.backendCode)
        assertIs<AppError.Authentication>(
            TmdbErrorMapper().map(operation = "loginUser", throwable = failure),
        )
    }

    private suspend fun executeValidation(
        status: HttpStatusCode,
        body: String,
    ): TmdbAuthenticationFailureException {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = createTmdbHttpClient(
            engine = engine,
            config = TmdbRuntimeConfig(
                baseUrl = "https://api.example.test/",
                readAccessToken = "fixture-token",
                accountId = "42",
            ),
            json = Json { ignoreUnknownKeys = true },
        )
        return try {
            withContext(Dispatchers.Default) {
                assertFailsWith<TmdbAuthenticationFailureException> {
                    KtorTmdbApi(client).validateRequestTokenWithLogin(
                        identifier = "fixture-user",
                        password = "fixture-password",
                        requestToken = "fixture-request-token",
                    )
                }
            }
        } finally {
            client.close()
        }
    }
}
