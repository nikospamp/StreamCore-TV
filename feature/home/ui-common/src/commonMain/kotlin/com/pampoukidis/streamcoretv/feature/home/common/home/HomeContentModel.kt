package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcoretv.core.model.content.RowModel

data class HomeContentModel(
    val featured: List<StreamCoreContent>,
    val continueWatching: RowModel?,
    val shelves: List<RowModel>,
)
