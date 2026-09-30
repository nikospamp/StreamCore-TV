package com.pampoukidis.streamcoretv.client.tmdb.data.catalog

import com.pampoukidis.streamcore.sdk.runtime.integration.provider.HomeProvider
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbMovieSummaryDto
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbReferenceData
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbReferenceDataSource
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbTrendingTimeWindow
import com.pampoukidis.streamcoretv.client.tmdb.data.profile.TmdbProfileRepository
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPurpose
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPresentationHint
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal class TmdbCatalogRepository constructor(
    private val tmdbApi: TmdbApi,
    private val referenceDataSource: TmdbReferenceDataSource,
    private val callExecutor: TmdbCallExecutor,
    private val profileRepository: TmdbProfileRepository,
) : HomeProvider {

    override suspend fun getCollections(profileId: String): StreamCoreResult<List<StreamCoreCollection>> {
        if (profileId.isBlank()) {
            return catalogFailure("PROFILE_ID_REQUIRED")
        }

        val policy = when (val result = profileRepository.getContentPolicy(profileId)) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> return result
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
                        purpose = StreamCoreCollectionPurpose.Featured,
                        content = trendingWeek.await().results,
                        includeAdult = policy.includeAdult,
                        referenceData = references,
                    ),
                    contentRow(
                        id = "tmdb-top-ten",
                        title = "Top 10 today",
                        subtitle = "Trending on TMDB",
                        purpose = StreamCoreCollectionPurpose.Ranked,
                        content = trendingDay.await().results.take(TOP_TEN_LIMIT),
                        includeAdult = policy.includeAdult,
                        referenceData = references,
                    ),
                    contentRow(
                        id = "tmdb-popular",
                        title = "Popular movies",
                        subtitle = "Most watched on TMDB",
                        presentationHint = StreamCoreCollectionPresentationHint.Poster,
                        content = popular.await().results,
                        includeAdult = policy.includeAdult,
                        referenceData = references,
                    ),
                    contentRow(
                        id = "tmdb-now-playing",
                        title = "Now playing",
                        subtitle = "Recently released movies",
                        presentationHint = StreamCoreCollectionPresentationHint.Landscape,
                        content = nowPlaying.await().results,
                        includeAdult = policy.includeAdult,
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
        purpose: StreamCoreCollectionPurpose = StreamCoreCollectionPurpose.Standard,
        presentationHint: StreamCoreCollectionPresentationHint? = null,
        content: List<TmdbMovieSummaryDto>,
        includeAdult: Boolean,
        referenceData: TmdbReferenceData,
    ): StreamCoreCollection {
        return StreamCoreCollection(
            id = id,
            title = title,
            subtitle = subtitle,
            purpose = purpose,
            presentationHint = presentationHint,
            content = content
                .filter { movie -> includeAdult || !movie.adult }
                .take(ROW_CONTENT_LIMIT)
                .toModels(
                    referenceData = referenceData,
                    row = id,
                ),
        )
    }

    private fun catalogFailure(backendCode: String): StreamCoreResult.Failure {
        return StreamCoreResult.Failure(
            StreamCoreError.Unknown(
                source = StreamCoreErrorSource(
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
        const val TOP_TEN_LIMIT = 10
        const val ROW_CONTENT_LIMIT = 20
    }
}
