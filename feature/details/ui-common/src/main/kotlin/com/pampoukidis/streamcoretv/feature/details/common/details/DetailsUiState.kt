package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcoretv.core.model.content.ContentModel

data class DetailsUiState(
    val isLoading: Boolean = true,
    val content: ContentModel? = null,
    val recommendations: List<ContentModel> = emptyList(),
)

/**
 * Returns a display-ready state seeded with navigation-provided content when it matches the
 * requested content id.
 *
 * This is intended for details routes that receive the selected item from the previous screen.
 * The initial content lets the UI render immediately while the backend-agnostic details load
 * is still running. Once loaded content exists, only the source row is preserved from the initial
 * content so shared-element keys remain stable across the transition.
 */
fun DetailsUiState.withInitialContent(
    contentId: String,
    initialContent: ContentModel?,
): DetailsUiState {
    if (initialContent?.id != contentId) {
        return this
    }

    val currentContent = content ?: return copy(content = initialContent)

    if (currentContent.row == initialContent.row) {
        return this
    }

    return copy(content = currentContent.copy(row = initialContent.row))
}