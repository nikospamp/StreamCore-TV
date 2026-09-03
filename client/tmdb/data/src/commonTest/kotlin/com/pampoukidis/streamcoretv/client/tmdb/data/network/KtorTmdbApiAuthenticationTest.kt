package com.pampoukidis.streamcoretv.client.tmdb.data.network

import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
import com.pampoukidis.streamcoretv.core.model.error.AppError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class KtorTmdbApiAuthenticationTest {

    @Test
    fun http401Code30MapsToAuthenticationWithoutParsingFallback() = runTest {
        val failure = executeValidationFailure<TmdbAuthenticationFailureException>(
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
        val failure = executeValidationFailure<TmdbAuthenticationFailureException>(
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
        val failure = executeValidationFailure<TmdbAuthenticationFailureException>(
            status = HttpStatusCode.Unauthorized,
            body = "not-json",
        )

        assertEquals("HTTP_401", failure.backendCode)
        assertIs<AppError.Authentication>(
            TmdbErrorMapper().map(operation = "loginUser", throwable = failure),
        )
    }

    @Test
    fun http403MapsToAuthentication() {
        runTest {
            val failure = executeValidationFailure<TmdbAuthenticationFailureException>(
                status = HttpStatusCode.Forbidden,
                body = "not-json",
            )

            assertEquals("HTTP_403", failure.backendCode)
            assertIs<AppError.Authentication>(
                TmdbErrorMapper().map(operation = "loginUser", throwable = failure),
            )
        }
    }

    @Test
    fun http400Code30MapsToAuthentication() {
        runTest {
            val failure = executeValidationFailure<TmdbAuthenticationFailureException>(
                status = HttpStatusCode.BadRequest,
                body = """{"success":false,"status_code":30}""",
            )

            assertEquals("30", failure.backendCode)
            assertIs<AppError.Authentication>(
                TmdbErrorMapper().map(operation = "loginUser", throwable = failure),
            )
        }
    }

    @Test
    fun http400WithoutCode30DoesNotMapToAuthentication() {
        runTest {
            val failure = executeValidationFailure<ResponseException>(
                status = HttpStatusCode.BadRequest,
                body = """{"success":false,"status_code":34}""",
            )

            assertIs<AppError.Unknown>(
                TmdbErrorMapper().map(operation = "loginUser", throwable = failure),
            )
        }
    }

    @Test
    fun http408MapsToTimeout() {
        runTest {
            val failure = executeValidationFailure<ResponseException>(
                status = HttpStatusCode.RequestTimeout,
                body = """{"success":false,"status_code":30}""",
            )

            assertIs<AppError.Timeout>(
                TmdbErrorMapper().map(operation = "loginUser", throwable = failure),
            )
        }
    }

    @Test
    fun http429MapsToServerFailure() {
        runTest {
            val failure = executeValidationFailure<ResponseException>(
                status = HttpStatusCode.TooManyRequests,
                body = """{"success":false,"status_code":30}""",
            )

            assertIs<AppError.Server>(
                TmdbErrorMapper().map(operation = "loginUser", throwable = failure),
            )
        }
    }

    @Test
    fun serverResponseMapsToServerFailure() {
        runTest {
            val failure = executeValidationFailure<ResponseException>(
                status = HttpStatusCode.ServiceUnavailable,
                body = """{"success":false,"status_code":30}""",
            )

            assertIs<AppError.Server>(
                TmdbErrorMapper().map(operation = "loginUser", throwable = failure),
            )
        }
    }

    private suspend inline fun <reified T : Throwable> executeValidationFailure(
        status: HttpStatusCode,
        body: String,
    ): T {
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
                assertFailsWith<T> {
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
