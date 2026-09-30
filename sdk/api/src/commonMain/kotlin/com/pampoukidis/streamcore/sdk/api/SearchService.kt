package com.pampoukidis.streamcore.sdk.api

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.search.StreamCoreSearchInteraction
import kotlinx.coroutines.flow.Flow

/**
 * Catalogue search, search interactions and local search-history management.
 * Every profile ID must match the current authorized activation. Search and history capabilities are distinct;
 * an unsupported operation returns `StreamCoreError.Unsupported` without implying a session failure.
 */
interface SearchService {
    /**
     * Normalizes the query; below-minimum queries return an empty result after context validation.
     * Typing does not record history. Submitted/recent-selected searches record successful nonempty results.
     * A history-storage failure does not replace successful catalogue results; cancellation and context loss still apply.
     */
    suspend fun search(profileId: String, query: String, interaction: StreamCoreSearchInteraction = StreamCoreSearchInteraction.Typing): StreamCoreResult<List<StreamCoreContent>>
    /** Loads provider discovery content subject to search capability and current profile restrictions. */
    suspend fun loadTrending(profileId: String): StreamCoreResult<List<StreamCoreContent>>
    /**
     * Records an explicit interaction with already-loaded nonempty results, without repeating the catalogue query.
     * Returns the history operation's result. Typing/empty results do not record history.
     */
    suspend fun displayedResults(profileId: String, query: String, results: List<StreamCoreContent>, interaction: StreamCoreSearchInteraction): StreamCoreResult<Unit>
    /** Records the query associated with an opened search result; returns the history operation's result. */
    suspend fun resultSelected(profileId: String, query: String): StreamCoreResult<Unit>

    /**
     * Observes this profile's recent queries, capturing authorization at flow creation.
     * Create and collect a new flow after profileActivationId changes; an old observer never attaches to a
     * subsequent activation. The host owns collector cancellation.
     */
    fun observeHistory(profileId: String): Flow<StreamCoreResult<List<String>>>

    /**
     * Explicitly records a submitted query or opened result. Normalizes input, ignores too-short queries and
     * deduplicates case-insensitively, retaining the recent spelling. Normal search interactions already record
     * history through [search], [displayedResults] and [resultSelected]; do not repeat the same interaction here.
     */
    suspend fun recordHistory(profileId: String, query: String): StreamCoreResult<Unit>

    /** Removes a normalized query case-insensitively from the active profile's history. */
    suspend fun removeHistoryQuery(profileId: String, query: String): StreamCoreResult<Unit>

    /** Clears only the active profile's account-owned search history. */
    suspend fun clearHistory(profileId: String): StreamCoreResult<Unit>
}
