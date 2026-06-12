package com.pampoukidis.streamcoretv.feature.details.domain

import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.details.data.DetailsModel
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest
import javax.inject.Inject

class LoadDetailsUseCase @Inject constructor(
    private val detailsRepository: DetailsRepository,
) {

    suspend operator fun invoke(request: DetailsRequest): AppResult<DetailsModel> {
        val contentResult = detailsRepository.getDetails(
            profileId = request.profileId,
            contentId = request.contentId,
        )

        if (contentResult is AppResult.Failure) {
            return contentResult
        }

        val recommendationsResult = detailsRepository.getRecommendations(
            profileId = request.profileId,
            contentId = request.contentId,
        )

        return when (recommendationsResult) {
            is AppResult.Success -> AppResult.Success(
                DetailsModel(
                    content = (contentResult as AppResult.Success).value,
                    recommendations = recommendationsResult.value,
                ),
            )

            is AppResult.Failure -> recommendationsResult
        }
    }
}
