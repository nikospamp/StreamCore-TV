package com.pampoukidis.streamcoretv.feature.library.common.library

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

data class LibraryUiState(
    val isLoading: Boolean = true,
    val continueWatching: List<StreamCoreContent> = emptyList(),
    val likedContent: List<StreamCoreContent> = emptyList(),
    val myListContent: List<StreamCoreContent> = emptyList(),
    val error: StreamCoreError? = null,
)
