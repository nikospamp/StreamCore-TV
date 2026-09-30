package com.pampoukidis.streamcore.sdk.model.catalog

import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgress
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCastMember
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreGenre
import kotlinx.serialization.Serializable

@Serializable
data class StreamCoreContent(
    val id: String,
    val title: String,
    val description: String,
    /** Whole-number audience score out of ten; the reference providers use zero when unavailable. */
    val rating: Int,
    val pgRatingName: String,
    /** Provider-specific classification. Consumers must not derive a universal maturity policy from it. */
    val pgRatingLevel: Int,
    val poster: String,
    val backdrop: String?,
    val cast: List<StreamCoreCastMember>,
    /** Unix epoch milliseconds; zero means the provider did not supply a usable date. */
    val releaseDate: Long,
    val genres: List<StreamCoreGenre>,
    val row: String? = null,
    val playbackProgress: StreamCorePlaybackProgress? = null,
    /** Supported external trailers, ordered by provider preference. */
    val trailers: List<StreamCoreTrailer> = emptyList(),
)
