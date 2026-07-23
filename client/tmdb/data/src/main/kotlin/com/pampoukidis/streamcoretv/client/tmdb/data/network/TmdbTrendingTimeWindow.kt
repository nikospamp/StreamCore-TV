package com.pampoukidis.streamcoretv.client.tmdb.data.network

/**
 * Supported TMDB trending windows for `/3/trending/movie/{time_window}`.
 */
internal enum class TmdbTrendingTimeWindow(
    val pathValue: String,
) {
    Day(pathValue = "day"),
    Week(pathValue = "week"),
}
