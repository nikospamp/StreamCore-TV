package com.pampoukidis.streamcoretv.feature.library.common.library

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError

data class LibraryUiState(
    val isLoading: Boolean = true,
    val continueWatching: List<ContentModel> = emptyList(),
    val likedContent: List<ContentModel> = emptyList(),
    val myListContent: List<ContentModel> = emptyList(),
    val error: AppError? = null,
)
