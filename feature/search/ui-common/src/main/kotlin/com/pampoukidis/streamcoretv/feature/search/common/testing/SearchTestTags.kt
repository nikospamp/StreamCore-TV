package com.pampoukidis.streamcoretv.feature.search.common.testing

object SearchTestTags {
    const val Screen = "search:screen"
    const val Field = "search:field"
    const val ClearQuery = "search:clear-query"
    const val RecentList = "search:recent-list"
    const val TrendingList = "search:trending-list"
    const val ResultsGrid = "search:results-grid"
    const val Loading = "search:loading"
    const val Empty = "search:empty"
    const val Failure = "search:failure"

    fun recent(query: String): String {
        return "search:recent:$query"
    }

    fun removeRecent(query: String): String {
        return "search:recent:$query:remove"
    }

    fun result(contentId: String): String {
        return "search:result:$contentId"
    }
}
