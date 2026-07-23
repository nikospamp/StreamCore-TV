package com.pampoukidis.streamcoretv.client.tmdb.data.catalog

import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbMovieSummaryDto
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbReferenceData
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbReferenceDataSource
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbTrendingTimeWindow
import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbCatalogRepository @Inject internal constructor(
    private val tmdbApi: TmdbApi,
    private val referenceDataSource: TmdbReferenceDataSource,
    private val callExecutor: TmdbCallExecutor,
) : HomeRepository {

    override suspend fun getHomeRows(profileId: String): AppResult<List<RowModel>> {
        if (profileId.isBlank()) {
            return catalogFailure("PROFILE_ID_REQUIRED")
        }

        return callExecutor.execute(operation = GET_HOME_ROWS_OPERATION) {
            coroutineScope {
                val referenceData = async { referenceDataSource.getReferenceData() }
                val trendingWeek = async {
                    tmdbApi.getTrendingMovies(timeWindow = TmdbTrendingTimeWindow.Week)
                }
                val trendingDay = async {
                    tmdbApi.getTrendingMovies(timeWindow = TmdbTrendingTimeWindow.Day)
                }
                val popular = async {
                    tmdbApi.getPopularMovies(region = DEFAULT_REGION)
                }
                val nowPlaying = async {
                    tmdbApi.getNowPlayingMovies(region = DEFAULT_REGION)
                }

                val references = referenceData.await()
                listOf(
                    contentRow(
                        id = "tmdb-trending-week",
                        title = "Trending this week",
                        subtitle = "Movies people are watching now",
                        type = RowType.Featured,
                        content = trendingWeek.await().results,
                        profileId = profileId,
                        referenceData = references,
                    ),
                    contentRow(
                        id = "tmdb-top-ten",
                        title = "Top 10 today",
                        subtitle = "Trending on TMDB",
                        type = RowType.TopTen,
                        content = trendingDay.await().results.take(TOP_TEN_LIMIT),
                        profileId = profileId,
                        referenceData = references,
                    ),
                    contentRow(
                        id = "tmdb-popular",
                        title = "Popular movies",
                        subtitle = "Most watched on TMDB",
                        type = RowType.Poster,
                        content = popular.await().results,
                        profileId = profileId,
                        referenceData = references,
                    ),
                    contentRow(
                        id = "tmdb-now-playing",
                        title = "Now playing",
                        subtitle = "Recently released movies",
                        type = RowType.Landscape,
                        content = nowPlaying.await().results,
                        profileId = profileId,
                        referenceData = references,
                    ),
                ).filter { row -> row.content.isNotEmpty() }
            }
        }
    }

    private fun contentRow(
        id: String,
        title: String,
        subtitle: String,
        type: RowType,
        content: List<TmdbMovieSummaryDto>,
        profileId: String,
        referenceData: TmdbReferenceData,
    ): RowModel {
        return RowModel(
            id = id,
            title = title,
            subtitle = subtitle,
            type = type,
            content = content
                .contentVisibleForProfile(profileId = profileId)
                .take(ROW_CONTENT_LIMIT)
                .toModels(
                    referenceData = referenceData,
                    row = id,
                ),
        )
    }

    private fun List<TmdbMovieSummaryDto>.contentVisibleForProfile(
        profileId: String,
    ): List<TmdbMovieSummaryDto> {
        if (profileId.contains(KIDS_PROFILE_MARKER, ignoreCase = true)) {
            return filterNot { movie -> movie.adult }
        }

        return this
    }

    private fun catalogFailure(backendCode: String): AppResult.Failure {
        return AppResult.Failure(
            AppError.Unknown(
                source = ErrorSource(
                    client = CLIENT,
                    operation = GET_HOME_ROWS_OPERATION,
                    backendCode = backendCode,
                ),
            ),
        )
    }

    private companion object {
        const val CLIENT = "tmdb"
        const val GET_HOME_ROWS_OPERATION = "getHomeRows"
        const val DEFAULT_REGION = "US"
        const val KIDS_PROFILE_MARKER = "kids"
        const val TOP_TEN_LIMIT = 10
        const val ROW_CONTENT_LIMIT = 20
    }
}