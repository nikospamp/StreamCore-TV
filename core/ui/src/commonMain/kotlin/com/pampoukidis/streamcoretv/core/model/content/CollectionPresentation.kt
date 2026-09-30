package com.pampoukidis.streamcoretv.core.model.content

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPresentationHint
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPurpose

fun StreamCoreCollection.toRowModel(): RowModel {
    val rowType = when (purpose) {
        StreamCoreCollectionPurpose.Featured -> RowType.Featured
        StreamCoreCollectionPurpose.ContinueWatching -> RowType.ContinueWatching
        StreamCoreCollectionPurpose.Ranked -> RowType.TopTen
        StreamCoreCollectionPurpose.Standard -> when (presentationHint) {
            StreamCoreCollectionPresentationHint.Landscape -> RowType.Landscape
            StreamCoreCollectionPresentationHint.Poster, null -> RowType.Poster
        }
    }
    return RowModel(id = id, title = title, subtitle = subtitle, content = content, type = rowType)
}
