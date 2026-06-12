package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcoretv.core.model.content.ContentModel

data class DetailsUiState(
    val isLoading: Boolean = true,
    val content: ContentModel? = null,
    val recommendations: List<ContentModel> = emptyList(),
)