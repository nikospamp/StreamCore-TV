package com.pampoukidis.streamcoretv.feature.search.common.search

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent

sealed interface SearchAction {
    data class Load(val profileId: String) : SearchAction
    data class QueryChanged(val query: String) : SearchAction
    data object ClearQuery : SearchAction
    data object SubmitQuery : SearchAction
    data class RecentSelected(val query: String) : SearchAction
    data class RecentRemoved(val query: String) : SearchAction
    data object ClearRecent : SearchAction
    data class TrendingSelected(
        val content: StreamCoreContent,
        val sourceArtworkUrl: String? = null,
    ) : SearchAction
    data class ResultSelected(
        val content: StreamCoreContent,
        val sourceArtworkUrl: String? = null,
    ) : SearchAction
    data object Retry : SearchAction
}
