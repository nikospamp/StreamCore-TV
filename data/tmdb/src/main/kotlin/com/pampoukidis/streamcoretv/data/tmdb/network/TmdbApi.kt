package com.pampoukidis.streamcoretv.data.tmdb.network

import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbAccountDetailsDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbConfigurationDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbDeleteSessionResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbGenreListResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieDetailsDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieListResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbRequestTokenResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbSessionResponseDto

/**
 * Internal TMDB HTTP contract used by the provider repositories.
 *
 * This interface keeps Ktor out of repository tests and keeps TMDB DTOs scoped to
 * the provider module. Methods intentionally mirror the remote endpoints instead
 * of returning app models; mapping happens in the catalog layer.
 */
internal interface TmdbApi {

    suspend fun createRequestToken(): TmdbRequestTokenResponseDto

    suspend fun validateRequestTokenWithLogin(
        identifier: String,
        password: String,
        requestToken: String,
    ): TmdbRequestTokenResponseDto

    suspend fun createSession(
        requestToken: String,
    ): TmdbSessionResponseDto

    suspend fun deleteSession(
        sessionId: String,
    ): TmdbDeleteSessionResponseDto

    suspend fun getAccountDetails(
        accountId: Int,
        sessionId: String,
    ): TmdbAccountDetailsDto

    /**
     * Loads global TMDB configuration, mainly image base URLs and supported sizes.
     */
    suspend fun getConfiguration(): TmdbConfigurationDto

    /**
     * Loads localized movie genres so list responses can map genre ids to names.
     */
    suspend fun getMovieGenres(
        language: String = DEFAULT_LANGUAGE,
    ): TmdbGenreListResponseDto

    /**
     * Loads daily or weekly trending movies from `/3/trending/movie/{time_window}`.
     */
    suspend fun getTrendingMovies(
        timeWindow: TmdbTrendingTimeWindow,
        language: String = DEFAULT_LANGUAGE,
        page: Int = DEFAULT_PAGE,
    ): TmdbMovieListResponseDto

    /**
     * Loads popular movies, optionally scoped to a region.
     */
    suspend fun getPopularMovies(
        language: String = DEFAULT_LANGUAGE,
        page: Int = DEFAULT_PAGE,
        region: String? = null,
    ): TmdbMovieListResponseDto

    /**
     * Loads now-playing movies, optionally scoped to a region.
     */
    suspend fun getNowPlayingMovies(
        language: String = DEFAULT_LANGUAGE,
        page: Int = DEFAULT_PAGE,
        region: String? = null,
    ): TmdbMovieListResponseDto

    /**
     * Loads a movie details payload. Credits and release dates are appended by default
     * because the details screen needs cast and certification data.
     */
    suspend fun getMovieDetails(
        movieId: Int,
        language: String = DEFAULT_LANGUAGE,
        appendToResponse: List<String> = DEFAULT_DETAILS_APPEND_TO_RESPONSE,
    ): TmdbMovieDetailsDto

    /**
     * Loads TMDB recommendations for a movie id.
     */
    suspend fun getMovieRecommendations(
        movieId: Int,
        language: String = DEFAULT_LANGUAGE,
        page: Int = DEFAULT_PAGE,
    ): TmdbMovieListResponseDto

    private companion object {
        const val DEFAULT_LANGUAGE = "en-US"
        const val DEFAULT_PAGE = 1
        val DEFAULT_DETAILS_APPEND_TO_RESPONSE = listOf("credits", "release_dates")
    }
}
