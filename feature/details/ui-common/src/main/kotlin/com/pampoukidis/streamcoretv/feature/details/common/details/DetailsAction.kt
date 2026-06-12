package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest

sealed interface DetailsAction {
    data class Load(val request: DetailsRequest) : DetailsAction
    data object Refresh : DetailsAction
    data class RecommendationSelected(val contentId: String) : DetailsAction
    data object BackSelected : DetailsAction
}