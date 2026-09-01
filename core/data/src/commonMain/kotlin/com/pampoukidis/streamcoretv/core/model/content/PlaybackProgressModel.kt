package com.pampoukidis.streamcoretv.core.model.content

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackProgressModel(
    val positionMillis: Long,
    val durationMillis: Long,
) {
    val fraction: Float
        get() {
            if (durationMillis <= 0L) {
                return 0f
            }

            return positionMillis
                .coerceIn(0L, durationMillis)
                .toFloat() / durationMillis.toFloat()
        }
}
