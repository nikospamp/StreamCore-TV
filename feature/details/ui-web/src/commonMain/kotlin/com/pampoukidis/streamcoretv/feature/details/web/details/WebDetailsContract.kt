package com.pampoukidis.streamcoretv.feature.details.web.details

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsAction
import com.pampoukidis.streamcoretv.feature.details.common.details.DetailsUiState
import com.pampoukidis.streamcoretv.feature.details.common.details.withInitialContent
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest

internal const val WebDetailsActionsSection = "details:actions"
internal const val WebDetailsRecommendationsSection = "details:recommendations"
internal const val WebDetailsBackItem = "back"
internal const val WebDetailsRefreshItem = "refresh"
internal const val WebDetailsPlayItem = "play"
internal const val WebDetailsTrailerItem = "trailer"
internal const val WebDetailsLikeItem = "like"
internal const val WebDetailsMyListItem = "my-list"

internal fun webDetailsLoadAction(
    profileId: String,
    contentId: String,
    initialContent: ContentModel?,
): DetailsAction.Load {
    return DetailsAction.Load(
        request = DetailsRequest(
            profileId = profileId,
            contentId = contentId,
        ),
        initialContent = initialContent,
    )
}

internal fun DetailsUiState.webDetailsDisplayState(
    contentId: String,
    initialContent: ContentModel?,
): DetailsUiState {
    val routeState = if (content == null || content.id == contentId) {
        this
    } else {
        DetailsUiState(isLoading = true)
    }
    return routeState.withInitialContent(
        contentId = contentId,
        initialContent = initialContent,
    )
}

internal fun webDetailsActionFocusKey(itemKey: String): WebBrowseFocusKey {
    require(itemKey.isNotBlank())
    return WebBrowseFocusKey(
        destination = WebBrowseDestination.Details,
        sectionKey = WebDetailsActionsSection,
        itemKey = itemKey,
    )
}

internal fun ContentModel.webDetailsRecommendationFocusKey(): WebBrowseFocusKey? {
    if (id.isBlank()) {
        return null
    }
    return WebBrowseFocusKey(
        destination = WebBrowseDestination.Details,
        sectionKey = WebDetailsRecommendationsSection,
        itemKey = id,
    )
}

internal fun DetailsUiState.findWebDetailsFocusTarget(
    focusKey: WebBrowseFocusKey?,
): WebDetailsFocusTarget? {
    if (focusKey == null || focusKey.destination != WebBrowseDestination.Details) {
        return null
    }

    return when (focusKey.sectionKey) {
        WebDetailsActionsSection -> findWebDetailsActionFocusTarget(focusKey.itemKey)
        WebDetailsRecommendationsSection -> {
            val itemIndex = recommendations.indexOfFirst { content ->
                content.id == focusKey.itemKey
            }
            if (itemIndex < 0) {
                null
            } else {
                WebDetailsFocusTarget(
                    sectionKey = WebDetailsRecommendationsSection,
                    itemKey = focusKey.itemKey,
                    itemIndex = itemIndex,
                )
            }
        }

        else -> null
    }
}

internal fun DetailsUiState.initialWebDetailsFocusTarget(): WebDetailsFocusTarget {
    return WebDetailsFocusTarget(
        sectionKey = WebDetailsActionsSection,
        itemKey = if (content == null) WebDetailsBackItem else WebDetailsPlayItem,
    )
}

private fun DetailsUiState.findWebDetailsActionFocusTarget(
    itemKey: String,
): WebDetailsFocusTarget? {
    val exists = when (itemKey) {
        WebDetailsBackItem -> true
        WebDetailsRefreshItem -> !isLoading
        WebDetailsPlayItem -> content != null
        WebDetailsTrailerItem -> content?.trailers?.isNotEmpty() == true
        WebDetailsLikeItem -> content != null && isLibraryAvailable && !isLikeMutationPending
        WebDetailsMyListItem -> content != null && isLibraryAvailable && !isMyListMutationPending
        else -> false
    }
    if (!exists) {
        return null
    }

    return WebDetailsFocusTarget(
        sectionKey = WebDetailsActionsSection,
        itemKey = itemKey,
    )
}
