package com.pampoukidis.streamcoretv.feature.search.web.search

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureIds
import com.pampoukidis.streamcoretv.core.ui.web.testing.WebBrowseFixtureScenario
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchContentState
import com.pampoukidis.streamcoretv.feature.search.common.search.SearchUiState
import com.pampoukidis.streamcoretv.feature.search.common.testing.SearchPreviewData

internal object WebSearchPreviewFixtures {
    val discovery: SearchUiState = SearchPreviewData.discovery.withBackendFreeArtwork()

    fun state(scenario: WebBrowseFixtureScenario): SearchUiState {
        return when (scenario) {
            WebBrowseFixtureScenario.Loading -> SearchPreviewData.loading.withBackendFreeArtwork()
            WebBrowseFixtureScenario.Content -> SearchPreviewData.results.withBackendFreeArtwork()
            WebBrowseFixtureScenario.Empty -> SearchPreviewData.empty.withBackendFreeArtwork()
            WebBrowseFixtureScenario.Offline -> SearchPreviewData.results.withBackendFreeArtwork().copy(
                showOfflineNotice = true,
            )
            WebBrowseFixtureScenario.Error -> SearchPreviewData.failure.withBackendFreeArtwork()
            WebBrowseFixtureScenario.LongText -> SearchPreviewData.longLocalized.withBackendFreeArtwork()
        }
    }
}

private fun SearchUiState.withBackendFreeArtwork(): SearchUiState {
    return copy(
        trending = trending.map(ContentModel::withoutRemoteArtwork),
        content = when (val current = content) {
            is SearchContentState.Results -> current.copy(
                items = current.items.map { content ->
                    content.withoutRemoteArtwork().copy(
                        row = WebBrowseFixtureIds.SearchResultsSectionKey,
                    )
                },
            )
            else -> current
        },
    )
}

private fun ContentModel.withoutRemoteArtwork(): ContentModel {
    return copy(
        poster = "",
        backdrop = null,
    )
}
