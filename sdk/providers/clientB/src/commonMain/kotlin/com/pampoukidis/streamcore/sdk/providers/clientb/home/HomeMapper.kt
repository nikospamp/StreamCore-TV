package com.pampoukidis.streamcore.sdk.providers.clientb.home

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPresentationHint
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPurpose
import com.pampoukidis.streamcore.sdk.providers.clientb.catalog.toModel

internal fun ClientBHomeLaneDto.toModel(): StreamCoreCollection {
    return StreamCoreCollection(
        id = laneId,
        title = title,
        subtitle = caption,
        content = assets.map { asset ->
            asset.toModel(row = laneId)
        },
        purpose = template.toCollectionPurpose(),
        presentationHint = template.toPresentationHint(),
    )
}

private fun ClientBLaneTemplateDto.toCollectionPurpose(): StreamCoreCollectionPurpose {
    return when (this) {
        ClientBLaneTemplateDto.Spotlight -> StreamCoreCollectionPurpose.Featured
        ClientBLaneTemplateDto.Ranking -> StreamCoreCollectionPurpose.Ranked
        ClientBLaneTemplateDto.Portrait, ClientBLaneTemplateDto.Landscape -> StreamCoreCollectionPurpose.Standard
    }
}

private fun ClientBLaneTemplateDto.toPresentationHint(): StreamCoreCollectionPresentationHint? {
    return when (this) {
        ClientBLaneTemplateDto.Portrait -> StreamCoreCollectionPresentationHint.Poster
        ClientBLaneTemplateDto.Landscape -> StreamCoreCollectionPresentationHint.Landscape
        ClientBLaneTemplateDto.Spotlight, ClientBLaneTemplateDto.Ranking -> null
    }
}
