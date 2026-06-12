package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcoretv.core.model.error.AppError

sealed interface DetailsEffect {
    data class RecommendationSelected(val contentId: String) : DetailsEffect
    data object NavigateBack : DetailsEffect
    data class ShowError(val error: AppError) : DetailsEffect
}