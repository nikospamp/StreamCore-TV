package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcore.sdk.api.DetailsService
import com.pampoukidis.streamcore.sdk.api.LibraryService
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibrary
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreContentLibraryState
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreTrailer
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibraryEntry
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreDetailsRequest
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.api.PlaybackService
import com.pampoukidis.streamcore.sdk.api.PlaybackProgressRecorder
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackMedia
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

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
    fun `trailer selection emits preferred loaded trailer`() {
        runTest {
            val trailer = StreamCoreTrailer("official", "Official Trailer", "https://www.youtube.com/watch?v=abcdefghijk")
            val content = contentModel("content-1").copy(
                trailers = listOf(trailer, trailer.copy(id = "other")),
            )
            val subject = detailsViewModel(
                FakeDetailsRepository(
                    detailsResult = StreamCoreResult.Success(content),
                    recommendationsResult = StreamCoreResult.Success(emptyList()),
                ),
            )
            subject.onAction(DetailsAction.Load(StreamCoreDetailsRequest("profile-1", content.id)))
            runCurrent()
            subject.onAction(DetailsAction.TrailerSelected)
            assertEquals(DetailsEffect.OpenTrailer(trailer), subject.effects.first())
        }
    }

    @Test
    fun `trailer selection without a trailer emits nothing`() {
        runTest {
            val subject = detailsViewModel()
            subject.onAction(DetailsAction.TrailerSelected)
            subject.onAction(DetailsAction.Load(StreamCoreDetailsRequest("profile-1", "content-1")))
            runCurrent()
            subject.onAction(DetailsAction.TrailerSelected)
            subject.onAction(DetailsAction.BackSelected)
            assertEquals(DetailsEffect.NavigateBack, subject.effects.first())
        }
    }

    @Test
    fun `load populates content and ignores duplicate request`() {
        runTest {
            val content = contentModel("content-1")
            val recommendations = listOf(contentModel("content-2"))
            val repository = FakeDetailsRepository(
                detailsResult = StreamCoreResult.Success(content),
                recommendationsResult = StreamCoreResult.Success(recommendations),
            )
            val subject = detailsViewModel(repository)
            val request = StreamCoreDetailsRequest(profileId = "profile-1", contentId = "content-1")

            subject.onAction(DetailsAction.Load(request))
            subject.onAction(DetailsAction.Load(request))
            runCurrent()

            assertEquals(content, subject.uiState.value.content)
            assertEquals(recommendations, subject.uiState.value.recommendations)
            assertFalse(subject.uiState.value.isLoading)
            assertEquals(1, repository.detailsRequestCount)
        }
    }

    @Test
    fun `refresh repeats active request`() {
        runTest {
            val repository = FakeDetailsRepository(
                detailsResult = StreamCoreResult.Success(contentModel("content-1")),
                recommendationsResult = StreamCoreResult.Success(emptyList()),
            )
            val subject = detailsViewModel(repository)

            subject.onAction(
                DetailsAction.Load(
                    StreamCoreDetailsRequest(profileId = "profile-1", contentId = "content-1"),
                ),
            )
            runCurrent()
            subject.onAction(DetailsAction.Refresh)
            runCurrent()

            assertEquals(2, repository.detailsRequestCount)
        }
    }

    @Test
    fun `load failure emits error`() {
        runTest {
            val error = StreamCoreError.Network()
            val subject = detailsViewModel(
                FakeDetailsRepository(
                    detailsResult = StreamCoreResult.Failure(error),
                    recommendationsResult = StreamCoreResult.Success(emptyList()),
                ),
            )
            val effect = async { subject.effects.first() }
            runCurrent()

            subject.onAction(
                DetailsAction.Load(
                    StreamCoreDetailsRequest(profileId = "profile-1", contentId = "content-1"),
                ),
            )
            runCurrent()

            assertEquals(DetailsEffect.ShowError(error), effect.await())
            assertFalse(subject.uiState.value.isLoading)
        }
    }

    @Test
    fun `recommendation selection emits navigation effect`() {
        runTest {
            val recommendation = contentModel("content-2")
            val subject = detailsViewModel()

            subject.onAction(DetailsAction.RecommendationSelected(recommendation))

            assertEquals(
                DetailsEffect.RecommendationSelected(recommendation),
                subject.effects.first(),
            )
        }
    }

    @Test
    fun `play selection uses initial content before backend load completes`() {
        runTest {
            val content = contentModel("content-1")
            val subject = detailsViewModel(
                repository = FakeDetailsRepository(
                    detailsResult = StreamCoreResult.Success(content),
                    recommendationsResult = StreamCoreResult.Success(emptyList()),
                ),
            )
            subject.onAction(
                DetailsAction.Load(
                    request = StreamCoreDetailsRequest(profileId = "profile-1", contentId = content.id),
                    initialContent = content,
                ),
            )
            assertEquals(content, subject.uiState.value.content)

            subject.onAction(DetailsAction.PlaySelected)
            runCurrent()

            assertEquals(
                DetailsEffect.PlaySelected(
                    com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest(
                        profileId = "profile-1",
                        contentId = content.id,
                        contentSnapshot = content,
                    ),
                ),
                subject.effects.first(),
            )
        }
    }

    @Test
    fun `resumable progress changes details CTA state`() {
        runTest {
            val content = contentModel("content-1")
            val progress = MutableStateFlow(emptyList<StreamCorePlaybackProgressEntry>())
            val subject = detailsViewModel(
                playback = FlowPlaybackService(progress),
            )

            subject.onAction(
                DetailsAction.Load(
                    StreamCoreDetailsRequest(profileId = "profile-1", contentId = content.id),
                ),
            )
            runCurrent()
            progress.value = listOf(
                StreamCorePlaybackProgressEntry(
                    profileId = "profile-1",
                    contentId = content.id,
                    contentSnapshot = content,
                    positionMillis = 31_000L,
                    durationMillis = 120_000L,
                    updatedAtMillis = 1L,
                ),
            )
            runCurrent()

            assertTrue(subject.uiState.value.hasResumableProgress)
        }
    }

    @Test
    fun `library membership is observed for active content`() {
        runTest {
            val content = contentModel("content-1")
            val libraryRepository = FakeLibraryRepository(
                initialEntries = listOf(
                    StreamCoreLibraryEntry(
                        content = content,
                        likedAtMillis = 1L,
                        addedToMyListAtMillis = 2L,
                    ),
                ),
            )
            val subject = detailsViewModel(libraryRepository = libraryRepository)

            subject.onAction(
                DetailsAction.Load(
                    StreamCoreDetailsRequest(profileId = "profile-1", contentId = content.id),
                ),
            )
            runCurrent()

            assertTrue(subject.uiState.value.isLibraryAvailable)
            assertTrue(subject.uiState.value.isLiked)
            assertTrue(subject.uiState.value.isInMyList)
        }
    }

    @Test
    fun `like toggle updates optimistically and completes`() {
        runTest {
            val content = contentModel("content-1")
            val mutationGate = CompletableDeferred<Unit>()
            val libraryRepository = FakeLibraryRepository(likeGate = mutationGate)
            val subject = detailsViewModel(libraryRepository = libraryRepository)
            subject.onAction(
                DetailsAction.Load(
                    StreamCoreDetailsRequest(profileId = "profile-1", contentId = content.id),
                ),
            )
            runCurrent()

            subject.onAction(DetailsAction.LikeToggled)

            assertTrue(subject.uiState.value.isLiked)
            assertTrue(subject.uiState.value.isLikeMutationPending)
            mutationGate.complete(Unit)
            runCurrent()
            assertTrue(subject.uiState.value.isLiked)
            assertFalse(subject.uiState.value.isLikeMutationPending)
            assertEquals(1, libraryRepository.likeMutationCount)
        }
    }

    @Test
    fun `failed like toggle rolls back and emits error`() {
        runTest {
            val error = StreamCoreError.Unknown()
            val content = contentModel("content-1")
            val libraryRepository = FakeLibraryRepository(
                likeResult = StreamCoreResult.Failure(error),
                likeGate = CompletableDeferred(),
            )
            val subject = detailsViewModel(libraryRepository = libraryRepository)
            subject.onAction(
                DetailsAction.Load(
                    StreamCoreDetailsRequest(profileId = "profile-1", contentId = content.id),
                ),
            )
            runCurrent()
            val effect = async { subject.effects.first() }
            runCurrent()

            subject.onAction(DetailsAction.LikeToggled)
            assertTrue(subject.uiState.value.isLiked)
            libraryRepository.completeLikeMutation()
            runCurrent()

            assertFalse(subject.uiState.value.isLiked)
            assertFalse(subject.uiState.value.isLikeMutationPending)
            assertEquals(DetailsEffect.ShowError(error), effect.await())
        }
    }

    private fun detailsViewModel(
        repository: DetailsService = FakeDetailsRepository(
            detailsResult = StreamCoreResult.Success(contentModel("content-1")),
            recommendationsResult = StreamCoreResult.Success(emptyList()),
        ),
        playback: PlaybackService = EmptyPlaybackService,
        libraryRepository: LibraryService = FakeLibraryRepository(),
    ): DetailsViewModel {
        return DetailsViewModel(
            detailsRepository = repository,
            playback = playback,
            library = libraryRepository,
        )
    }

    private fun contentModel(id: String): StreamCoreContent {
        return StreamCoreContent(
            id = id,
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

    private class FakeDetailsRepository(
        private val detailsResult: StreamCoreResult<StreamCoreContent>,
        private val recommendationsResult: StreamCoreResult<List<StreamCoreContent>>,
    ) : DetailsService {

        var detailsRequestCount: Int = 0
            private set

        override suspend fun getDetails(
            profileId: String,
            contentId: String,
        ): StreamCoreResult<StreamCoreContent> {
            detailsRequestCount += 1
            return detailsResult
        }

        override suspend fun getRecommendations(
            profileId: String,
            contentId: String,
        ): StreamCoreResult<List<StreamCoreContent>> {
            return recommendationsResult
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

    private class FakeLibraryRepository(
        initialEntries: List<StreamCoreLibraryEntry> = emptyList(),
        private val likeResult: StreamCoreResult<Unit> = StreamCoreResult.Success(Unit),
        private val myListResult: StreamCoreResult<Unit> = StreamCoreResult.Success(Unit),
        private val likeGate: CompletableDeferred<Unit>? = null,
    ) : LibraryService {
        private val entries = MutableStateFlow<StreamCoreResult<List<StreamCoreLibraryEntry>>>(
            StreamCoreResult.Success(initialEntries),
        )

        var likeMutationCount: Int = 0
            private set

        override fun observe(profileId: String): Flow<StreamCoreResult<StreamCoreLibrary>> {
            error("Details consumes membership state only")
        }

        override fun observeContentState(profileId: String, contentId: String): Flow<StreamCoreResult<StreamCoreContentLibraryState>> {
            return entries.map { result ->
                when (result) {
                    is StreamCoreResult.Failure -> result
                    is StreamCoreResult.Success -> {
                        val entry = result.value.find { it.content.id == contentId }
                        StreamCoreResult.Success(StreamCoreContentLibraryState(entry?.likedAtMillis != null, entry?.addedToMyListAtMillis != null))
                    }
                }
            }
        }

        override suspend fun setLiked(
            profileId: String,
            content: StreamCoreContent,
            isLiked: Boolean,
        ): StreamCoreResult<Unit> {
            likeMutationCount += 1
            likeGate?.await()
            if (likeResult is StreamCoreResult.Success) {
                updateEntry(content) { entry ->
                    entry.copy(likedAtMillis = 1L.takeIf { isLiked })
                }
            }
            return likeResult
        }

        override suspend fun setInMyList(
            profileId: String,
            content: StreamCoreContent,
            isInMyList: Boolean,
        ): StreamCoreResult<Unit> {
            if (myListResult is StreamCoreResult.Success) {
                updateEntry(content) { entry ->
                    entry.copy(addedToMyListAtMillis = 1L.takeIf { isInMyList })
                }
            }
            return myListResult
        }

        fun completeLikeMutation() {
            likeGate?.complete(Unit)
        }

        private fun updateEntry(
            content: StreamCoreContent,
            transform: (StreamCoreLibraryEntry) -> StreamCoreLibraryEntry,
        ) {
            val current = (entries.value as? StreamCoreResult.Success)?.value.orEmpty()
            val existing = current.firstOrNull { entry -> entry.content.id == content.id }
                ?: StreamCoreLibraryEntry(content = content)
            val updated = transform(existing)
            val retained = current.filterNot { entry -> entry.content.id == content.id }
            entries.value = StreamCoreResult.Success(
                if (updated.likedAtMillis == null && updated.addedToMyListAtMillis == null) {
                    retained
                } else {
                    retained + updated
                },
            )
        }
    }
}
