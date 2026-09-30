package com.pampoukidis.streamcoretv.feature.search.common.search

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

sealed interface SearchContentState {
    data object Discovery : SearchContentState
    data object Searching : SearchContentState
    data object Loading : SearchContentState
    data class Results(val items: List<StreamCoreContent>) : SearchContentState
    data class Empty(val query: String) : SearchContentState
    data class Failure(val error: StreamCoreError) : SearchContentState
}
