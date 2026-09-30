package com.pampoukidis.streamcoretv.feature.search.common.search

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.api.SearchService
import com.pampoukidis.streamcore.sdk.model.search.StreamCoreSearchInteraction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val mainDispatcherRule = MainDispatcherRule()

    private lateinit var search: FakeSearchService
    private lateinit var viewModel: SearchViewModel

    @BeforeTest
    fun setUp() {
        mainDispatcherRule.setUp()
        search = FakeSearchService()
        viewModel = SearchViewModel(
            search = search,
        )
    }

    @AfterTest
    fun tearDown() {
        mainDispatcherRule.tearDown()
    }

    @Test
    fun `typing normalizes and debounces query`() {
        runTest {
            load()
            viewModel.onAction(SearchAction.QueryChanged("  orbit   fall  "))

            advanceTimeBy(299)
            runCurrent()
            assertTrue(search.queries.isEmpty())

            advanceTimeBy(1)
            runCurrent()

            assertEquals(listOf("orbit fall"), search.queries)
            val results = assertType<SearchContentState.Results>(viewModel.uiState.value.content)
            assertEquals("search:orbit fall", results.items.single().row)
        }
    }

    @Test
    fun `query below threshold does not search`() {
        runTest {
            load()
            viewModel.onAction(SearchAction.QueryChanged(" o "))

            advanceTimeBy(1_000)
            runCurrent()

            assertTrue(search.queries.isEmpty())
            assertType<SearchContentState.Discovery>(viewModel.uiState.value.content)
        }
    }

    @Test
    fun `skeleton is delayed until request has run for 150ms`() {
        runTest {
            search.searchDelayMillis = 200L
            load()
            viewModel.onAction(SearchAction.QueryChanged("orbit"))

            advanceTimeBy(449)
            runCurrent()
            assertType<SearchContentState.Searching>(viewModel.uiState.value.content)

            advanceTimeBy(1)
            runCurrent()
            assertType<SearchContentState.Loading>(viewModel.uiState.value.content)

            advanceTimeBy(50)
            runCurrent()
            assertType<SearchContentState.Results>(viewModel.uiState.value.content)
        }
    }

    @Test
    fun `new query cancels stale result`() {
        runTest {
            search.searchDelayMillis = 400L
            load()
            viewModel.onAction(SearchAction.QueryChanged("orbit"))
            advanceTimeBy(300)
            runCurrent()

            search.searchDelayMillis = 0L
            viewModel.onAction(SearchAction.QueryChanged("northern"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(listOf("orbit", "northern"), search.queries)
            assertEquals("northern", viewModel.uiState.value.resultQuery)
            val results = assertType<SearchContentState.Results>(viewModel.uiState.value.content)
            assertEquals("search:northern", results.items.single().row)
        }
    }

    @Test
    fun `submit executes immediately and records successful query`() {
        runTest {
            load()
            viewModel.onAction(SearchAction.QueryChanged(" orbit "))
            viewModel.onAction(SearchAction.SubmitQuery)
            viewModel.onAction(SearchAction.SubmitQuery)
            runCurrent()

            assertEquals(listOf("orbit"), search.queries)
            assertEquals(listOf("orbit"), search.values.value)
        }
    }

    @Test
    fun `completed query ignores repeated submit and equivalent query changes`() {
        runTest {
            load()
            viewModel.onAction(SearchAction.QueryChanged("Spider Man"))
            advanceTimeBy(300)
            runCurrent()

            viewModel.onAction(SearchAction.SubmitQuery)
            viewModel.onAction(SearchAction.SubmitQuery)
            viewModel.onAction(SearchAction.QueryChanged("  spider   man  "))
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(listOf("Spider Man"), search.queries)
            assertEquals(listOf("Spider Man"), search.values.value)
            assertType<SearchContentState.Results>(viewModel.uiState.value.content)
        }
    }

    @Test
    fun `submit upgrades pending debounce without starting duplicate requests`() {
        runTest {
            load()
            viewModel.onAction(SearchAction.QueryChanged("orbit"))
            viewModel.onAction(SearchAction.SubmitQuery)
            viewModel.onAction(SearchAction.SubmitQuery)
            runCurrent()
            advanceTimeBy(300)
            runCurrent()

            assertEquals(listOf("orbit"), search.queries)
        }
    }

    @Test
    fun `network failure for cached query keeps results with offline notice`() {
        runTest {
            load()
            viewModel.onAction(SearchAction.QueryChanged("orbit"))
            advanceTimeBy(300)
            runCurrent()
            search.searchResult = StreamCoreResult.Failure(StreamCoreError.Network())

            viewModel.onAction(SearchAction.Retry)
            runCurrent()

            assertType<SearchContentState.Results>(viewModel.uiState.value.content)
            assertTrue(viewModel.uiState.value.showOfflineNotice)
        }
    }

    @Test
    fun `non network failure without cache is inline failure`() {
        runTest {
            search.searchResult = StreamCoreResult.Failure(StreamCoreError.Server())
            load()
            viewModel.onAction(SearchAction.QueryChanged("orbit"))
            advanceTimeBy(300)
            runCurrent()

            val failure = assertType<SearchContentState.Failure>(viewModel.uiState.value.content)
            assertType<StreamCoreError.Server>(failure.error)
            assertFalse(viewModel.uiState.value.showOfflineNotice)
        }
    }

    @Test
    fun `retry forces the current failed query to execute again`() {
        runTest {
            search.searchResult = StreamCoreResult.Failure(StreamCoreError.Server())
            load()
            viewModel.onAction(SearchAction.QueryChanged("orbit"))
            advanceTimeBy(300)
            runCurrent()

            search.searchResult = StreamCoreResult.Success(listOf(content()))
            viewModel.onAction(SearchAction.Retry)
            runCurrent()

            assertEquals(listOf("orbit", "orbit"), search.queries)
            assertType<SearchContentState.Results>(viewModel.uiState.value.content)
        }
    }

    @Test
    fun `result selection emits once and records result query`() {
        runTest {
            load()
            viewModel.onAction(SearchAction.QueryChanged("orbit"))
            advanceTimeBy(300)
            runCurrent()
            val item = (viewModel.uiState.value.content as SearchContentState.Results).items.single()

            viewModel.onAction(SearchAction.ResultSelected(item))
            val effect = viewModel.effects.first()

            assertEquals(SearchEffect.ContentSelected(item), effect)
            assertEquals(listOf("orbit"), search.values.value)
        }
    }

    @Test
    fun `recent operations are delegated per active profile`() {
        runTest {
            load()
            search.recordHistory(ProfileId, "Orbit")
            runCurrent()

            viewModel.onAction(SearchAction.RecentRemoved("Orbit"))
            runCurrent()
            assertTrue(search.values.value.isEmpty())

            search.recordHistory(ProfileId, "Northern")
            viewModel.onAction(SearchAction.ClearRecent)
            runCurrent()
            assertTrue(search.values.value.isEmpty())
        }
    }

    private fun load() {
        viewModel.onAction(SearchAction.Load(ProfileId))
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
    }

    private inline fun <reified T> assertType(value: Any?): T {
        assertTrue(
            actual = value is T,
            message = "Expected ${T::class.simpleName}, was ${value?.let { it::class.simpleName }}",
        )
        return value as T
    }

    private class FakeSearchService : SearchService {
        val queries = mutableListOf<String>()
        val values = MutableStateFlow<List<String>>(emptyList())
        var searchDelayMillis = 0L
        var searchResult: StreamCoreResult<List<StreamCoreContent>> = StreamCoreResult.Success(listOf(content()))
        var discoveryResult: StreamCoreResult<List<StreamCoreContent>> = StreamCoreResult.Success(listOf(content("trending")))

        override suspend fun search(profileId: String, query: String, interaction: StreamCoreSearchInteraction): StreamCoreResult<List<StreamCoreContent>> {
            queries += query
            delay(searchDelayMillis)
            val result = searchResult
            if (result is StreamCoreResult.Success) displayedResults(profileId, query, result.value, interaction)
            return result
        }

        override suspend fun loadTrending(profileId: String): StreamCoreResult<List<StreamCoreContent>> {
            return discoveryResult
        }

        override suspend fun displayedResults(profileId: String, query: String, results: List<StreamCoreContent>, interaction: StreamCoreSearchInteraction): StreamCoreResult<Unit> {
            if (interaction == StreamCoreSearchInteraction.Typing || results.isEmpty()) return StreamCoreResult.Success(Unit)
            return recordHistory(profileId, query)
        }

        override suspend fun resultSelected(profileId: String, query: String): StreamCoreResult<Unit> {
            return recordHistory(profileId, query)
        }

        override fun observeHistory(profileId: String): Flow<StreamCoreResult<List<String>>> {
            return values.map { StreamCoreResult.Success(it) }
        }

        override suspend fun recordHistory(profileId: String, query: String): StreamCoreResult<Unit> {
            values.value = listOf(query) + values.value.filterNot { value -> value.equals(query, ignoreCase = true) }
            return StreamCoreResult.Success(Unit)
        }

        override suspend fun removeHistoryQuery(profileId: String, query: String): StreamCoreResult<Unit> {
            values.value = values.value.filterNot { value -> value.equals(query, ignoreCase = true) }
            return StreamCoreResult.Success(Unit)
        }

        override suspend fun clearHistory(profileId: String): StreamCoreResult<Unit> {
            values.value = emptyList()
            return StreamCoreResult.Success(Unit)
        }
    }

    private companion object {
        const val ProfileId = "profile-1"

        fun content(id: String = "orbit"): StreamCoreContent {
            return StreamCoreContent(
                id = id,
                title = "Orbit Fall",
                description = "Description",
                rating = 9,
                pgRatingName = "PG-13",
                pgRatingLevel = 13,
                poster = "poster",
                backdrop = "backdrop",
                cast = emptyList(),
                releaseDate = 0L,
                genres = emptyList(),
            )
        }
    }
}
