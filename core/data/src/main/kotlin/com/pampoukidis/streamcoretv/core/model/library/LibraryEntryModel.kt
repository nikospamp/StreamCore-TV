package com.pampoukidis.streamcoretv.core.model.library

import com.pampoukidis.streamcoretv.core.model.content.ContentModel

data class LibraryEntryModel(
    val content: ContentModel,
    val likedAtMillis: Long? = null,
    val addedToMyListAtMillis: Long? = null,
)
