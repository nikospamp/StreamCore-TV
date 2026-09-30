package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcore.sdk.api.HomeService
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollectionPurpose
import com.pampoukidis.streamcoretv.core.model.content.toRowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcoretv.core.tracing.NoOpPerformanceTracer
import com.pampoukidis.streamcoretv.core.tracing.PerformanceTracer
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.api.PlaybackService
import com.pampoukidis.streamcore.sdk.api.PlaybackProgressRecorder
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackMedia
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.map
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
            val repository = FakeHomeRepository(StreamCoreResult.Success(rows))
            val subject = homeViewModel(repository)

            subject.onAction(HomeAction.Load("profile-1"))
            subject.onAction(HomeAction.Load("profile-1"))
            runCurrent()

            assertEquals(rows.map { it.toRowModel() }, subject.uiState.value.rows)
            assertFalse(subject.uiState.value.isLoading)
            assertEquals(1, repository.requestCount)
        }
    }

    @Test
    fun `refresh repeats active profile request`() {
        runTest {
            val repository = FakeHomeRepository(StreamCoreResult.Success(listOf(rowModel())))
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
            val error = StreamCoreError.Network()
            val subject = homeViewModel(FakeHomeRepository(StreamCoreResult.Failure(error)))
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
            val progress = MutableStateFlow(emptyList<StreamCorePlaybackProgressEntry>())
            val subject = homeViewModel(
                repository = FakeHomeRepository(StreamCoreResult.Success(listOf(rowModel()))),
                playback = FlowPlaybackService(progress),
            )

            subject.onAction(HomeAction.Load("profile-1"))
            runCurrent()
            progress.value = listOf(
                StreamCorePlaybackProgressEntry(
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

            assertEquals(listOf(rowModel().toRowModel()), subject.uiState.value.rows)
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
        repository: HomeService = FakeHomeRepository(StreamCoreResult.Success(emptyList())),
        playback: PlaybackService = EmptyPlaybackService,
        performanceTracer: PerformanceTracer = NoOpPerformanceTracer,
    ): HomeViewModel {
        return HomeViewModel(
            homeRepository = repository,
            playback = playback,
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

    private fun rowModel(): StreamCoreCollection {
        return StreamCoreCollection(
            id = "row-1",
            title = "Featured",
            subtitle = "Selected for you",
            content = listOf(contentModel()),
            purpose = StreamCoreCollectionPurpose.Featured,
        )
    }

    private fun contentModel(): StreamCoreContent {
        return StreamCoreContent(
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
        private val result: StreamCoreResult<List<StreamCoreCollection>>,
    ) : HomeService {

        var requestCount: Int = 0
            private set

        var requestedProfileId: String? = null
            private set

        override suspend fun getCollections(profileId: String): StreamCoreResult<List<StreamCoreCollection>> {
            requestCount += 1
            requestedProfileId = profileId
            return result
        }
    }

    private object EmptyPlaybackService : PlaybackService {
        override suspend fun resolveSource(request: StreamCorePlaybackRequest): StreamCoreResult<StreamCorePlaybackMedia> { error("Not used by this screen") }
        override fun observeProgress(profileId: String): Flow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>> {
            return flowOf(StreamCoreResult.Success(emptyList()))
        }

        override suspend fun getProgress(profileId: String, contentId: String): StreamCoreResult<StreamCorePlaybackProgressEntry?> {
            return StreamCoreResult.Success(null)
        }

        override suspend fun updateProgress(entry: StreamCorePlaybackProgressEntry): StreamCoreResult<Unit> { return StreamCoreResult.Success(Unit) }
        override suspend fun removeProgress(profileId: String, contentId: String): StreamCoreResult<Unit> { return StreamCoreResult.Success(Unit) }
        override fun createProgressRecorder(request: StreamCorePlaybackRequest, initialPositionMillis: Long): PlaybackProgressRecorder { error("Not used by this screen") }
    }

    private class FlowPlaybackService(
        private val entries: Flow<List<StreamCorePlaybackProgressEntry>>,
    ) : PlaybackService {
        override suspend fun resolveSource(request: StreamCorePlaybackRequest): StreamCoreResult<StreamCorePlaybackMedia> { error("Not used by this screen") }
        override fun observeProgress(profileId: String): Flow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>> {
            return entries.map { StreamCoreResult.Success(it) }
        }

        override suspend fun getProgress(profileId: String, contentId: String): StreamCoreResult<StreamCorePlaybackProgressEntry?> {
            return StreamCoreResult.Success(null)
        }

        override suspend fun updateProgress(entry: StreamCorePlaybackProgressEntry): StreamCoreResult<Unit> { return StreamCoreResult.Success(Unit) }
        override suspend fun removeProgress(profileId: String, contentId: String): StreamCoreResult<Unit> { return StreamCoreResult.Success(Unit) }
        override fun createProgressRecorder(request: StreamCorePlaybackRequest, initialPositionMillis: Long): PlaybackProgressRecorder { error("Not used by this screen") }
    }
}
