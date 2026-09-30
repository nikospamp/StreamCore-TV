package com.pampoukidis.streamcore.sdk.model.catalog

/** Ordered membership and meaning; consumers choose their own rendering. */
data class StreamCoreCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val content: List<StreamCoreContent>,
    val purpose: StreamCoreCollectionPurpose = StreamCoreCollectionPurpose.Standard,
    val presentationHint: StreamCoreCollectionPresentationHint? = null,
)
