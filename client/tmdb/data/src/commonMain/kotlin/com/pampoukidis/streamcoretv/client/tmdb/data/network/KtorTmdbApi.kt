package com.pampoukidis.streamcoretv.client.tmdb.data.network

import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbAccountDetailsDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbConfigurationDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbDeleteSessionRequestDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbDeleteSessionResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbGenreListResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbMovieAccountStatesDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbMovieDetailsDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbMovieListResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbRequestTokenResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbSessionRequestDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbSessionResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbValidateLoginRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.http.contentType
import io.ktor.http.path
import kotlin.coroutines.cancellation.CancellationException

/**
 * Ktor implementation of [TmdbApi].
 *
 * The shared [HttpClient] owns the base URL, authentication header, JSON handling,
 * timeout policy, and non-2xx validation. This class only declares endpoint paths
 * and query parameters.
 */
internal class KtorTmdbApi constructor(
    private val httpClient: HttpClient,
) : TmdbApi {

    override suspend fun createRequestToken(): TmdbRequestTokenResponseDto {
        return httpClient.get {
            url {
                path(API_VERSION, "authentication", "token", "new")
            }
        }.body()
    }

    override suspend fun validateRequestTokenWithLogin(
        identifier: String,
        password: String,
        requestToken: String,
    ): TmdbRequestTokenResponseDto {
        val response = httpClient.post {
            expectSuccess = false
            url {
                path(API_VERSION, "authentication", "token", "validate_with_login")
            }
            contentType(ContentType.Application.Json)
            setBody(
                TmdbValidateLoginRequestDto(
                    username = identifier,
                    password = password,
                    requestToken = requestToken,
                ),
            )
        }
        val httpCode = response.status.value
        if (httpCode == HTTP_UNAUTHORIZED || httpCode == HTTP_FORBIDDEN) {
            val backendCode = try {
                response.body<TmdbRequestTokenResponseDto>().statusCode?.toString()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                null
            }
            throw TmdbAuthenticationFailureException(
                backendCode = backendCode ?: "HTTP_$httpCode",
                message = "TMDB rejected the supplied credentials.",
            )
        }
        if (!response.status.isSuccess()) {
            if (
                httpCode in 400..499 &&
                httpCode != HTTP_REQUEST_TIMEOUT &&
                httpCode != HTTP_TOO_MANY_REQUESTS
            ) {
                val backendCode = try {
                    response.body<TmdbRequestTokenResponseDto>().statusCode
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Throwable) {
                    null
                }
                if (backendCode == INVALID_CREDENTIALS_STATUS_CODE) {
                    throw TmdbAuthenticationFailureException(
                        backendCode = backendCode.toString(),
                        message = "TMDB rejected the supplied credentials.",
                    )
                }
            }
            throw ResponseException(
                response = response,
                cachedResponseText = "TMDB login validation failed.",
            )
        }
        val payload = response.body<TmdbRequestTokenResponseDto>()
        if (payload.statusCode == INVALID_CREDENTIALS_STATUS_CODE) {
            throw TmdbAuthenticationFailureException(
                backendCode = payload.statusCode.toString(),
                message = "TMDB rejected the supplied credentials.",
            )
        }
        return payload
    }

    override suspend fun createSession(requestToken: String): TmdbSessionResponseDto {
        return httpClient.post {
            url {
                path(API_VERSION, "authentication", "session", "new")
            }
            contentType(ContentType.Application.Json)
            setBody(TmdbSessionRequestDto(requestToken = requestToken))
        }.body()
    }

    override suspend fun deleteSession(sessionId: String): TmdbDeleteSessionResponseDto {
        return httpClient.delete {
            url {
                path(API_VERSION, "authentication", "session")
            }
            contentType(ContentType.Application.Json)
            setBody(TmdbDeleteSessionRequestDto(sessionId = sessionId))
        }.body()
    }

    override suspend fun getAccountDetails(
        accountId: Int,
        sessionId: String,
    ): TmdbAccountDetailsDto {
        return httpClient.get {
            url {
                path(API_VERSION, "account", accountId.toString())
            }
            parameter("session_id", sessionId)
        }.body()
    }

    override suspend fun getMovieAccountStates(
        movieId: Int,
        sessionId: String,
    ): TmdbMovieAccountStatesDto {
        return httpClient.get {
            url {
                path(API_VERSION, "movie", movieId.toString(), "account_states")
            }
            parameter("session_id", sessionId)
        }.body()
    }

    override suspend fun getConfiguration(): TmdbConfigurationDto {
        return httpClient.get {
            url {
                path(API_VERSION, "configuration")
            }
        }.body()
    }

    override suspend fun getMovieGenres(language: String): TmdbGenreListResponseDto {
        return httpClient.get {
            url {
                path(API_VERSION, "genre", "movie", "list")
            }
            parameter("language", language)
        }.body()
    }

    override suspend fun getTrendingMovies(
        timeWindow: TmdbTrendingTimeWindow,
        language: String,
        page: Int,
    ): TmdbMovieListResponseDto {
        return httpClient.get {
            url {
                path(API_VERSION, "trending", "movie", timeWindow.pathValue)
            }
            parameter("language", language)
            parameter("page", page)
        }.body()
    }

    override suspend fun getPopularMovies(
        language: String,
        page: Int,
        region: String?,
    ): TmdbMovieListResponseDto {
        return httpClient.get {
            url {
                path(API_VERSION, "movie", "popular")
            }
            parameter("language", language)
            parameter("page", page)
            appendOptionalParameter(name = "region", value = region)
        }.body()
    }

    override suspend fun getNowPlayingMovies(
        language: String,
        page: Int,
        region: String?,
    ): TmdbMovieListResponseDto {
        return httpClient.get {
            url {
                path(API_VERSION, "movie", "now_playing")
            }
            parameter("language", language)
            parameter("page", page)
            appendOptionalParameter(name = "region", value = region)
        }.body()
    }

    override suspend fun searchMovies(
        query: String,
        includeAdult: Boolean,
        language: String,
        page: Int,
    ): TmdbMovieListResponseDto {
        return httpClient.get {
            url {
                path(API_VERSION, "search", "movie")
            }
            parameter("query", query)
            parameter("include_adult", includeAdult)
            parameter("language", language)
            parameter("page", page)
        }.body()
    }

    override suspend fun getMovieDetails(
        movieId: Int,
        language: String,
        appendToResponse: List<String>,
    ): TmdbMovieDetailsDto {
        return httpClient.get {
            url {
                path(API_VERSION, "movie", movieId.toString())
            }
            parameter("language", language)
            if (appendToResponse.isNotEmpty()) {
                parameter("append_to_response", appendToResponse.joinToString(separator = ","))
            }
        }.body()
    }

    override suspend fun getMovieRecommendations(
        movieId: Int,
        language: String,
        page: Int,
    ): TmdbMovieListResponseDto {
        return httpClient.get {
            url {
                path(API_VERSION, "movie", movieId.toString(), "recommendations")
            }
            parameter("language", language)
            parameter("page", page)
        }.body()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.appendOptionalParameter(
        name: String,
        value: String?,
    ) {
        if (!value.isNullOrBlank()) {
            parameter(name, value)
        }
    }

    private companion object {
        const val API_VERSION = "3"
        const val INVALID_CREDENTIALS_STATUS_CODE = 30
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_REQUEST_TIMEOUT = 408
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
