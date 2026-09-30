package com.pampoukidis.streamcore.sdk.model.catalog

/** Headless identity input for a profile-scoped details operation. */
data class StreamCoreDetailsRequest(
    val profileId: String,
    val contentId: String,
)
