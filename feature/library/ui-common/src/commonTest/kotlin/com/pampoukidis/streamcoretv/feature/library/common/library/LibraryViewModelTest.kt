package com.pampoukidis.streamcoretv.feature.library.common.library

import com.pampoukidis.streamcore.sdk.api.LibraryService
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibrary
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreContentLibraryState
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
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
            val repository = FakeLibraryService(
                StreamCoreResult.Success(StreamCoreLibrary(
                    continueWatching = listOf(watching),
                    likedContent = listOf(liked),
                    myListContent = listOf(listed),
                )),
            )
            val subject = LibraryViewModel(repository)

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
            val repository = FakeLibraryService(
                StreamCoreResult.Success(StreamCoreLibrary(likedContent = listOf(content("liked")))),
            )
            val subject = LibraryViewModel(repository)
            subject.onAction(LibraryAction.Load("profile-1"))
            runCurrent()

            repository.result.value = StreamCoreResult.Failure(StreamCoreError.Unknown())
            runCurrent()

            assertEquals(listOf("liked"), subject.uiState.value.likedContent.map { it.id })
            assertNotNull(subject.uiState.value.error)
        }
    }

    @Test
    fun `content selection emits one navigation effect`() {
        runTest {
            val selected = content("selected")
            val subject = LibraryViewModel(FakeLibraryService(StreamCoreResult.Success(StreamCoreLibrary())))

            subject.onAction(LibraryAction.ContentSelected(selected))

            assertEquals(LibraryEffect.ContentSelected(selected), subject.effects.first())
        }
    }

    private fun content(id: String): StreamCoreContent {
        return StreamCoreContent(
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

    private class FakeLibraryService(initialResult: StreamCoreResult<StreamCoreLibrary>) : LibraryService {
        val result = MutableStateFlow(initialResult)

        override fun observe(profileId: String): Flow<StreamCoreResult<StreamCoreLibrary>> {
            return result
        }

        override fun observeContentState(profileId: String, contentId: String): Flow<StreamCoreResult<StreamCoreContentLibraryState>> {
            error("Not used by library screen")
        }

        override suspend fun setLiked(profileId: String, content: StreamCoreContent, isLiked: Boolean): StreamCoreResult<Unit> {
            error("Not used by library screen")
        }

        override suspend fun setInMyList(profileId: String, content: StreamCoreContent, isInMyList: Boolean): StreamCoreResult<Unit> {
            error("Not used by library screen")
        }
    }
}
