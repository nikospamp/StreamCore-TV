package com.pampoukidis.streamcoretv.feature.library.common.library

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError

sealed interface LibraryEffect {
    data class ContentSelected(
        val content: ContentModel,
        val sourceArtworkUrl: String? = null,
    ) : LibraryEffect
    data class ShowError(val error: AppError) : LibraryEffect
}
