package com.pampoukidis.streamcore.sdk.providers.clientb.catalog

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCastMember
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreGenre

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
