package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreTrailer
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest

sealed interface DetailsEffect {
    data class RecommendationSelected(
        val content: StreamCoreContent,
        val sourceArtworkUrl: String? = null,
    ) : DetailsEffect
    data class PlaySelected(val request: StreamCorePlaybackRequest) : DetailsEffect
    data class OpenTrailer(val trailer: StreamCoreTrailer) : DetailsEffect
    data object NavigateBack : DetailsEffect
    data class ShowError(val error: StreamCoreError) : DetailsEffect
}
