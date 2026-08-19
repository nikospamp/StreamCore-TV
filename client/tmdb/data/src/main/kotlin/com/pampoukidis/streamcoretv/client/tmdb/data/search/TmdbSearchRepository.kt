package com.pampoukidis.streamcoretv.client.tmdb.data.search

import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.toModels
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbMovieSummaryDto
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbReferenceDataSource
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbTrendingTimeWindow
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.feature.search.domain.SearchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbSearchRepository @Inject internal constructor(
    private val tmdbApi: TmdbApi,
    private val referenceDataSource: TmdbReferenceDataSource,
    private val callExecutor: TmdbCallExecutor,
) : SearchRepository {

    override suspend fun search(
        profileId: String,
        query: String,
    ): AppResult<List<ContentModel>> {
        val validationFailure = validateRequest(
            profileId = profileId,
            query = query,
        )
        if (validationFailure != null) {
            return validationFailure
        }

        val isKidsProfile = profileId.isKidsProfile()
        return callExecutor.execute(operation = SEARCH_OPERATION) {
            coroutineScope {
                val referenceData = async { referenceDataSource.getReferenceData() }
                val searchResponse = async {
                    tmdbApi.searchMovies(
                        query = query,
                        includeAdult = !isKidsProfile,
                    )
                }

                searchResponse.await().results
                    .contentVisibleForProfile(isKidsProfile = isKidsProfile)
                    .take(MAX_SEARCH_RESULTS)
                    .toModels(referenceData = referenceData.await())
            }
        }
    }

    override suspend fun loadTrending(profileId: String): AppResult<List<ContentModel>> {
        if (profileId.isBlank()) {
            return searchFailure(
                operation = LOAD_TRENDING_OPERATION,
                backendCode = "PROFILE_ID_REQUIRED",
            )
        }

        val isKidsProfile = profileId.isKidsProfile()
        return callExecutor.execute(operation = LOAD_TRENDING_OPERATION) {
            coroutineScope {
                val referenceData = async { referenceDataSource.getReferenceData() }
                val trendingResponse = async {
                    tmdbApi.getTrendingMovies(timeWindow = TmdbTrendingTimeWindow.Week)
                }

                trendingResponse.await().results
                    .contentVisibleForProfile(isKidsProfile = isKidsProfile)
                    .take(MAX_TRENDING_RESULTS)
                    .toModels(referenceData = referenceData.await())
            }
        }
    }

    private fun validateRequest(
        profileId: String,
        query: String,
    ): AppResult.Failure? {
        if (profileId.isBlank()) {
            return searchFailure(
                operation = SEARCH_OPERATION,
                backendCode = "PROFILE_ID_REQUIRED",
            )
        }

        if (query.isBlank()) {
            return searchFailure(
                operation = SEARCH_OPERATION,
                backendCode = "QUERY_REQUIRED",
            )
        }

        return null
    }

    private fun String.isKidsProfile(): Boolean {
        return contains(KIDS_PROFILE_MARKER, ignoreCase = true)
    }

    private fun List<TmdbMovieSummaryDto>.contentVisibleForProfile(
        isKidsProfile: Boolean,
    ): List<TmdbMovieSummaryDto> {
        if (isKidsProfile) {
            return filterNot { movie -> movie.adult }
        }

        return this
    }

    private fun searchFailure(
        operation: String,
        backendCode: String,
    ): AppResult.Failure {
        return AppResult.Failure(
            AppError.Unknown(
                source = ErrorSource(
                    client = CLIENT,
                    operation = operation,
                    backendCode = backendCode,
                ),
            ),
        )
    }

    private companion object {
        const val CLIENT = "tmdb"
        const val SEARCH_OPERATION = "search"
        const val LOAD_TRENDING_OPERATION = "loadTrending"
        const val KIDS_PROFILE_MARKER = "kids"
        const val MAX_SEARCH_RESULTS = 20
        const val MAX_TRENDING_RESULTS = 6
    }
}
