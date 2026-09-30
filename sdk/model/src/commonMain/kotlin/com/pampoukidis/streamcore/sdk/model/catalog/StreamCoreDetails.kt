package com.pampoukidis.streamcore.sdk.model.catalog

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent

/** Details and related catalogue content, independent of a particular screen layout. */
data class StreamCoreDetails(
    val content: StreamCoreContent,
    val recommendations: List<StreamCoreContent>,
)
