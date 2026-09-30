package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreDetailsRequest

sealed interface DetailsAction {
    data class Load(
        val request: StreamCoreDetailsRequest,
        val initialContent: StreamCoreContent? = null,
    ) : DetailsAction
    data object Refresh : DetailsAction
    data class RecommendationSelected(
        val content: StreamCoreContent,
        val sourceArtworkUrl: String? = null,
    ) : DetailsAction
    data object PlaySelected : DetailsAction
    data object TrailerSelected : DetailsAction
    data object LikeToggled : DetailsAction
    data object MyListToggled : DetailsAction
    data object BackSelected : DetailsAction
}
