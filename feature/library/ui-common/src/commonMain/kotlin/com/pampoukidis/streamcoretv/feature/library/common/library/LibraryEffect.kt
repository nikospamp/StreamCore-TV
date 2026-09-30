package com.pampoukidis.streamcoretv.feature.library.common.library

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

sealed interface LibraryEffect {
    data class ContentSelected(
        val content: StreamCoreContent,
        val sourceArtworkUrl: String? = null,
    ) : LibraryEffect
    data class ShowError(val error: StreamCoreError) : LibraryEffect
}
