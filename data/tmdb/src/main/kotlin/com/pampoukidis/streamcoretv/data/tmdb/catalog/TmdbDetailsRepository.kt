package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbMovieSummaryDto
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbApi
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.data.tmdb.network.TmdbReferenceDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class TmdbDetailsRepository @Inject internal constructor(
    private val tmdbApi: TmdbApi,
    private val referenceDataSource: TmdbReferenceDataSource,
    private val callExecutor: TmdbCallExecutor,
) : DetailsRepository {

    override suspend fun getDetails(
        profileId: String,
        contentId: String,
    ): AppResult<ContentModel> {
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

        return callExecutor.execute(operation = GET_DETAILS_OPERATION) {
            coroutineScope {
                val referenceData = async { referenceDataSource.getReferenceData() }
                val details = async { tmdbApi.getMovieDetails(movieId = movieId) }

                details.await().toModel(referenceData = referenceData.await())
            }
        }
    }

    override suspend fun getRecommendations(
        profileId: String,
        contentId: String,
    ): AppResult<List<ContentModel>> {
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

        return callExecutor.execute(operation = GET_RECOMMENDATIONS_OPERATION) {
            coroutineScope {
                val referenceData = async { referenceDataSource.getReferenceData() }
                val recommendations = async {
                    tmdbApi.getMovieRecommendations(movieId = movieId)
                }

                recommendations.await().results
                    .contentVisibleForProfile(profileId = profileId)
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
    ): AppResult.Failure? {
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

    private fun List<TmdbMovieSummaryDto>.contentVisibleForProfile(
        profileId: String,
    ): List<TmdbMovieSummaryDto> {
        if (profileId.contains(KIDS_PROFILE_MARKER, ignoreCase = true)) {
            return filterNot { movie -> movie.adult }
        }

        return this
    }

    private fun detailsFailure(
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
        const val GET_DETAILS_OPERATION = "getDetails"
        const val GET_RECOMMENDATIONS_OPERATION = "getRecommendations"
        const val MAX_RECOMMENDATIONS = 12
        const val KIDS_PROFILE_MARKER = "kids"
    }
}