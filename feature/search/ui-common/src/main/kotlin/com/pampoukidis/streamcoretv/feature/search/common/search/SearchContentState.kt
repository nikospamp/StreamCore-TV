package com.pampoukidis.streamcoretv.feature.search.common.search

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError

sealed interface SearchContentState {
    data object Discovery : SearchContentState
    data object Searching : SearchContentState
    data object Loading : SearchContentState
    data class Results(val items: List<ContentModel>) : SearchContentState
    data class Empty(val query: String) : SearchContentState
    data class Failure(val error: AppError) : SearchContentState
}
