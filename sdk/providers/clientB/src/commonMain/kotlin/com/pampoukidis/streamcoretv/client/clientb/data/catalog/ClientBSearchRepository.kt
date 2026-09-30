package com.pampoukidis.streamcoretv.client.clientb.data.catalog

import com.pampoukidis.streamcore.sdk.runtime.integration.provider.SearchProvider
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBContentDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ClientBLaneTemplateDto
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProfileProvider
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import com.pampoukidis.streamcore.sdk.api.validation.SearchQueryNormalizer

internal class ClientBSearchRepository constructor(
    private val catalogSource: ClientBCatalogSource,
    private val profileRepository: ProfileProvider,
) : SearchProvider {

    override suspend fun search(
        profileId: String,
        query: String,
    ): StreamCoreResult<List<StreamCoreContent>> {
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
            val comparableQuery = normalizedQuery.lowercase()
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

            StreamCoreResult.Success(results)
        }
    }

    override suspend fun loadTrending(profileId: String): StreamCoreResult<List<StreamCoreContent>> {
        return withProfile(
            profileId = profileId,
            operation = LOAD_TRENDING_OPERATION,
        ) { profile ->
            val rankingLane = catalogSource
                .homeLanes(catalogSource.contentForProfile(profile.isKidsProfile))
                .first { lane -> lane.template == ClientBLaneTemplateDto.Ranking }

            StreamCoreResult.Success(
                rankingLane.assets
                    .take(MAX_TRENDING_RESULTS)
                    .map { content -> content.toModel() },
            )
        }
    }

    private suspend fun <T> withProfile(
        profileId: String,
        operation: String,
        block: (StreamCoreProfile) -> StreamCoreResult<T>,
    ): StreamCoreResult<T> {
        if (profileId.isBlank()) {
            return searchFailure(
                operation = operation,
                backendCode = "PROFILE_ID_REQUIRED",
            )
        }

        return when (val profilesResult = profileRepository.getProfiles()) {
            is StreamCoreResult.Failure -> profilesResult
            is StreamCoreResult.Success -> {
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
        val title = displayTitle.lowercase()
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
    ): StreamCoreResult.Failure {
        return StreamCoreResult.Failure(
            StreamCoreError.Unknown(
                source = StreamCoreErrorSource(
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
