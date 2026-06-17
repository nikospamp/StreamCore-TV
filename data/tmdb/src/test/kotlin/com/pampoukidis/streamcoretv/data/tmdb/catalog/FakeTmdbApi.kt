package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbAccountDetailsDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbApiGenreDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbCastMemberDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbConfigurationDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbCreditsDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbDeleteSessionResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbGenreListResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbImagesConfigurationDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieDetailsDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieListResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieSummaryDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbRequestTokenResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbReleaseDateDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbReleaseDatesCountryDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbReleaseDatesResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbSessionResponseDto
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbApi
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbTrendingTimeWindow

internal class FakeTmdbApi : TmdbApi {

    var failure: Throwable? = null
    var configurationCalls = 0
        private set
    var genreCalls = 0
        private set
    var createRequestTokenCalls = 0
        private set
    var validateRequestTokenCalls = 0
        private set
    var createSessionCalls = 0
        private set
    var deleteSessionCalls = 0
        private set
    var accountDetailsCalls = 0
        private set

    var lastLoginIdentifier: String? = null
        private set
    var lastLoginPassword: String? = null
        private set
    var lastValidatedRequestToken: String? = null
        private set
    var lastSessionRequestToken: String? = null
        private set
    var lastDeletedSessionId: String? = null
        private set
    var lastAccountId: Int? = null
        private set
    var lastAccountSessionId: String? = null
        private set

    var requestTokenResponse = TmdbRequestTokenResponseDto(
        success = true,
        expiresAt = "2026-06-17 12:00:00 UTC",
        requestToken = "request-token",
    )
    var validatedTokenResponse = TmdbRequestTokenResponseDto(
        success = true,
        expiresAt = "2026-06-17 12:00:00 UTC",
        requestToken = "validated-token",
    )
    var sessionResponse = TmdbSessionResponseDto(
        success = true,
        sessionId = "session-id",
    )
    var deleteSessionResponse = TmdbDeleteSessionResponseDto(success = true)
    var accountDetailsResponse: TmdbAccountDetailsDto? = TmdbAccountDetailsDto(
        id = 548,
        username = "lead",
        displayName = "Lead",
    )

    private val imageConfiguration = TmdbImagesConfigurationDto(
        secureBaseUrl = "https://image.tmdb.test/t/p/",
        posterSizes = listOf("w342", "w500", "original"),
        backdropSizes = listOf("w780", "w1280", "original"),
        profileSizes = listOf("w45", "w185", "h632", "original"),
    )

    private val genres = listOf(
        TmdbApiGenreDto(id = 18, name = "Drama"),
        TmdbApiGenreDto(id = 28, name = "Action"),
        TmdbApiGenreDto(id = 878, name = "Science Fiction"),
        TmdbApiGenreDto(id = 10751, name = "Family"),
    )

    private val orbitFall = movieSummary(
        id = 1,
        title = "Orbit Fall",
        overview = "A rescue crew races to stabilize a failing orbital station.",
        posterPath = "/orbit-poster.jpg",
        backdropPath = "/orbit-backdrop.jpg",
        genreIds = listOf(878, 28),
        releaseDate = "2026-05-01",
        voteAverage = 8.6,
    )

    private val northernLine = movieSummary(
        id = 2,
        title = "Northern Line",
        overview = "A detective follows one final lead through a frozen border town.",
        posterPath = "/northern-poster.jpg",
        backdropPath = "/northern-backdrop.jpg",
        genreIds = listOf(18),
        releaseDate = "2026-04-12",
        voteAverage = 7.7,
    )

    private val littleComets = movieSummary(
        id = 3,
        title = "Little Comets",
        overview = "Young explorers build a telescope that changes their summer.",
        posterPath = "/little-comets-poster.jpg",
        backdropPath = "/little-comets-backdrop.jpg",
        genreIds = listOf(10751),
        releaseDate = "2026-03-20",
        voteAverage = 8.1,
    )

    private val afterHours = movieSummary(
        id = 99,
        title = "After Hours",
        overview = "A late-night thriller.",
        adult = true,
        posterPath = "/after-hours-poster.jpg",
        backdropPath = "/after-hours-backdrop.jpg",
        genreIds = listOf(18),
        releaseDate = "2026-02-10",
        voteAverage = 6.1,
    )

    override suspend fun createRequestToken(): TmdbRequestTokenResponseDto {
        throwIfNeeded()
        createRequestTokenCalls += 1
        return requestTokenResponse
    }

    override suspend fun validateRequestTokenWithLogin(
        identifier: String,
        password: String,
        requestToken: String,
    ): TmdbRequestTokenResponseDto {
        throwIfNeeded()
        validateRequestTokenCalls += 1
        lastLoginIdentifier = identifier
        lastLoginPassword = password
        lastValidatedRequestToken = requestToken
        return validatedTokenResponse
    }

    override suspend fun createSession(requestToken: String): TmdbSessionResponseDto {
        throwIfNeeded()
        createSessionCalls += 1
        lastSessionRequestToken = requestToken
        return sessionResponse
    }

    override suspend fun deleteSession(sessionId: String): TmdbDeleteSessionResponseDto {
        throwIfNeeded()
        deleteSessionCalls += 1
        lastDeletedSessionId = sessionId
        return deleteSessionResponse
    }

    override suspend fun getAccountDetails(
        accountId: Int,
        sessionId: String,
    ): TmdbAccountDetailsDto {
        throwIfNeeded()
        accountDetailsCalls += 1
        lastAccountId = accountId
        lastAccountSessionId = sessionId
        return accountDetailsResponse ?: error("No TMDB account details configured")
    }

    override suspend fun getConfiguration(): TmdbConfigurationDto {
        throwIfNeeded()
        configurationCalls += 1
        return TmdbConfigurationDto(images = imageConfiguration)
    }

    override suspend fun getMovieGenres(language: String): TmdbGenreListResponseDto {
        throwIfNeeded()
        genreCalls += 1
        return TmdbGenreListResponseDto(genres = genres)
    }

    override suspend fun getTrendingMovies(
        timeWindow: TmdbTrendingTimeWindow,
        language: String,
        page: Int,
    ): TmdbMovieListResponseDto {
        throwIfNeeded()
        val results = when (timeWindow) {
            TmdbTrendingTimeWindow.Day -> listOf(northernLine, orbitFall, littleComets)
            TmdbTrendingTimeWindow.Week -> listOf(orbitFall, afterHours, northernLine)
        }
        return movieList(results = results)
    }

    override suspend fun getPopularMovies(
        language: String,
        page: Int,
        region: String?,
    ): TmdbMovieListResponseDto {
        throwIfNeeded()
        return movieList(results = listOf(afterHours, orbitFall, northernLine, littleComets))
    }

    override suspend fun getNowPlayingMovies(
        language: String,
        page: Int,
        region: String?,
    ): TmdbMovieListResponseDto {
        throwIfNeeded()
        return movieList(results = listOf(littleComets, orbitFall))
    }

    override suspend fun getMovieDetails(
        movieId: Int,
        language: String,
        appendToResponse: List<String>,
    ): TmdbMovieDetailsDto {
        throwIfNeeded()
        return when (movieId) {
            1 -> movieDetails(
                id = 1,
                title = "Orbit Fall",
                overview = orbitFall.overview,
                posterPath = orbitFall.posterPath,
                backdropPath = orbitFall.backdropPath,
                releaseDate = orbitFall.releaseDate,
                voteAverage = orbitFall.voteAverage,
                genreIds = orbitFall.genreIds,
            )

            else -> error("No movie details configured for $movieId")
        }
    }

    override suspend fun getMovieRecommendations(
        movieId: Int,
        language: String,
        page: Int,
    ): TmdbMovieListResponseDto {
        throwIfNeeded()
        return movieList(results = listOf(orbitFall, northernLine, afterHours, littleComets))
    }

    private fun movieSummary(
        id: Int,
        title: String,
        overview: String,
        adult: Boolean = false,
        posterPath: String,
        backdropPath: String,
        genreIds: List<Int>,
        releaseDate: String,
        voteAverage: Double,
    ): TmdbMovieSummaryDto {
        return TmdbMovieSummaryDto(
            id = id,
            title = title,
            overview = overview,
            adult = adult,
            posterPath = posterPath,
            backdropPath = backdropPath,
            genreIds = genreIds,
            releaseDate = releaseDate,
            voteAverage = voteAverage,
        )
    }

    private fun movieDetails(
        id: Int,
        title: String,
        overview: String,
        posterPath: String?,
        backdropPath: String?,
        releaseDate: String?,
        voteAverage: Double,
        genreIds: List<Int>,
    ): TmdbMovieDetailsDto {
        return TmdbMovieDetailsDto(
            id = id,
            title = title,
            overview = overview,
            posterPath = posterPath,
            backdropPath = backdropPath,
            releaseDate = releaseDate,
            voteAverage = voteAverage,
            genres = genres.filter { genre -> genre.id in genreIds },
            credits = TmdbCreditsDto(
                cast = listOf(
                    TmdbCastMemberDto(
                        id = 11,
                        name = "Second Actor",
                        character = "Engineer",
                        profilePath = "/second-actor.jpg",
                        order = 1,
                    ),
                    TmdbCastMemberDto(
                        id = 10,
                        name = "Lead Actor",
                        character = "Captain",
                        profilePath = "/lead-actor.jpg",
                        order = 0,
                    ),
                ),
            ),
            releaseDates = TmdbReleaseDatesResponseDto(
                results = listOf(
                    TmdbReleaseDatesCountryDto(
                        countryCode = "US",
                        releaseDates = listOf(
                            TmdbReleaseDateDto(certification = "", type = 4),
                            TmdbReleaseDateDto(certification = "PG-13", type = 3),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun movieList(results: List<TmdbMovieSummaryDto>): TmdbMovieListResponseDto {
        return TmdbMovieListResponseDto(
            page = 1,
            results = results,
            totalPages = 1,
            totalResults = results.size,
        )
    }

    private fun throwIfNeeded() {
        failure?.let { throwable ->
            throw throwable
        }
    }
}
