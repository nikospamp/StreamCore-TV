package com.pampoukidis.streamcoretv.feature.search.web.search

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchContentState
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchUiState

internal fun ContentModel.webSearchFocusKey(): WebBrowseFocusKey {
    return WebBrowseFocusKey(
        destination = WebBrowseDestination.Search,
        sectionKey = row?.takeIf(String::isNotBlank) ?: SearchFallbackSectionKey,
        itemKey = id,
    )
}

internal fun ContentModel.matchesWebSearchFocusKey(key: WebBrowseFocusKey?): Boolean {
    return key != null && webSearchFocusKey() == key
}

internal fun resolveWebSearchReturnFocus(
    state: SearchUiState,
    key: WebBrowseFocusKey?,
): WebSearchReturnFocusTarget {
    if (key == null || key.destination != WebBrowseDestination.Search) {
        return WebSearchReturnFocusTarget.None
    }
    return when (val content = state.content) {
        SearchContentState.Searching,
        SearchContentState.Loading -> WebSearchReturnFocusTarget.Pending

        SearchContentState.Discovery -> {
            val index = state.trending.indexOfFirst { item -> item.matchesWebSearchFocusKey(key) }
            if (index >= 0) {
                WebSearchReturnFocusTarget.Trending(index)
            } else {
                WebSearchReturnFocusTarget.Field
            }
        }

        is SearchContentState.Results -> {
            val index = content.items.indexOfFirst { item -> item.matchesWebSearchFocusKey(key) }
            if (index >= 0) {
                WebSearchReturnFocusTarget.Results(index)
            } else {
                WebSearchReturnFocusTarget.Field
            }
        }

        is SearchContentState.Empty,
        is SearchContentState.Failure -> WebSearchReturnFocusTarget.Field
    }
}

private const val SearchFallbackSectionKey = "search:content"
