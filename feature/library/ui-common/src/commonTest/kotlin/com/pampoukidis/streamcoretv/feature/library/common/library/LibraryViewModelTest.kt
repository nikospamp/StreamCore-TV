package com.pampoukidis.streamcoretv.feature.library.common.library

import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.library.LibraryEntryModel
import com.pampoukidis.streamcoretv.feature.library.domain.ObserveLibraryUseCase
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressEntryModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

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
    fun `load publishes all three library collections`() {
        runTest {
            val liked = content("liked")
            val listed = content("listed")
            val watching = content("watching")
            val repository = FakeLibraryRepository(
                AppResult.Success(
                    listOf(
                        LibraryEntryModel(content = liked, likedAtMillis = 2L),
                        LibraryEntryModel(content = listed, addedToMyListAtMillis = 3L),
                    ),
                ),
            )
            val progressRepository = FakeProgressRepository(
                listOf(
                    PlaybackProgressEntryModel(
                        profileId = "profile-1",
                        contentId = watching.id,
                        contentSnapshot = watching,
                        positionMillis = 40_000L,
                        durationMillis = 100_000L,
                        updatedAtMillis = 4L,
                    ),
                ),
            )
            val subject = LibraryViewModel(
                ObserveLibraryUseCase(repository, progressRepository),
            )

            subject.onAction(LibraryAction.Load("profile-1"))
            runCurrent()

            assertFalse(subject.uiState.value.isLoading)
            assertEquals(listOf("watching"), subject.uiState.value.continueWatching.map { it.id })
            assertEquals(listOf("liked"), subject.uiState.value.likedContent.map { it.id })
            assertEquals(listOf("listed"), subject.uiState.value.myListContent.map { it.id })
        }
    }

    @Test
    fun `failure retains last content and exposes retry state`() {
        runTest {
            val repository = FakeLibraryRepository(
                AppResult.Success(
                    listOf(LibraryEntryModel(content = content("liked"), likedAtMillis = 1L)),
                ),
            )
            val subject = LibraryViewModel(
                ObserveLibraryUseCase(repository, FakeProgressRepository(emptyList())),
            )
            subject.onAction(LibraryAction.Load("profile-1"))
            runCurrent()

            repository.result.value = AppResult.Failure(AppError.Unknown())
            runCurrent()

            assertEquals(listOf("liked"), subject.uiState.value.likedContent.map { it.id })
            assertNotNull(subject.uiState.value.error)
        }
    }

    @Test
    fun `content selection emits one navigation effect`() {
        runTest {
            val selected = content("selected")
            val subject = LibraryViewModel(
                ObserveLibraryUseCase(
                    FakeLibraryRepository(AppResult.Success(emptyList())),
                    FakeProgressRepository(emptyList()),
                ),
            )

            subject.onAction(LibraryAction.ContentSelected(selected))

            assertEquals(LibraryEffect.ContentSelected(selected), subject.effects.first())
        }
    }

    private fun content(id: String): ContentModel {
        return ContentModel(
            id = id,
            title = id,
            description = "",
            rating = 8,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
        )
    }

    private class FakeLibraryRepository(
        initialResult: AppResult<List<LibraryEntryModel>>,
    ) : LibraryRepository {
        val result = MutableStateFlow(initialResult)

        override fun observe(profileId: String): Flow<AppResult<List<LibraryEntryModel>>> {
            return result
        }

        override suspend fun setLiked(
            profileId: String,
            content: ContentModel,
            isLiked: Boolean,
            changedAtMillis: Long,
        ): AppResult<Unit> {
            return AppResult.Success(Unit)
        }

        override suspend fun setInMyList(
            profileId: String,
            content: ContentModel,
            isInMyList: Boolean,
            changedAtMillis: Long,
        ): AppResult<Unit> {
            return AppResult.Success(Unit)
        }
    }

    private class FakeProgressRepository(
        entries: List<PlaybackProgressEntryModel>,
    ) : PlaybackProgressRepository {
        private val values = MutableStateFlow(entries)

        override fun observe(profileId: String): Flow<List<PlaybackProgressEntryModel>> {
            return values
        }

        override suspend fun get(
            profileId: String,
            contentId: String,
        ): PlaybackProgressEntryModel? {
            return values.value.firstOrNull { entry -> entry.contentId == contentId }
        }

        override suspend fun upsert(entry: PlaybackProgressEntryModel) = Unit

        override suspend fun remove(profileId: String, contentId: String) = Unit
    }
}
