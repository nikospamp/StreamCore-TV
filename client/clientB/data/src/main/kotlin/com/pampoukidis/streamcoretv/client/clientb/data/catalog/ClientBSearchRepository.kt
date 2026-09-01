package com.pampoukidis.streamcoretv.client.clientb.data.catalog

import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBContentDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBLaneTemplateDto
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.feature.search.domain.SearchQueryNormalizer
import com.pampoukidis.streamcoretv.feature.search.domain.SearchRepository
import java.util.Locale

class ClientBSearchRepository constructor(
    private val catalogSource: ClientBCatalogSource,
    private val profileRepository: ProfileRepository,
) : SearchRepository {

    override suspend fun search(
        profileId: String,
        query: String,
    ): AppResult<List<ContentModel>> {
        val normalizedQuery = SearchQueryNormalizer.normalize(query)
        if (!SearchQueryNormalizer.isSearchable(normalizedQuery)) {
            return searchFailure(
                operation = SEARCH_OPERATION,
                backendCode = "QUERY_TOO_SHORT",
            )
        }

        return withProfile(
            profileId = profileId,
            operation = SEARCH_OPERATION,
        ) { profile ->
            val comparableQuery = normalizedQuery.lowercase(Locale.ROOT)
            val results = catalogSource.contentForProfile(profile.isKidsProfile)
                .mapIndexedNotNull { index, content ->
                    content.matchRank(comparableQuery)?.let { rank ->
                        RankedContent(
                            content = content,
                            rank = rank,
                            catalogIndex = index,
                        )
                    }
                }
                .sortedWith(
                    compareBy<RankedContent> { it.rank }
                        .thenBy { it.catalogIndex },
                )
                .take(MAX_SEARCH_RESULTS)
                .map { rankedContent ->
                    rankedContent.content.toModel()
                }

            AppResult.Success(results)
        }
    }

    override suspend fun loadTrending(profileId: String): AppResult<List<ContentModel>> {
        return withProfile(
            profileId = profileId,
            operation = LOAD_TRENDING_OPERATION,
        ) { profile ->
            val rankingLane = catalogSource
                .homeLanes(catalogSource.contentForProfile(profile.isKidsProfile))
                .first { lane -> lane.template == ClientBLaneTemplateDto.Ranking }

            AppResult.Success(
                rankingLane.assets
                    .take(MAX_TRENDING_RESULTS)
                    .map { content -> content.toModel() },
            )
        }
    }

    private suspend fun <T> withProfile(
        profileId: String,
        operation: String,
        block: (ProfileModel) -> AppResult<T>,
    ): AppResult<T> {
        if (profileId.isBlank()) {
            return searchFailure(
                operation = operation,
                backendCode = "PROFILE_ID_REQUIRED",
            )
        }

        return when (val profilesResult = profileRepository.getProfiles()) {
            is AppResult.Failure -> profilesResult
            is AppResult.Success -> {
                val profile = profilesResult.value.firstOrNull { profile ->
                    profile.id == profileId
                } ?: return searchFailure(
                    operation = operation,
                    backendCode = "PROFILE_NOT_FOUND",
                )
                block(profile)
            }
        }
    }

    private fun ClientBContentDto.matchRank(query: String): Int? {
        val title = displayTitle.lowercase(Locale.ROOT)
        return when {
            title == query -> MatchRank.ExactTitle.value
            title.startsWith(query) -> MatchRank.TitlePrefix.value
            title.contains(query) -> MatchRank.TitleContains.value
            contributors.any { contributor ->
                contributor.displayName.contains(query, ignoreCase = true)
            } || categories.any { category ->
                category.displayName.contains(query, ignoreCase = true)
            } -> MatchRank.Metadata.value

            description.contains(query, ignoreCase = true) -> MatchRank.Description.value
            else -> null
        }
    }

    private fun searchFailure(
        operation: String,
        backendCode: String,
    ): AppResult.Failure {
        return AppResult.Failure(
            AppError.Unknown(
                source = ErrorSource(
                    client = CLIENT,
                    operation = operation,
                    backendCode = backendCode,
                ),
            ),
        )
    }

    private data class RankedContent(
        val content: ClientBContentDto,
        val rank: Int,
        val catalogIndex: Int,
    )

    private enum class MatchRank(val value: Int) {
        ExactTitle(0),
        TitlePrefix(1),
        TitleContains(2),
        Metadata(3),
        Description(4),
    }

    private companion object {
        const val CLIENT = "clientB"
        const val SEARCH_OPERATION = "search"
        const val LOAD_TRENDING_OPERATION = "loadTrending"
        const val MAX_SEARCH_RESULTS = 20
        const val MAX_TRENDING_RESULTS = 6
    }
}
