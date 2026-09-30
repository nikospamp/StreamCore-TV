package com.pampoukidis.streamcore.sdk.testing

import com.pampoukidis.streamcore.sdk.api.SearchService
import com.pampoukidis.streamcore.sdk.api.validation.SearchQueryNormalizer
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.search.StreamCoreSearchInteraction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Consumer history fake with unsupported catalogue queries; never a production dependency. */
class InMemorySearchService : SearchService {
    private val queries = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    override suspend fun search(
        profileId: String,
        query: String,
        interaction: StreamCoreSearchInteraction,
    ): StreamCoreResult<List<StreamCoreContent>> {
        return StreamCoreResult.Failure(StreamCoreError.Unsupported("search"))
    }

    override suspend fun loadTrending(profileId: String): StreamCoreResult<List<StreamCoreContent>> {
        return StreamCoreResult.Failure(StreamCoreError.Unsupported("search.trending"))
    }

    override suspend fun displayedResults(
        profileId: String,
        query: String,
        results: List<StreamCoreContent>,
        interaction: StreamCoreSearchInteraction,
    ): StreamCoreResult<Unit> {
        if (interaction == StreamCoreSearchInteraction.Typing || results.isEmpty()) return StreamCoreResult.Success(Unit)
        return recordHistory(profileId, query)
    }

    override suspend fun resultSelected(profileId: String, query: String): StreamCoreResult<Unit> {
        return recordHistory(profileId, query)
    }

    override fun observeHistory(profileId: String): Flow<StreamCoreResult<List<String>>> {
        return queries.map { StreamCoreResult.Success(it[profileId].orEmpty()) }
    }

    override suspend fun recordHistory(profileId: String, query: String): StreamCoreResult<Unit> {
        val normalized = SearchQueryNormalizer.normalize(query)
        if (!SearchQueryNormalizer.isSearchable(normalized)) return StreamCoreResult.Success(Unit)
        queries.update { current ->
            val retained = current[profileId].orEmpty().filterNot { it.equals(normalized, ignoreCase = true) }
            current + (profileId to (listOf(normalized) + retained).take(5))
        }
        return StreamCoreResult.Success(Unit)
    }

    override suspend fun removeHistoryQuery(profileId: String, query: String): StreamCoreResult<Unit> {
        val normalized = SearchQueryNormalizer.normalize(query)
        queries.update { current ->
            current + (profileId to current[profileId].orEmpty().filterNot { it.equals(normalized, ignoreCase = true) })
        }
        return StreamCoreResult.Success(Unit)
    }

    override suspend fun clearHistory(profileId: String): StreamCoreResult<Unit> {
        queries.update { it - profileId }
        return StreamCoreResult.Success(Unit)
    }
}
