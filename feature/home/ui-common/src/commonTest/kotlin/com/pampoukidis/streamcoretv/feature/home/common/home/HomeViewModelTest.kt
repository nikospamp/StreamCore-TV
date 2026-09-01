package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcoretv.core.domain.HomeRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.tracing.NoOpPerformanceTracer
import com.pampoukidis.streamcoretv.core.tracing.PerformanceTracer
import com.pampoukidis.streamcoretv.feature.home.domain.LoadHomeRowsUseCase
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressEntryModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val mainDispatcherRule = MainDispatcherRule()

    @BeforeTest
    fun setUpDispatcher() {
        mainDispatcherRule.setUp()
    }

    @AfterTest
    fun tearDownDispatcher() {
        mainDispatcherRule.tearDown()
    }

    @Test
    fun `load populates rows and ignores duplicate route load`() {
        runTest {
            val rows = listOf(rowModel())
            val repository = FakeHomeRepository(AppResult.Success(rows))
            val subject = homeViewModel(repository)

            subject.onAction(HomeAction.Load("profile-1"))
            subject.onAction(HomeAction.Load("profile-1"))
            runCurrent()

            assertEquals(rows, subject.uiState.value.rows)
            assertFalse(subject.uiState.value.isLoading)
            assertEquals(1, repository.requestCount)
        }
    }

    @Test
    fun `refresh repeats active profile request`() {
        runTest {
            val repository = FakeHomeRepository(AppResult.Success(listOf(rowModel())))
            val subject = homeViewModel(repository)

            subject.onAction(HomeAction.Load("profile-1"))
            runCurrent()
            subject.onAction(HomeAction.Refresh)
            runCurrent()

            assertEquals(2, repository.requestCount)
            assertEquals("profile-1", repository.requestedProfileId)
        }
    }

    @Test
    fun `load failure emits error and stops loading`() {
        runTest {
            val error = AppError.Network()
            val subject = homeViewModel(FakeHomeRepository(AppResult.Failure(error)))
            val effect = async { subject.effects.first() }
            runCurrent()

            subject.onAction(HomeAction.Load("profile-1"))
            runCurrent()

            assertEquals(HomeEffect.ShowError(error), effect.await())
            assertFalse(subject.uiState.value.isLoading)
        }
    }

    @Test
    fun `content selection emits selected content id`() {
        runTest {
            val content = contentModel()
            val subject = homeViewModel()

            subject.onAction(HomeAction.ContentSelected(content))

            assertEquals(HomeEffect.ContentSelected(content), subject.effects.first())
        }
    }

    @Test
    fun `progress prepends reactive continue watching row`() {
        runTest {
            val content = contentModel()
            val progress = MutableStateFlow(emptyList<PlaybackProgressEntryModel>())
            val subject = homeViewModel(
                repository = FakeHomeRepository(AppResult.Success(listOf(rowModel()))),
                progressRepository = FlowPlaybackProgressRepository(progress),
            )

            subject.onAction(HomeAction.Load("profile-1"))
            runCurrent()
            progress.value = listOf(
                PlaybackProgressEntryModel(
                    profileId = "profile-1",
                    contentId = content.id,
                    contentSnapshot = content,
                    positionMillis = 40_000L,
                    durationMillis = 100_000L,
                    updatedAtMillis = 1L,
                ),
            )
            runCurrent()

            val continueWatching = subject.uiState.value.rows.first()
            assertEquals(RowType.ContinueWatching, continueWatching.type)
            assertEquals(40_000L, continueWatching.content.single().playbackProgress?.positionMillis)

            progress.value = emptyList()
            runCurrent()

            assertEquals(listOf(rowModel()), subject.uiState.value.rows)
        }
    }

    @Test
    fun `disabled tracing publishes without invoking trace callbacks or counters`() {
        runTest {
            val tracer = DisabledRecordingTracer()
            val subject = homeViewModel(performanceTracer = tracer)

            subject.onAction(HomeAction.Load("profile-1"))
            runCurrent()

            assertEquals(0, tracer.callbackCount)
        }
    }

    private fun homeViewModel(
        repository: HomeRepository = FakeHomeRepository(AppResult.Success(emptyList())),
        progressRepository: PlaybackProgressRepository = EmptyPlaybackProgressRepository,
        performanceTracer: PerformanceTracer = NoOpPerformanceTracer,
    ): HomeViewModel {
        return HomeViewModel(
            loadHomeRows = LoadHomeRowsUseCase(repository),
            progressRepository = progressRepository,
            performanceTracer = performanceTracer,
        )
    }

    private class DisabledRecordingTracer : PerformanceTracer {
        override val enabled: Boolean = false
        var callbackCount: Int = 0

        override fun beginSection(name: String) {
            callbackCount += 1
        }

        override fun endSection() {
            callbackCount += 1
        }

        override fun counter(name: String, value: Long) {
            callbackCount += 1
        }
    }

    private fun rowModel(): RowModel {
        return RowModel(
            id = "row-1",
            title = "Featured",
            subtitle = "Selected for you",
            content = listOf(contentModel()),
            type = RowType.Featured,
        )
    }

    private fun contentModel(): ContentModel {
        return ContentModel(
            id = "content-1",
            title = "Content",
            description = "Description",
            rating = 8,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "poster",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
        )
    }

    private class FakeHomeRepository(
        private val result: AppResult<List<RowModel>>,
    ) : HomeRepository {

        var requestCount: Int = 0
            private set

        var requestedProfileId: String? = null
            private set

        override suspend fun getHomeRows(profileId: String): AppResult<List<RowModel>> {
            requestCount += 1
            requestedProfileId = profileId
            return result
        }
    }

    private object EmptyPlaybackProgressRepository : PlaybackProgressRepository {
        override fun observe(profileId: String): Flow<List<PlaybackProgressEntryModel>> {
            return flowOf(emptyList())
        }

        override suspend fun get(profileId: String, contentId: String): PlaybackProgressEntryModel? {
            return null
        }

        override suspend fun upsert(entry: PlaybackProgressEntryModel) = Unit
        override suspend fun remove(profileId: String, contentId: String) = Unit
    }

    private class FlowPlaybackProgressRepository(
        private val entries: Flow<List<PlaybackProgressEntryModel>>,
    ) : PlaybackProgressRepository {
        override fun observe(profileId: String): Flow<List<PlaybackProgressEntryModel>> {
            return entries
        }

        override suspend fun get(profileId: String, contentId: String): PlaybackProgressEntryModel? {
            return null
        }

        override suspend fun upsert(entry: PlaybackProgressEntryModel) = Unit
        override suspend fun remove(profileId: String, contentId: String) = Unit
    }
}
