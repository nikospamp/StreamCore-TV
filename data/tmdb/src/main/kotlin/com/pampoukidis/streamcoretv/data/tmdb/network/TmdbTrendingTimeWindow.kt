package com.pampoukidis.streamcoretv.data.tmdb.network

/**
 * Supported TMDB trending windows for `/3/trending/movie/{time_window}`.
 */
internal enum class TmdbTrendingTimeWindow(
    val pathValue: String,
) {
    Day(pathValue = "day"),
    Week(pathValue = "week"),
}
