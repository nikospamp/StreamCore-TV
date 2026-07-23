package com.pampoukidis.streamcoretv.client.tmdb.data.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads and caches TMDB reference data for the process lifetime.
 *
 * The first caller fetches configuration and movie genres. Concurrent callers wait
 * on [mutex], then reuse the same cached value once loading completes.
 */
@Singleton
internal class TmdbReferenceDataSource @Inject constructor(
    private val tmdbApi: TmdbApi,
) {

    private val mutex = Mutex()
    private var cachedReferenceData: TmdbReferenceData? = null

    /**
     * Returns cached reference data, loading it once if needed.
     */
    suspend fun getReferenceData(): TmdbReferenceData {
        cachedReferenceData?.let { referenceData ->
            return referenceData
        }

        return mutex.withLock {
            cachedReferenceData?.let { referenceData ->
                return@withLock referenceData
            }

            val configuration = tmdbApi.getConfiguration()
            val genres = tmdbApi.getMovieGenres()
            val referenceData = TmdbReferenceData(
                images = configuration.images,
                genresById = genres.genres.associateBy { genre -> genre.id },
            )
            cachedReferenceData = referenceData
            return@withLock referenceData
        }
    }
}
