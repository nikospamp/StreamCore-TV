package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel

data class HomeContentModel(
    val featured: List<ContentModel>,
    val continueWatching: RowModel?,
    val shelves: List<RowModel>,
)