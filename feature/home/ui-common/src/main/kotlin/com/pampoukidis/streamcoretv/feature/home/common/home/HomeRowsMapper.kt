package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType

fun List<RowModel>.toHomeContentModel(): HomeContentModel {
    val populatedRows = filter { row -> row.content.isNotEmpty() }

    val featuredRow = populatedRows.firstOrNull { row ->
        row.type == RowType.Featured
    } ?: populatedRows.firstOrNull()

    val continueWatching = populatedRows
        .firstOrNull { row -> row.type == RowType.ContinueWatching }
        ?.let { row ->
            row.copy(
                content = row.content.filter { content ->
                    content.playbackProgress != null
                },
            )
        }
        ?.takeIf { row -> row.content.isNotEmpty() }

    return HomeContentModel(
        featured = featuredRow?.content.orEmpty(),
        continueWatching = continueWatching,
        shelves = populatedRows.filterNot { row ->
            row.id == featuredRow?.id || row.type == RowType.ContinueWatching
        },
    )
}