package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbContentDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbDetailsRepository @Inject constructor(
    private val catalogSource: TmdbCatalogSource,
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

        val content = findContent(
            profileId = profileId,
            contentId = contentId,
        ) ?: return detailsFailure(
            operation = GET_DETAILS_OPERATION,
            backendCode = "CONTENT_NOT_FOUND",
        )

        return AppResult.Success(content.toModel())
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

        val profileCatalog = catalogSource.contentForProfile(profileId)
        val selected = profileCatalog.firstOrNull { it.contentId == contentId }
            ?: return detailsFailure(
                operation = GET_RECOMMENDATIONS_OPERATION,
                backendCode = "CONTENT_NOT_FOUND",
            )

        return AppResult.Success(
            recommendations(
                selected = selected,
                catalog = profileCatalog,
            ).map { it.toModel() },
        )
    }

    private fun findContent(
        profileId: String,
        contentId: String,
    ): TmdbContentDto? {
        return catalogSource.contentForProfile(profileId)
            .firstOrNull { it.contentId == contentId }
    }

    private fun recommendations(
        selected: TmdbContentDto,
        catalog: List<TmdbContentDto>,
    ): List<TmdbContentDto> {
        val selectedGenreIds = selected.genres.map { it.genreId }.toSet()
        val candidates = catalog.filterNot { it.contentId == selected.contentId }

        return candidates
            .sortedWith(
                compareByDescending<TmdbContentDto> { content ->
                    content.genres.any { it.genreId in selectedGenreIds }
                }.thenByDescending { it.score },
            )
            .take(MAX_RECOMMENDATIONS)
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
    }
}