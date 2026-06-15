package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowStyle
import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbCastDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbContentDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbGenreDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbHomeRowDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbRowPresentationDto

internal fun TmdbHomeRowDto.toModel(): RowModel {
    return RowModel(
        id = rowId,
        title = heading,
        subtitle = subheading,
        content = items.map { it.toModel() },
        style = presentation.toModel(),
    )
}

internal fun TmdbContentDto.toModel(): ContentModel {
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

private fun TmdbCastDto.toModel(): Cast {
    return Cast(
        id = personId,
        name = fullName,
        characterName = character,
        image = portraitUrl,
    )
}

private fun TmdbGenreDto.toModel(): Genre {
    return Genre(
        id = genreId,
        name = label,
    )
}

private fun TmdbRowPresentationDto.toModel(): RowStyle {
    return when (this) {
        TmdbRowPresentationDto.HeroCarousel -> RowStyle.Carousel
        TmdbRowPresentationDto.PortraitShelf -> RowStyle.Poster
        TmdbRowPresentationDto.LandscapeShelf -> RowStyle.Landscape
        TmdbRowPresentationDto.RankedShelf -> RowStyle.TopTen
    }
}
