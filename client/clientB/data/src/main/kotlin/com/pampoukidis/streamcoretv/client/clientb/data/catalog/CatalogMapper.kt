package com.pampoukidis.streamcoretv.client.clientb.data.catalog

import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBCategoryDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBContentDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBContributorDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBHomeLaneDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBLaneTemplateDto
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre

internal fun ClientBHomeLaneDto.toModel(): RowModel {
    return RowModel(
        id = laneId,
        title = title,
        subtitle = caption,
        content = assets.map { asset ->
            asset.toModel(row = laneId)
        },
        type = template.toRowType(),
    )
}

internal fun ClientBContentDto.toModel(row: String? = null): ContentModel {
    return ContentModel(
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

private fun ClientBContributorDto.toModel(): Cast {
    return Cast(
        id = id,
        name = displayName,
        characterName = roleName,
        image = imageUrl,
    )
}

private fun ClientBCategoryDto.toModel(): Genre {
    return Genre(
        id = code,
        name = displayName,
    )
}

private fun ClientBLaneTemplateDto.toRowType(): RowType {
    return when (this) {
        ClientBLaneTemplateDto.Spotlight -> RowType.Featured
        ClientBLaneTemplateDto.Portrait -> RowType.Poster
        ClientBLaneTemplateDto.Landscape -> RowType.Landscape
        ClientBLaneTemplateDto.Ranking -> RowType.TopTen
    }
}
