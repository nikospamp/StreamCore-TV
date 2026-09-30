package com.pampoukidis.streamcoretv.feature.search.common.search

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent

data class SearchUiState(
    val query: String = "",
    val recentQueries: List<String> = emptyList(),
    val trending: List<StreamCoreContent> = emptyList(),
    val resultQuery: String? = null,
    val content: SearchContentState = SearchContentState.Discovery,
    val showOfflineNotice: Boolean = false,
)
