package com.pampoukidis.streamcoretv.feature.library.web.library

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import com.pampoukidis.streamcoretv.feature.library.common.library.LibraryUiState

internal const val WebLibraryViewModelKeyPrefix = "web-library:"
internal const val WebLibraryContinueWatchingSection = "library:continue-watching"
internal const val WebLibraryLikedSection = "library:liked"
internal const val WebLibraryMyListSection = "library:my-list"

internal fun libraryViewModelKey(profileId: String): String {
    require(profileId.isNotBlank())
    return WebLibraryViewModelKeyPrefix + profileId
}

internal fun ContentModel.webLibraryFocusKey(): WebBrowseFocusKey? {
    val sectionKey = when (row) {
        WebLibraryContinueWatchingSection -> WebLibraryContinueWatchingSection
        WebLibraryLikedSection -> WebLibraryLikedSection
        WebLibraryMyListSection -> WebLibraryMyListSection
        else -> null
    } ?: return null

    if (id.isBlank()) {
        return null
    }

    return WebBrowseFocusKey(
        destination = WebBrowseDestination.Library,
        sectionKey = sectionKey,
        itemKey = id,
    )
}

internal fun LibraryUiState.findWebLibraryFocusTarget(
    focusKey: WebBrowseFocusKey?,
): WebLibraryFocusTarget? {
    if (focusKey == null || focusKey.destination != WebBrowseDestination.Library) {
        return null
    }

    val content = contentForWebLibrarySection(focusKey.sectionKey) ?: return null
    val itemIndex = content.indexOfFirst { item -> item.id == focusKey.itemKey }
    if (itemIndex < 0) {
        return null
    }

    return WebLibraryFocusTarget(
        sectionKey = focusKey.sectionKey,
        itemIndex = itemIndex,
    )
}

internal fun LibraryUiState.firstWebLibraryFocusTarget(): WebLibraryFocusTarget? {
    return when {
        continueWatching.isNotEmpty() -> WebLibraryFocusTarget(
            sectionKey = WebLibraryContinueWatchingSection,
            itemIndex = 0,
        )

        likedContent.isNotEmpty() -> WebLibraryFocusTarget(
            sectionKey = WebLibraryLikedSection,
            itemIndex = 0,
        )

        myListContent.isNotEmpty() -> WebLibraryFocusTarget(
            sectionKey = WebLibraryMyListSection,
            itemIndex = 0,
        )

        else -> null
    }
}

private fun LibraryUiState.contentForWebLibrarySection(
    sectionKey: String,
): List<ContentModel>? {
    return when (sectionKey) {
        WebLibraryContinueWatchingSection -> continueWatching
        WebLibraryLikedSection -> likedContent
        WebLibraryMyListSection -> myListContent
        else -> null
    }
}
