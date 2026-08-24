package com.pampoukidis.streamcoretv.core.model.library

import com.pampoukidis.streamcoretv.core.model.content.ContentModel

data class LibraryModel(
    val continueWatching: List<ContentModel> = emptyList(),
    val likedContent: List<ContentModel> = emptyList(),
    val myListContent: List<ContentModel> = emptyList(),
)
