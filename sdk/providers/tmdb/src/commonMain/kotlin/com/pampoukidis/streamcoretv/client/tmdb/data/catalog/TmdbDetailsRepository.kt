package com.pampoukidis.streamcoretv.client.tmdb.data.catalog

import com.pampoukidis.streamcore.sdk.runtime.integration.provider.DetailsProvider
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbReferenceDataSource
import com.pampoukidis.streamcoretv.client.tmdb.data.profile.TmdbProfileRepository
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class TmdbDetailsRepository constructor(
    private val tmdbApi: TmdbApi,
    private val referenceDataSource: TmdbReferenceDataSource,
    private val callExecutor: TmdbCallExecutor,
    private val profileRepository: TmdbProfileRepository,
) : DetailsProvider {

    override suspend fun getDetails(
        profileId: String,
        contentId: String,
    ): StreamCoreResult<StreamCoreContent> {
        val validationFailure = validateRequest(
            profileId = profileId,
            contentId = contentId,
            operation = GET_DETAILS_OPERATION,
        )
        if (validationFailure != null) {
            return validationFailure
        }

        val movieId = contentId.toMovieIdOrNull() ?: return detailsFailure(
            operation = GET_DETAILS_OPERATION,
            backendCode = "CONTENT_ID_INVALID",
        )

        val policy = when (val result = profileRepository.getContentPolicy(profileId)) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> return result
        }
        val result = callExecutor.execute(operation = GET_DETAILS_OPERATION) {
            coroutineScope {
                val referenceData = async { referenceDataSource.getReferenceData() }
                val details = async { tmdbApi.getMovieDetails(movieId = movieId) }
                val movie = details.await()
                if (!policy.includeAdult && movie.adult) {
                    null
                } else {
                    movie.toModel(referenceData = referenceData.await())
                }
            }
        }
        return when (result) {
            is StreamCoreResult.Failure -> result
            is StreamCoreResult.Success -> result.value?.let { StreamCoreResult.Success(it) }
                ?: StreamCoreResult.Failure(StreamCoreError.InvalidContext())
        }
    }

    override suspend fun getRecommendations(
        profileId: String,
        contentId: String,
    ): StreamCoreResult<List<StreamCoreContent>> {
        val validationFailure = validateRequest(
            profileId = profileId,
            contentId = contentId,
            operation = GET_RECOMMENDATIONS_OPERATION,
        )
        if (validationFailure != null) {
            return validationFailure
        }

        val movieId = contentId.toMovieIdOrNull() ?: return detailsFailure(
            operation = GET_RECOMMENDATIONS_OPERATION,
            backendCode = "CONTENT_ID_INVALID",
        )

        val policy = when (val result = profileRepository.getContentPolicy(profileId)) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> return result
        }

        return callExecutor.execute(operation = GET_RECOMMENDATIONS_OPERATION) {
            coroutineScope {
                val referenceData = async { referenceDataSource.getReferenceData() }
                val recommendations = async {
                    tmdbApi.getMovieRecommendations(movieId = movieId)
                }

                recommendations.await().results
                    .filter { movie -> policy.includeAdult || !movie.adult }
                    .filterNot { movie -> movie.id == movieId }
                    .take(MAX_RECOMMENDATIONS)
                    .toModels(referenceData = referenceData.await())
            }
        }
    }

    private fun validateRequest(
        profileId: String,
        contentId: String,
        operation: String,
    ): StreamCoreResult.Failure? {
        if (profileId.isBlank()) {
            return detailsFailure(
                operation = operation,
                backendCode = "PROFILE_ID_REQUIRED",
            )
        }

        if (contentId.isBlank()) {
            return detailsFailure(
                operation = operation,
                backendCode = "CONTENT_ID_REQUIRED",
            )
        }

        return null
    }

    private fun String.toMovieIdOrNull(): Int? {
        return toIntOrNull()?.takeIf { movieId -> movieId > 0 }
    }

    private fun detailsFailure(
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
        const val GET_DETAILS_OPERATION = "getDetails"
        const val GET_RECOMMENDATIONS_OPERATION = "getRecommendations"
        const val MAX_RECOMMENDATIONS = 12
    }
}
