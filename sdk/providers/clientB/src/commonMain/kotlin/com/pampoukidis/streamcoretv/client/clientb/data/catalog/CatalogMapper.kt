package com.pampoukidis.streamcoretv.client.clientb.data.catalog

import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBCategoryDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBContentDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBContributorDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBHomeLaneDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBLaneTemplateDto
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPurpose
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPresentationHint
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCastMember
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreGenre

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

internal fun ClientBContentDto.toModel(row: String? = null): StreamCoreContent {
    return StreamCoreContent(
        id = assetId,
        title = displayTitle,
        description = description,
        rating = ratingOutOfTen,
        pgRatingName = parentalRating,
        pgRatingLevel = parentalLevel,
        poster = portraitImage,
        backdrop = landscapeImage,
        cast = contributors.map { it.toModel() },
        releaseDate = releaseEpochMillis,
        genres = categories.map { it.toModel() },
        row = row,
    )
}

private fun ClientBContributorDto.toModel(): StreamCoreCastMember {
    return StreamCoreCastMember(
        id = id,
        name = displayName,
        characterName = roleName,
        image = imageUrl,
    )
}

private fun ClientBCategoryDto.toModel(): StreamCoreGenre {
    return StreamCoreGenre(
        id = code,
        name = displayName,
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
