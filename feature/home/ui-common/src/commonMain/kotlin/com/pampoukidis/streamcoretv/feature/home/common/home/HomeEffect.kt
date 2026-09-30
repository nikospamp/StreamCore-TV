package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

sealed interface HomeEffect {
    data class ContentSelected(
        val content: StreamCoreContent,
        val sourceArtworkUrl: String? = null,
    ) : HomeEffect
    data class ShowError(val error: StreamCoreError) : HomeEffect
}
