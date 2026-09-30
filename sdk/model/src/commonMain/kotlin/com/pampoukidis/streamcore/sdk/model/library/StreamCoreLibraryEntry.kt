package com.pampoukidis.streamcore.sdk.model.library

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent

data class StreamCoreLibraryEntry(
    val content: StreamCoreContent,
    val likedAtMillis: Long? = null,
    val addedToMyListAtMillis: Long? = null,
)
