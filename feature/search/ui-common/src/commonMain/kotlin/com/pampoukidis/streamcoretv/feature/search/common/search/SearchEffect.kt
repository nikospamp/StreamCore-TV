package com.pampoukidis.streamcoretv.feature.search.common.search

import com.pampoukidis.streamcoretv.core.model.content.ContentModel

sealed interface SearchEffect {
    data class ContentSelected(
        val content: ContentModel,
        val sourceArtworkUrl: String? = null,
    ) : SearchEffect
}
