package com.pampoukidis.streamcore.sdk.providers.tmdb.search

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.providers.tmdb.catalog.TmdbReferenceDataSource
import com.pampoukidis.streamcore.sdk.providers.tmdb.catalog.toModels
import com.pampoukidis.streamcore.sdk.providers.tmdb.network.TmdbApi
import com.pampoukidis.streamcore.sdk.providers.tmdb.network.TmdbCallExecutor
import com.pampoukidis.streamcore.sdk.providers.tmdb.network.TmdbTrendingTimeWindow
import com.pampoukidis.streamcore.sdk.providers.tmdb.profile.TmdbProfileRepository
import com.pampoukidis.streamcore.sdk.runtime.search.SearchProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class TmdbSearchRepository constructor(
    private val tmdbApi: TmdbApi,
    private val referenceDataSource: TmdbReferenceDataSource,
    private val callExecutor: TmdbCallExecutor,
    private val profileRepository: TmdbProfileRepository,
) : SearchProvider {

    override suspend fun search(
        profileId: String,
        query: String,
    ): StreamCoreResult<List<StreamCoreContent>> {
        val validationFailure = validateRequest(
            profileId = profileId,
            query = query,
        )
        if (validationFailure != null) {
            return validationFailure
        }

        val policy = when (val result = profileRepository.getContentPolicy(profileId)) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> return result
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

    override suspend fun loadTrending(profileId: String): StreamCoreResult<List<StreamCoreContent>> {
        if (profileId.isBlank()) {
            return searchFailure(
                operation = LOAD_TRENDING_OPERATION,
                backendCode = "PROFILE_ID_REQUIRED",
            )
        }

        val policy = when (val result = profileRepository.getContentPolicy(profileId)) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> return result
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
    ): StreamCoreResult.Failure? {
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
    ): StreamCoreResult.Failure {
        return StreamCoreResult.Failure(
            StreamCoreError.Unknown(
                source = StreamCoreErrorSource(
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
