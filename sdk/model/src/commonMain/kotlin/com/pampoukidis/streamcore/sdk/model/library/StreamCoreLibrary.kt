package com.pampoukidis.streamcore.sdk.model.library

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent

data class StreamCoreLibrary(
    val continueWatching: List<StreamCoreContent> = emptyList(),
    val likedContent: List<StreamCoreContent> = emptyList(),
    val myListContent: List<StreamCoreContent> = emptyList(),
)
