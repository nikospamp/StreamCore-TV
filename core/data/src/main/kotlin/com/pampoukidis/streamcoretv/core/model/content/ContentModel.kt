package com.pampoukidis.streamcoretv.core.model.content

import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre

data class ContentModel(
    val id: String,
    val title: String,
    val description: String,
    val rating: Int,
    val pgRatingName: String,
    val pgRatingLevel: Int,
    val poster: String,
    val backdrop: String?,
    val cast: List<Cast>,
    val releaseDate: Long,
    val genres: List<Genre>,
)

fun ContentModel.fallbackText(): String {
    return title.firstOrNull()?.uppercase() ?: "?"
}

fun ContentModel.imageUrl(style: RowStyle): String? {
    return when (style) {
        RowStyle.Carousel,
        RowStyle.Landscape -> backdrop ?: poster

        RowStyle.Poster,
        RowStyle.TopTen -> poster
    }
}