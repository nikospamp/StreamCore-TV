package com.pampoukidis.streamcoretv.feature.search.common.search

import com.pampoukidis.streamcoretv.core.model.content.ContentModel

data class SearchUiState(
    val query: String = "",
    val recentQueries: List<String> = emptyList(),
    val trending: List<ContentModel> = emptyList(),
    val resultQuery: String? = null,
    val content: SearchContentState = SearchContentState.Discovery,
    val showOfflineNotice: Boolean = false,
)
