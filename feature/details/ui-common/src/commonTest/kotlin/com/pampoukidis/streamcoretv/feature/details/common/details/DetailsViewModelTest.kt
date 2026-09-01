package com.pampoukidis.streamcoretv.feature.details.common.details

import com.pampoukidis.streamcoretv.core.domain.DetailsRepository
import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.TrailerModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.library.LibraryEntryModel
import com.pampoukidis.streamcoretv.feature.details.data.DetailsRequest
import com.pampoukidis.streamcoretv.feature.details.domain.LoadDetailsUseCase
import com.pampoukidis.streamcoretv.feature.library.domain.ObserveContentLibraryStateUseCase
import com.pampoukidis.streamcoretv.feature.library.domain.SetContentInMyListUseCase
import com.pampoukidis.streamcoretv.feature.library.domain.SetContentLikedUseCase
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressEntryModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import kotlinx.coroutines.CompletableDeferred
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
            val trailer = TrailerModel("official", "Official Trailer", "https://www.youtube.com/watch?v=abcdefghijk")
            val content = contentModel("content-1").copy(
                trailers = listOf(trailer, trailer.copy(id = "other")),
            )
            val subject = detailsViewModel(
                FakeDetailsRepository(
                    detailsResult = AppResult.Success(content),
                    recommendationsResult = AppResult.Success(emptyList()),
                ),
            )
            subject.onAction(DetailsAction.Load(DetailsRequest("profile-1", content.id)))
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
            subject.onAction(DetailsAction.Load(DetailsRequest("profile-1", "content-1")))
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
                detailsResult = AppResult.Success(content),
                recommendationsResult = AppResult.Success(recommendations),
            )
            val subject = detailsViewModel(repository)
            val request = DetailsRequest(profileId = "profile-1", contentId = "content-1")

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
                detailsResult = AppResult.Success(contentModel("content-1")),
                recommendationsResult = AppResult.Success(emptyList()),
            )
            val subject = detailsViewModel(repository)

            subject.onAction(
                DetailsAction.Load(
                    DetailsRequest(profileId = "profile-1", contentId = "content-1"),
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
            val error = AppError.Network()
            val subject = detailsViewModel(
                FakeDetailsRepository(
                    detailsResult = AppResult.Failure(error),
                    recommendationsResult = AppResult.Success(emptyList()),
                ),
            )
            val effect = async { subject.effects.first() }
            runCurrent()

            subject.onAction(
                DetailsAction.Load(
                    DetailsRequest(profileId = "profile-1", contentId = "content-1"),
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
                    detailsResult = AppResult.Success(content),
                    recommendationsResult = AppResult.Success(emptyList()),
                ),
            )
            subject.onAction(
                DetailsAction.Load(
                    request = DetailsRequest(profileId = "profile-1", contentId = content.id),
                    initialContent = content,
                ),
            )
            assertEquals(content, subject.uiState.value.content)

            subject.onAction(DetailsAction.PlaySelected)
            runCurrent()

            assertEquals(
                DetailsEffect.PlaySelected(
                    com.pampoukidis.streamcoretv.playback.api.PlaybackRequestModel(
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
            val progress = MutableStateFlow(emptyList<PlaybackProgressEntryModel>())
            val subject = detailsViewModel(
                progressRepository = FlowPlaybackProgressRepository(progress),
            )

            subject.onAction(
                DetailsAction.Load(
                    DetailsRequest(profileId = "profile-1", contentId = content.id),
                ),
            )
            runCurrent()
            progress.value = listOf(
                PlaybackProgressEntryModel(
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
                    LibraryEntryModel(
                        content = content,
                        likedAtMillis = 1L,
                        addedToMyListAtMillis = 2L,
                    ),
                ),
            )
            val subject = detailsViewModel(libraryRepository = libraryRepository)

            subject.onAction(
                DetailsAction.Load(
                    DetailsRequest(profileId = "profile-1", contentId = content.id),
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
                    DetailsRequest(profileId = "profile-1", contentId = content.id),
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
            val error = AppError.Unknown()
            val content = contentModel("content-1")
            val libraryRepository = FakeLibraryRepository(
                likeResult = AppResult.Failure(error),
                likeGate = CompletableDeferred(),
            )
            val subject = detailsViewModel(libraryRepository = libraryRepository)
            subject.onAction(
                DetailsAction.Load(
                    DetailsRequest(profileId = "profile-1", contentId = content.id),
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
        repository: DetailsRepository = FakeDetailsRepository(
            detailsResult = AppResult.Success(contentModel("content-1")),
            recommendationsResult = AppResult.Success(emptyList()),
        ),
        progressRepository: PlaybackProgressRepository = EmptyPlaybackProgressRepository,
        libraryRepository: LibraryRepository = FakeLibraryRepository(),
    ): DetailsViewModel {
        return DetailsViewModel(
            loadDetails = LoadDetailsUseCase(repository),
            progressRepository = progressRepository,
            observeContentLibraryState = ObserveContentLibraryStateUseCase(libraryRepository),
            setContentLiked = SetContentLikedUseCase(libraryRepository),
            setContentInMyList = SetContentInMyListUseCase(libraryRepository),
        )
    }

    private fun contentModel(id: String): ContentModel {
        return ContentModel(
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
        private val detailsResult: AppResult<ContentModel>,
        private val recommendationsResult: AppResult<List<ContentModel>>,
    ) : DetailsRepository {

        var detailsRequestCount: Int = 0
            private set

        override suspend fun getDetails(
            profileId: String,
            contentId: String,
        ): AppResult<ContentModel> {
            detailsRequestCount += 1
            return detailsResult
        }

        override suspend fun getRecommendations(
            profileId: String,
            contentId: String,
        ): AppResult<List<ContentModel>> {
            return recommendationsResult
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

    private class FakeLibraryRepository(
        initialEntries: List<LibraryEntryModel> = emptyList(),
        private val likeResult: AppResult<Unit> = AppResult.Success(Unit),
        private val myListResult: AppResult<Unit> = AppResult.Success(Unit),
        private val likeGate: CompletableDeferred<Unit>? = null,
    ) : LibraryRepository {
        private val entries = MutableStateFlow<AppResult<List<LibraryEntryModel>>>(
            AppResult.Success(initialEntries),
        )

        var likeMutationCount: Int = 0
            private set

        override fun observe(profileId: String): Flow<AppResult<List<LibraryEntryModel>>> {
            return entries
        }

        override suspend fun setLiked(
            profileId: String,
            content: ContentModel,
            isLiked: Boolean,
            changedAtMillis: Long,
        ): AppResult<Unit> {
            likeMutationCount += 1
            likeGate?.await()
            if (likeResult is AppResult.Success) {
                updateEntry(content) { entry ->
                    entry.copy(likedAtMillis = changedAtMillis.takeIf { isLiked })
                }
            }
            return likeResult
        }

        override suspend fun setInMyList(
            profileId: String,
            content: ContentModel,
            isInMyList: Boolean,
            changedAtMillis: Long,
        ): AppResult<Unit> {
            if (myListResult is AppResult.Success) {
                updateEntry(content) { entry ->
                    entry.copy(addedToMyListAtMillis = changedAtMillis.takeIf { isInMyList })
                }
            }
            return myListResult
        }

        fun completeLikeMutation() {
            likeGate?.complete(Unit)
        }

        private fun updateEntry(
            content: ContentModel,
            transform: (LibraryEntryModel) -> LibraryEntryModel,
        ) {
            val current = (entries.value as? AppResult.Success)?.value.orEmpty()
            val existing = current.firstOrNull { entry -> entry.content.id == content.id }
                ?: LibraryEntryModel(content = content)
            val updated = transform(existing)
            val retained = current.filterNot { entry -> entry.content.id == content.id }
            entries.value = AppResult.Success(
                if (updated.likedAtMillis == null && updated.addedToMyListAtMillis == null) {
                    retained
                } else {
                    retained + updated
                },
            )
        }
    }
}
