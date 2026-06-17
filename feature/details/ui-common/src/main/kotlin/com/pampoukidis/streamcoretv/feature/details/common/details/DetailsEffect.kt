package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError

sealed interface DetailsEffect {
    data class RecommendationSelected(val content: ContentModel) : DetailsEffect
    data object NavigateBack : DetailsEffect
    data class ShowError(val error: AppError) : DetailsEffect
}