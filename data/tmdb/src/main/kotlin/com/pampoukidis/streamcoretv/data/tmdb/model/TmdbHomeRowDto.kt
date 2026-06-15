package com.pampoukidis.streamcoretv.data.tmdb.model

internal data class TmdbHomeRowDto(
    val rowId: String,
    val heading: String,
    val subheading: String,
    val presentation: TmdbRowPresentationDto,
    val items: List<TmdbContentDto>,
)
