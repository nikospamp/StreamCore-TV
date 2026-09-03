package com.pampoukidis.streamcoretv.feature.search.web.search

internal sealed interface WebSearchReturnFocusTarget {
    data object None : WebSearchReturnFocusTarget
    data object Pending : WebSearchReturnFocusTarget
    data object Field : WebSearchReturnFocusTarget
    data class Trending(val index: Int) : WebSearchReturnFocusTarget
    data class Results(val index: Int) : WebSearchReturnFocusTarget
}
