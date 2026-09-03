package com.pampoukidis.streamcoretv.feature.search.web.search

import com.pampoukidis.streamcoretv.feature.search.common.search.SearchAction

internal fun submitCommittedSearch(
    committedValue: String,
    onAction: (SearchAction) -> Unit,
) {
    onAction(SearchAction.QueryChanged(committedValue))
    onAction(SearchAction.SubmitQuery)
}
