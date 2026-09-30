package com.pampoukidis.streamcoretv.feature.search.common.search

import com.pampoukidis.streamcore.sdk.api.SearchService
import com.pampoukidis.streamcore.sdk.model.search.StreamCoreSearchInteraction
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.api.validation.SearchQueryNormalizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel constructor(
    private val search: SearchService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<SearchEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<SearchEffect> = effectsChannel.receiveAsFlow()

    private val searchRequests = Channel<SearchRequest>(capacity = Channel.CONFLATED)
    private val resultCache = SearchResultCache(MaxCachedQueries)

    private var activeProfileId: String? = null
    private var recentSearchesJob: Job? = null
    private var discoveryJob: Job? = null
    private var queuedQueryKey: String? = null
    private var queuedDebounceMillis: Long? = null
    private var startedQueryKey: String? = null

    init {
        observeSearchRequests()
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.Load -> load(action.profileId)
            is SearchAction.QueryChanged -> changeQuery(action.query)
            SearchAction.ClearQuery -> clearQuery()
            SearchAction.SubmitQuery -> submitQuery()
            is SearchAction.RecentSelected -> selectRecent(action.query)
            is SearchAction.RecentRemoved -> removeRecent(action.query)
            SearchAction.ClearRecent -> clearRecent()
            is SearchAction.TrendingSelected -> selectContent(
                content = action.content,
                recordQuery = false,
                sourceArtworkUrl = action.sourceArtworkUrl,
            )
            is SearchAction.ResultSelected -> selectContent(
                content = action.content,
                recordQuery = true,
                sourceArtworkUrl = action.sourceArtworkUrl,
            )
            SearchAction.Retry -> retry()
        }
    }

    private fun load(profileId: String) {
        if (activeProfileId == profileId) {
            return
        }

        activeProfileId = profileId
        resultCache.clear()
        resetQueryDeduplication()
        observeRecents(profileId)
        loadDiscovery(profileId)
    }

    private fun observeRecents(profileId: String) {
        recentSearchesJob?.cancel()
        recentSearchesJob = viewModelScope.launch {
            search.observeHistory(profileId).collect { result ->
                if (activeProfileId == profileId && result is StreamCoreResult.Success) {
                    _uiState.update { state -> state.copy(recentQueries = result.value) }
                }
            }
        }
    }

    private fun loadDiscovery(profileId: String) {
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            when (val result = search.loadTrending(profileId)) {
                is StreamCoreResult.Success -> {
                    if (activeProfileId == profileId) {
                        _uiState.update { state ->
                            state.copy(
                                trending = result.value.take(MaxTrendingItems).map { content ->
                                    content.copy(row = TrendingRow)
                                },
                            )
                        }
                    }
                }

                is StreamCoreResult.Failure -> Unit
            }
        }
    }

    private fun changeQuery(query: String) {
        val normalizedQuery = SearchQueryNormalizer.normalize(query)
        val isSearchable = SearchQueryNormalizer.isSearchable(normalizedQuery)
        val willQueueSearch = isSearchable && shouldQueueSearch(
            query = normalizedQuery,
            debounceMillis = SearchDebounceMillis,
        )
        _uiState.update { state ->
            state.copy(
                query = query,
                resultQuery = if (isSearchable) state.resultQuery else null,
                content = if (willQueueSearch) {
                    SearchContentState.Searching
                } else if (!isSearchable) {
                    SearchContentState.Discovery
                } else {
                    state.content
                },
                showOfflineNotice = if (willQueueSearch) false else state.showOfflineNotice,
            )
        }

        if (willQueueSearch) {
            queueSearch(
                query = normalizedQuery,
                debounceMillis = SearchDebounceMillis,
                recordOnSuccess = false,
            )
        } else if (!isSearchable) {
            resetQueryDeduplication()
            searchRequests.trySend(SearchRequest.Cancel)
        }
    }

    private fun clearQuery() {
        _uiState.update { state ->
            state.copy(
                query = "",
                resultQuery = null,
                content = SearchContentState.Discovery,
                showOfflineNotice = false,
            )
        }
        resetQueryDeduplication()
        searchRequests.trySend(SearchRequest.Cancel)
    }

    private fun submitQuery() {
        executeImmediately(
            query = _uiState.value.query,
            recordOnSuccess = true,
        )
    }

    private fun selectRecent(query: String) {
        _uiState.update { state -> state.copy(query = query) }
        executeImmediately(
            query = query,
            recordOnSuccess = true,
        )
    }

    private fun retry() {
        executeImmediately(
            query = _uiState.value.query,
            recordOnSuccess = false,
            force = true,
        )
    }

    private fun executeImmediately(
        query: String,
        recordOnSuccess: Boolean,
        force: Boolean = false,
    ) {
        val normalizedQuery = SearchQueryNormalizer.normalize(query)
        if (!SearchQueryNormalizer.isSearchable(normalizedQuery)) {
            return
        }

        val willQueueSearch = force || shouldQueueSearch(
            query = normalizedQuery,
            debounceMillis = 0L,
        )

        _uiState.update { state ->
            state.copy(
                query = normalizedQuery,
                content = if (willQueueSearch) {
                    SearchContentState.Searching
                } else {
                    state.content
                },
                showOfflineNotice = if (willQueueSearch) false else state.showOfflineNotice,
            )
        }

        if (!willQueueSearch) {
            if (recordOnSuccess) {
                recordDisplayedQuery(normalizedQuery)
            }
            return
        }

        queueSearch(
            query = normalizedQuery,
            debounceMillis = 0L,
            recordOnSuccess = recordOnSuccess,
        )
    }

    private fun recordDisplayedQuery(query: String) {
        val profileId = activeProfileId ?: return
        val state = _uiState.value
        val isDisplayedQuery = state.content is SearchContentState.Results &&
                state.resultQuery?.let(::queryKey) == queryKey(query)
        if (!isDisplayedQuery) {
            return
        }

        viewModelScope.launch {
            search.displayedResults(
                profileId, query, (state.content as SearchContentState.Results).items, StreamCoreSearchInteraction.Submitted,
            )
        }
    }

    private fun shouldQueueSearch(
        query: String,
        debounceMillis: Long,
    ): Boolean {
        val queryKey = queryKey(query)
        if (queryKey != queuedQueryKey) {
            return true
        }

        return debounceMillis == 0L &&
                queuedDebounceMillis != 0L &&
                startedQueryKey != queryKey
    }

    private fun queueSearch(
        query: String,
        debounceMillis: Long,
        recordOnSuccess: Boolean,
    ) {
        queuedQueryKey = queryKey(query)
        queuedDebounceMillis = debounceMillis
        searchRequests.trySend(
            SearchRequest.Execute(
                query = query,
                debounceMillis = debounceMillis,
                recordOnSuccess = recordOnSuccess,
            ),
        )
    }

    private fun resetQueryDeduplication() {
        queuedQueryKey = null
        queuedDebounceMillis = null
        startedQueryKey = null
    }

    private fun removeRecent(query: String) {
        val profileId = activeProfileId ?: return
        viewModelScope.launch {
            search.removeHistoryQuery(profileId, query)
        }
    }

    private fun clearRecent() {
        val profileId = activeProfileId ?: return
        viewModelScope.launch {
            search.clearHistory(profileId)
        }
    }

    private fun selectContent(
        content: StreamCoreContent,
        recordQuery: Boolean,
        sourceArtworkUrl: String?,
    ) {
        viewModelScope.launch {
            val profileId = activeProfileId
            val resultQuery = _uiState.value.resultQuery
            if (recordQuery && profileId != null && resultQuery != null) {
                search.resultSelected(profileId, resultQuery)
            }
            effectsChannel.send(SearchEffect.ContentSelected(content, sourceArtworkUrl))
        }
    }

    private fun observeSearchRequests() {
        viewModelScope.launch {
            searchRequests.receiveAsFlow()
                .flatMapLatest { request -> createExecutionFlow(request) }
                .collect(::applyExecution)
        }
    }

    private fun createExecutionFlow(request: SearchRequest): Flow<SearchExecution> {
        return when (request) {
            SearchRequest.Cancel -> flow { emit(SearchExecution.Cancelled) }
            is SearchRequest.Execute -> executeSearch(request)
        }
    }

    private fun executeSearch(request: SearchRequest.Execute): Flow<SearchExecution> {
        return flow {
            delay(request.debounceMillis)
            val profileId = activeProfileId ?: return@flow
            startedQueryKey = queryKey(request.query)
            coroutineScope {
                val resultDeferred = async {
                    search.search(
                        profileId = profileId,
                        query = request.query,
                        interaction = if (request.recordOnSuccess) StreamCoreSearchInteraction.Submitted else StreamCoreSearchInteraction.Typing,
                    )
                }
                val immediateResult = withTimeoutOrNull(SkeletonDelayMillis) {
                    resultDeferred.await()
                }
                if (immediateResult == null) {
                    emit(SearchExecution.Loading(request.query))
                    emit(SearchExecution.Completed(request, resultDeferred.await()))
                } else {
                    emit(SearchExecution.Completed(request, immediateResult))
                }
            }
        }
    }

    private suspend fun applyExecution(execution: SearchExecution) {
        when (execution) {
            SearchExecution.Cancelled -> Unit
            is SearchExecution.Loading -> {
                if (currentNormalizedQuery() == execution.query) {
                    _uiState.update { state ->
                        state.copy(
                            resultQuery = execution.query,
                            content = SearchContentState.Loading,
                            showOfflineNotice = false,
                        )
                    }
                }
            }

            is SearchExecution.Completed -> applyResult(
                request = execution.request,
                result = execution.result,
            )
        }
    }

    private suspend fun applyResult(
        request: SearchRequest.Execute,
        result: StreamCoreResult<List<StreamCoreContent>>,
    ) {
        if (currentNormalizedQuery() != request.query) {
            return
        }

        when (result) {
            is StreamCoreResult.Success -> {
                val items = result.value.map { content ->
                    content.copy(row = searchRow(request.query))
                }
                if (items.isNotEmpty()) {
                    resultCache[request.query] = items
                }
                _uiState.update { state ->
                    state.copy(
                        resultQuery = request.query,
                        content = if (items.isEmpty()) {
                            SearchContentState.Empty(request.query)
                        } else {
                            SearchContentState.Results(items)
                        },
                        showOfflineNotice = false,
                    )
                }
            }

            is StreamCoreResult.Failure -> applyFailure(request.query, result.error)
        }
    }

    private fun applyFailure(
        query: String,
        error: StreamCoreError,
    ) {
        val cachedItems = resultCache[query]
        val canUseCache = cachedItems != null && (error is StreamCoreError.Network || error is StreamCoreError.Timeout)
        _uiState.update { state ->
            state.copy(
                resultQuery = query,
                content = if (canUseCache) {
                    SearchContentState.Results(cachedItems.orEmpty())
                } else {
                    SearchContentState.Failure(error)
                },
                showOfflineNotice = canUseCache,
            )
        }
    }

    private fun currentNormalizedQuery(): String {
        return SearchQueryNormalizer.normalize(_uiState.value.query)
    }

    private fun searchRow(query: String): String {
        return "search:$query"
    }

    private fun queryKey(query: String): String {
        return SearchQueryNormalizer.normalize(query).lowercase()
    }

    private sealed interface SearchRequest {
        data object Cancel : SearchRequest
        data class Execute(
            val query: String,
            val debounceMillis: Long,
            val recordOnSuccess: Boolean,
        ) : SearchRequest
    }

    private sealed interface SearchExecution {
        data object Cancelled : SearchExecution
        data class Loading(val query: String) : SearchExecution
        data class Completed(
            val request: SearchRequest.Execute,
            val result: StreamCoreResult<List<StreamCoreContent>>,
        ) : SearchExecution
    }

    private companion object {
        const val SearchDebounceMillis = 300L
        const val SkeletonDelayMillis = 150L
        const val MaxTrendingItems = 6
        const val MaxCachedQueries = 10
        const val TrendingRow = "search:trending"
    }
}

private class SearchResultCache(
    private val maximumSize: Int,
) {
    private val entries = linkedMapOf<String, List<StreamCoreContent>>()

    operator fun get(key: String): List<StreamCoreContent>? {
        val value = entries.remove(key) ?: return null
        entries[key] = value
        return value
    }

    operator fun set(key: String, value: List<StreamCoreContent>) {
        entries.remove(key)
        entries[key] = value
        if (entries.size > maximumSize) {
            entries.remove(entries.keys.first())
        }
    }

    fun clear() {
        entries.clear()
    }
}
