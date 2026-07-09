package com.pampoukidis.streamcoretv.core.model.content

data class RowModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val content: List<ContentModel>,
    val type: RowType
)