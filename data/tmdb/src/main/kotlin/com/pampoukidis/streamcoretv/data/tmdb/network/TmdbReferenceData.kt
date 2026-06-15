package com.pampoukidis.streamcoretv.data.tmdb.network

import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbApiGenreDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbImagesConfigurationDto

/**
 * Read-only TMDB reference data needed when mapping API DTOs to app models.
 *
 * TMDB list/detail responses contain image paths and genre ids. This type turns
 * image paths into full URLs and resolves genre ids to the loaded genre DTOs.
 */
internal class TmdbReferenceData(
    private val images: TmdbImagesConfigurationDto,
    private val genresById: Map<Int, TmdbApiGenreDto>,
) {

    /**
     * Builds a poster image URL using the closest available poster size preference.
     */
    fun posterUrl(path: String?): String? {
        return imageUrl(
            path = path,
            availableSizes = images.posterSizes,
            preferredSizes = POSTER_SIZE_PREFERENCES,
        )
    }

    /**
     * Builds a backdrop image URL using the closest available backdrop size preference.
     */
    fun backdropUrl(path: String?): String? {
        return imageUrl(
            path = path,
            availableSizes = images.backdropSizes,
            preferredSizes = BACKDROP_SIZE_PREFERENCES,
        )
    }

    /**
     * Builds a cast/profile image URL using the closest available profile size preference.
     */
    fun profileUrl(path: String?): String? {
        return imageUrl(
            path = path,
            availableSizes = images.profileSizes,
            preferredSizes = PROFILE_SIZE_PREFERENCES,
        )
    }

    /**
     * Resolves a TMDB genre id to the corresponding loaded genre.
     */
    fun genre(genreId: Int): TmdbApiGenreDto? {
        return genresById[genreId]
    }

    private fun imageUrl(
        path: String?,
        availableSizes: List<String>,
        preferredSizes: List<String>,
    ): String? {
        if (path.isNullOrBlank()) {
            return null
        }

        val size = selectImageSize(
            availableSizes = availableSizes,
            preferredSizes = preferredSizes,
        )
        val normalizedBaseUrl = images.secureBaseUrl.ensureTrailingSlash()
        val normalizedPath = path.removePrefix("/")
        return "$normalizedBaseUrl$size/$normalizedPath"
    }

    private fun selectImageSize(
        availableSizes: List<String>,
        preferredSizes: List<String>,
    ): String {
        preferredSizes.forEach { preferredSize ->
            if (preferredSize in availableSizes) {
                return preferredSize
            }
        }

        return availableSizes.firstOrNull { it != ORIGINAL_SIZE }
            ?: availableSizes.firstOrNull()
            ?: ORIGINAL_SIZE
    }

    private fun String.ensureTrailingSlash(): String {
        if (endsWith("/")) {
            return this
        }

        return "$this/"
    }

    private companion object {
        const val ORIGINAL_SIZE = "original"
        val POSTER_SIZE_PREFERENCES = listOf("w500", "w342", "w780", ORIGINAL_SIZE)
        val BACKDROP_SIZE_PREFERENCES = listOf("w1280", "w780", ORIGINAL_SIZE)
        val PROFILE_SIZE_PREFERENCES = listOf("w185", "w342", "h632", ORIGINAL_SIZE)
    }
}
