package com.pampoukidis.streamcoretv.data.client.catalog

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowStyle
import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre
import com.pampoukidis.streamcoretv.data.client.model.ClientACastDto
import com.pampoukidis.streamcoretv.data.client.model.ClientAContentDto
import com.pampoukidis.streamcoretv.data.client.model.ClientAGenreDto
import com.pampoukidis.streamcoretv.data.client.model.ClientAHomeRowDto
import com.pampoukidis.streamcoretv.data.client.model.ClientARowPresentationDto

internal fun ClientAHomeRowDto.toModel(): RowModel {
    return RowModel(
        id = rowId,
        title = heading,
        subtitle = subheading,
        content = items.map { it.toModel() },
        style = presentation.toModel(),
    )
}

internal fun ClientAContentDto.toModel(): ContentModel {
    return ContentModel(
        id = contentId,
        title = name,
        description = synopsis,
        rating = score,
        pgRatingName = maturityLabel,
        pgRatingLevel = maturityRank,
        poster = posterUrl,
        backdrop = heroImageUrl,
        cast = cast.map { it.toModel() },
        releaseDate = publishedAtEpochMs,
        genres = genres.map { it.toModel() },
    )
}

private fun ClientACastDto.toModel(): Cast {
    return Cast(
        id = personId,
        name = fullName,
        characterName = character,
        image = portraitUrl,
    )
}

private fun ClientAGenreDto.toModel(): Genre {
    return Genre(
        id = genreId,
        name = label,
    )
}

private fun ClientARowPresentationDto.toModel(): RowStyle {
    return when (this) {
        ClientARowPresentationDto.HeroCarousel -> RowStyle.Carousel
        ClientARowPresentationDto.PortraitShelf -> RowStyle.Poster
        ClientARowPresentationDto.LandscapeShelf -> RowStyle.Landscape
        ClientARowPresentationDto.RankedShelf -> RowStyle.TopTen
    }
}
