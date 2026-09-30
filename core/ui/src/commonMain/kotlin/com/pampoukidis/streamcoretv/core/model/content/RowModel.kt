package com.pampoukidis.streamcoretv.core.model.content

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent

/** Application rendering projection of a semantic SDK collection. */
data class RowModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val content: List<StreamCoreContent>,
    val type: RowType
)
