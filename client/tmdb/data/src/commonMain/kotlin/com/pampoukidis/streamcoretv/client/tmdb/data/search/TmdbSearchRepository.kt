package com.pampoukidis.streamcoretv.client.tmdb.data.search

import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.toModels
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbReferenceDataSource
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbTrendingTimeWindow
import com.pampoukidis.streamcoretv.client.tmdb.data.profile.TmdbProfileRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.feature.search.domain.SearchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class TmdbSearchRepository internal constructor(
    private val tmdbApi: TmdbApi,
    private val referenceDataSource: TmdbReferenceDataSource,
    private val callExecutor: TmdbCallExecutor,
    private val profileRepository: TmdbProfileRepository,
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

        val policy = when (val result = profileRepository.getContentPolicy(profileId)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }

        return callExecutor.execute(operation = SEARCH_OPERATION) {
            coroutineScope {
                val referenceData = async { referenceDataSource.getReferenceData() }
                val searchResponse = async {
                    tmdbApi.searchMovies(
                        query = query,
                        includeAdult = policy.includeAdult,
                    )
                }

                searchResponse.await().results
                    .filter { movie -> policy.includeAdult || !movie.adult }
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

        val policy = when (val result = profileRepository.getContentPolicy(profileId)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }

        return callExecutor.execute(operation = LOAD_TRENDING_OPERATION) {
            coroutineScope {
                val referenceData = async { referenceDataSource.getReferenceData() }
                val trendingResponse = async {
                    tmdbApi.getTrendingMovies(timeWindow = TmdbTrendingTimeWindow.Week)
                }

                trendingResponse.await().results
                    .filter { movie -> policy.includeAdult || !movie.adult }
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
        const val MAX_SEARCH_RESULTS = 20
        const val MAX_TRENDING_RESULTS = 6
    }
}
