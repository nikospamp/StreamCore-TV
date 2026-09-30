package com.pampoukidis.streamcoretv.feature.search.common.search

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent

sealed interface SearchEffect {
    data class ContentSelected(
        val content: StreamCoreContent,
        val sourceArtworkUrl: String? = null,
    ) : SearchEffect
}
