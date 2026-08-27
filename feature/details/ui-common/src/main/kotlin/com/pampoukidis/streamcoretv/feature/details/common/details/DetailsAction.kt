package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest

sealed interface DetailsAction {
    data class Load(
        val request: DetailsRequest,
        val initialContent: ContentModel? = null,
    ) : DetailsAction
    data object Refresh : DetailsAction
    data class RecommendationSelected(val content: ContentModel) : DetailsAction
    data object PlaySelected : DetailsAction
    data object TrailerSelected : DetailsAction
    data object LikeToggled : DetailsAction
    data object MyListToggled : DetailsAction
    data object BackSelected : DetailsAction
}
