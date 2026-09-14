package com.pampoukidis.streamcoretv.feature.library.common.library

import com.pampoukidis.streamcoretv.core.model.content.ContentModel

sealed interface LibraryAction {
    data class Load(val profileId: String) : LibraryAction
    data object Retry : LibraryAction
    data class ContentSelected(
        val content: ContentModel,
        val sourceArtworkUrl: String? = null,
    ) : LibraryAction
}
