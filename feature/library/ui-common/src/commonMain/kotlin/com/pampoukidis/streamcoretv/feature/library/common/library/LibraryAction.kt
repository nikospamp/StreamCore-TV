package com.pampoukidis.streamcoretv.feature.library.common.library

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent

sealed interface LibraryAction {
    data class Load(val profileId: String) : LibraryAction
    data object Retry : LibraryAction
    data class ContentSelected(
        val content: StreamCoreContent,
        val sourceArtworkUrl: String? = null,
    ) : LibraryAction
}
