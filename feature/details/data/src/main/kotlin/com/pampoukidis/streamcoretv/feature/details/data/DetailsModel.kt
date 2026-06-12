package com.pampoukidis.streamcoretv.feature.details.data

import com.pampoukidis.streamcoretv.core.model.content.ContentModel

data class DetailsModel(
    val content: ContentModel,
    val recommendations: List<ContentModel>,
)