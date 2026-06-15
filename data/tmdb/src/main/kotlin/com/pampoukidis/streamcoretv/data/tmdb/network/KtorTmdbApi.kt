package com.pampoukidis.streamcoretv.data.tmdb.network

import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbConfigurationDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbGenreListResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieDetailsDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieListResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.path
import javax.inject.Inject

/**
 * Ktor implementation of [TmdbApi].
 *
 * The shared [HttpClient] owns the base URL, authentication header, JSON handling,
 * timeout policy, and non-2xx validation. This class only declares endpoint paths
 * and query parameters.
 */
internal class KtorTmdbApi @Inject constructor(
    private val httpClient: HttpClient,
) : TmdbApi {

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
    }
}
