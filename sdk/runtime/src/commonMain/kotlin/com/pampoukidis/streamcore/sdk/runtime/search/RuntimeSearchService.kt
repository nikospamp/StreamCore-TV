package com.pampoukidis.streamcore.sdk.runtime.search

import com.pampoukidis.streamcore.sdk.api.SearchService
import com.pampoukidis.streamcore.sdk.validation.SearchQueryNormalizer
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.search.StreamCoreSearchInteraction
import com.pampoukidis.streamcore.sdk.runtime.session.AuthorizedProfile
import com.pampoukidis.streamcore.sdk.runtime.session.RuntimeSession
import com.pampoukidis.streamcore.sdk.runtime.session.unsupported
import com.pampoukidis.streamcore.sdk.runtime.storage.SdkLocalRepositories
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Shared search behavior; backend work stays in the matching provider. */
internal class RuntimeSearchService(
    private val runtimeSession: RuntimeSession,
    private val capabilities: StreamCoreCapabilities,
    private val local: SdkLocalRepositories,
) : SearchService {
    override fun observeHistory(profileId: String): Flow<StreamCoreResult<List<String>>> {
        return runtimeSession.observeStoredProfileData(profileId, runtimeSession.currentAuthorization, capabilities.searchHistory, "search.history") { authorization, _ ->
            local.history.observe(runtimeSession.storageKey(authorization)).map { StreamCoreResult.Success(it) }
        }
    }
    override suspend fun recordHistory(profileId: String, query: String): StreamCoreResult<Unit> {
        return recordHistoryForProfile(profileId, query, runtimeSession.currentAuthorization)
    }
    override suspend fun removeHistoryQuery(profileId: String, query: String): StreamCoreResult<Unit> {
        return historyMutation(profileId, runtimeSession.currentAuthorization) { authorization ->
            local.history.remove(runtimeSession.storageKey(authorization), SearchQueryNormalizer.normalize(query))
        }
    }
    override suspend fun clearHistory(profileId: String): StreamCoreResult<Unit> {
        return historyMutation(profileId, runtimeSession.currentAuthorization) { authorization -> local.history.clear(runtimeSession.storageKey(authorization)) }
    }
    override suspend fun search(profileId: String, query: String, interaction: StreamCoreSearchInteraction): StreamCoreResult<List<StreamCoreContent>> {
        val captured = runtimeSession.currentAuthorization
        val result = searchForProfile(profileId, query, captured)
        // withAuthorizedProfile already validated and applied authoritative rejection to runtimeSession.context.
        if (result is StreamCoreResult.Failure) {
            return result
        }
        currentCoroutineContext().ensureActive()
        if (result is StreamCoreResult.Success && interaction != StreamCoreSearchInteraction.Typing && result.value.isNotEmpty()) {
            // Retain successful catalogue results even when a history checkpoint fails.
            recordHistoryForProfile(profileId, query, captured)
        }
        return if (captured == null) {
            result
        } else {
            runtimeSession.checkProfileResult(captured, result)
        }
    }
    override suspend fun loadTrending(profileId: String): StreamCoreResult<List<StreamCoreContent>> {
        if (!capabilities.search) return unsupported("search")
        return runtimeSession.withAuthorizedProfile(profileId) { authorization, profile -> runtimeSession.filterAllowedContents(authorization.providers.search.loadTrending(profileId), authorization, profile) }
    }
    override suspend fun displayedResults(profileId: String, query: String, results: List<StreamCoreContent>, interaction: StreamCoreSearchInteraction): StreamCoreResult<Unit> {
        val captured = runtimeSession.currentAuthorization
        return runtimeSession.withAuthorizedProfile(profileId, captured) { _, _ ->
            if (interaction == StreamCoreSearchInteraction.Typing || results.isEmpty()) {
                StreamCoreResult.Success(Unit)
            } else {
                recordHistoryForProfile(profileId, query, captured)
            }
        }
    }
    override suspend fun resultSelected(profileId: String, query: String): StreamCoreResult<Unit> {
        return recordHistoryForProfile(profileId, query, runtimeSession.currentAuthorization)
    }

    private suspend fun searchForProfile(profileId: String, query: String, captured: AuthorizedProfile?): StreamCoreResult<List<StreamCoreContent>> {
        if (!capabilities.search) return unsupported("search")
        val normalized = SearchQueryNormalizer.normalize(query)
        return runtimeSession.withAuthorizedProfile(profileId, captured) { authorization, profile ->
            if (!SearchQueryNormalizer.isSearchable(normalized)) {
                StreamCoreResult.Success(emptyList())
            } else {
                val searchResult = authorization.providers.search.search(profileId, normalized)
                runtimeSession.filterAllowedContents(searchResult, authorization, profile)
            }
        }
    }

    private suspend fun recordHistoryForProfile(profileId: String, query: String, captured: AuthorizedProfile?): StreamCoreResult<Unit> {
        val normalized = SearchQueryNormalizer.normalize(query)
        return historyMutation(profileId, captured) { authorization ->
            if (SearchQueryNormalizer.isSearchable(normalized)) {
                local.history.add(runtimeSession.storageKey(authorization), normalized)
            }
        }
    }

    private suspend fun historyMutation(profileId: String, captured: AuthorizedProfile?, block: suspend (AuthorizedProfile) -> Unit): StreamCoreResult<Unit> {
        if (!capabilities.searchHistory) return unsupported("search.history")
        return runtimeSession.withAuthorizedProfile(profileId, captured) { authorization, _ ->
            block(authorization)
            StreamCoreResult.Success(Unit)
        }
    }
}
