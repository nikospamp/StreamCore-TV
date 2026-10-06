package com.pampoukidis.streamcore.sdk.providers.tmdb.details

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreTrailer

/** The first entry is the preferred trailer: official first, then newest publication. */
internal fun TmdbVideosResponseDto.toTrailers(): List<StreamCoreTrailer> {
    return results
        .filter { video -> video.type.equals("Trailer", ignoreCase = true) }
        .sortedWith(
            compareByDescending<TmdbVideoDto> { it.official }
                .thenByDescending { it.publishedAt.orEmpty() },
        )
        .mapNotNull { video -> video.toTrailerOrNull() }
        .distinctBy { trailer -> trailer.url }
}

private fun TmdbVideoDto.toTrailerOrNull(): StreamCoreTrailer? {
    val videoKey = key.trim()
    val url = when {
        site.equals("YouTube", ignoreCase = true) && YouTubeKey.matches(videoKey) -> {
            "https://www.youtube.com/watch?v=$videoKey"
        }
        site.equals("Vimeo", ignoreCase = true) && VimeoKey.matches(videoKey) -> {
            "https://vimeo.com/$videoKey"
        }
        else -> return null
    }
    return StreamCoreTrailer(
        id = id.ifBlank { url },
        title = name.trim().ifBlank { "Trailer" },
        url = url,
    )
}

private val YouTubeKey = Regex("[A-Za-z0-9_-]{11}")
private val VimeoKey = Regex("[0-9]+")
