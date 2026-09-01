package com.pampoukidis.streamcoretv.feature.search.common.search

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.search.domain.AddRecentSearchUseCase
import com.pampoukidis.streamcoretv.feature.search.domain.ClearRecentSearchesUseCase
import com.pampoukidis.streamcoretv.feature.search.domain.LoadSearchDiscoveryUseCase
import com.pampoukidis.streamcoretv.feature.search.domain.ObserveRecentSearchesUseCase
import com.pampoukidis.streamcoretv.feature.search.domain.RecentSearchRepository
import com.pampoukidis.streamcoretv.feature.search.domain.RemoveRecentSearchUseCase
import com.pampoukidis.streamcoretv.feature.search.domain.SearchContentUseCase
import com.pampoukidis.streamcoretv.feature.search.domain.SearchRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

    private lateinit var searchRepository: FakeSearchRepository
    private lateinit var recentRepository: FakeRecentSearchRepository
    private lateinit var viewModel: SearchViewModel

    @BeforeTest
    fun setUp() {
        mainDispatcherRule.setUp()
        searchRepository = FakeSearchRepository()
        recentRepository = FakeRecentSearchRepository()
        viewModel = SearchViewModel(
            searchContent = SearchContentUseCase(searchRepository),
            loadSearchDiscovery = LoadSearchDiscoveryUseCase(searchRepository),
            observeRecentSearches = ObserveRecentSearchesUseCase(recentRepository),
            addRecentSearch = AddRecentSearchUseCase(recentRepository),
            removeRecentSearch = RemoveRecentSearchUseCase(recentRepository),
            clearRecentSearches = ClearRecentSearchesUseCase(recentRepository),
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
            assertTrue(searchRepository.queries.isEmpty())

            advanceTimeBy(1)
            runCurrent()

            assertEquals(listOf("orbit fall"), searchRepository.queries)
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

            assertTrue(searchRepository.queries.isEmpty())
            assertType<SearchContentState.Discovery>(viewModel.uiState.value.content)
        }
    }

    @Test
    fun `skeleton is delayed until request has run for 150ms`() {
        runTest {
            searchRepository.searchDelayMillis = 200L
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
            searchRepository.searchDelayMillis = 400L
            load()
            viewModel.onAction(SearchAction.QueryChanged("orbit"))
            advanceTimeBy(300)
            runCurrent()

            searchRepository.searchDelayMillis = 0L
            viewModel.onAction(SearchAction.QueryChanged("northern"))
            advanceTimeBy(300)
            runCurrent()

            assertEquals(listOf("orbit", "northern"), searchRepository.queries)
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

            assertEquals(listOf("orbit"), searchRepository.queries)
            assertEquals(listOf("orbit"), recentRepository.values.value)
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

            assertEquals(listOf("Spider Man"), searchRepository.queries)
            assertEquals(listOf("Spider Man"), recentRepository.values.value)
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

            assertEquals(listOf("orbit"), searchRepository.queries)
        }
    }

    @Test
    fun `network failure for cached query keeps results with offline notice`() {
        runTest {
            load()
            viewModel.onAction(SearchAction.QueryChanged("orbit"))
            advanceTimeBy(300)
            runCurrent()
            searchRepository.searchResult = AppResult.Failure(AppError.Network())

            viewModel.onAction(SearchAction.Retry)
            runCurrent()

            assertType<SearchContentState.Results>(viewModel.uiState.value.content)
            assertTrue(viewModel.uiState.value.showOfflineNotice)
        }
    }

    @Test
    fun `non network failure without cache is inline failure`() {
        runTest {
            searchRepository.searchResult = AppResult.Failure(AppError.Server())
            load()
            viewModel.onAction(SearchAction.QueryChanged("orbit"))
            advanceTimeBy(300)
            runCurrent()

            val failure = assertType<SearchContentState.Failure>(viewModel.uiState.value.content)
            assertType<AppError.Server>(failure.error)
            assertFalse(viewModel.uiState.value.showOfflineNotice)
        }
    }

    @Test
    fun `retry forces the current failed query to execute again`() {
        runTest {
            searchRepository.searchResult = AppResult.Failure(AppError.Server())
            load()
            viewModel.onAction(SearchAction.QueryChanged("orbit"))
            advanceTimeBy(300)
            runCurrent()

            searchRepository.searchResult = AppResult.Success(listOf(content()))
            viewModel.onAction(SearchAction.Retry)
            runCurrent()

            assertEquals(listOf("orbit", "orbit"), searchRepository.queries)
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
            assertEquals(listOf("orbit"), recentRepository.values.value)
        }
    }

    @Test
    fun `recent operations are delegated per active profile`() {
        runTest {
            load()
            recentRepository.add(ProfileId, "Orbit")
            runCurrent()

            viewModel.onAction(SearchAction.RecentRemoved("Orbit"))
            runCurrent()
            assertTrue(recentRepository.values.value.isEmpty())

            recentRepository.add(ProfileId, "Northern")
            viewModel.onAction(SearchAction.ClearRecent)
            runCurrent()
            assertTrue(recentRepository.values.value.isEmpty())
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

    private class FakeSearchRepository : SearchRepository {
        val queries = mutableListOf<String>()
        var searchDelayMillis = 0L
        var searchResult: AppResult<List<ContentModel>> = AppResult.Success(listOf(content()))
        var discoveryResult: AppResult<List<ContentModel>> = AppResult.Success(listOf(content("trending")))

        override suspend fun search(
            profileId: String,
            query: String,
        ): AppResult<List<ContentModel>> {
            queries += query
            delay(searchDelayMillis)
            return searchResult
        }

        override suspend fun loadTrending(profileId: String): AppResult<List<ContentModel>> {
            return discoveryResult
        }
    }

    private class FakeRecentSearchRepository : RecentSearchRepository {
        val values = MutableStateFlow<List<String>>(emptyList())

        override fun observe(profileId: String): Flow<List<String>> {
            return values
        }

        override suspend fun add(profileId: String, query: String) {
            values.value = listOf(query) + values.value.filterNot { value ->
                value.equals(query, ignoreCase = true)
            }
        }

        override suspend fun remove(profileId: String, query: String) {
            values.value = values.value.filterNot { value ->
                value.equals(query, ignoreCase = true)
            }
        }

        override suspend fun clear(profileId: String) {
            values.value = emptyList()
        }
    }

    private companion object {
        const val ProfileId = "profile-1"

        fun content(id: String = "orbit"): ContentModel {
            return ContentModel(
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
