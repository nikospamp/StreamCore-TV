package com.pampoukidis.streamcoretv.feature.library.domain

import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.library.LibraryEntryModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressEntryModel
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryUseCasesTest {

    @Test
    fun `observe library creates independently ordered rows with playback progress`() = runTest {
        val shared = content("shared")
        val libraryRepository = FakeLibraryRepository(
            AppResult.Success(
                listOf(
                    LibraryEntryModel(
                        content = shared,
                        likedAtMillis = 10L,
                        addedToMyListAtMillis = 30L,
                    ),
                    LibraryEntryModel(
                        content = content("liked-newer"),
                        likedAtMillis = 20L,
                    ),
                    LibraryEntryModel(
                        content = content("list-older"),
                        addedToMyListAtMillis = 5L,
                    ),
                ),
            ),
        )
        val progress = PlaybackProgressEntryModel(
            profileId = "profile",
            contentId = "watching",
            contentSnapshot = content("watching"),
            positionMillis = 40_000L,
            durationMillis = 100_000L,
            updatedAtMillis = 100L,
        )
        val subject = ObserveLibraryUseCase(
            libraryRepository = libraryRepository,
            playbackProgressRepository = FakePlaybackProgressRepository(listOf(progress)),
        )

        val result = subject("profile").first() as AppResult.Success
        val library = result.value

        assertEquals(listOf("liked-newer", "shared"), library.likedContent.map { it.id })
        assertEquals(listOf("shared", "list-older"), library.myListContent.map { it.id })
        assertEquals("library:liked", library.likedContent.first().row)
        assertEquals("library:my-list", library.myListContent.first().row)
        assertEquals("library:continue-watching", library.continueWatching.single().row)
        assertEquals(40_000L, library.continueWatching.single().playbackProgress?.positionMillis)
        assertNull(library.likedContent.first().playbackProgress)
    }

    @Test
    fun `observe library forwards persistence failure`() = runTest {
        val error = AppError.Parsing()
        val subject = ObserveLibraryUseCase(
            libraryRepository = FakeLibraryRepository(AppResult.Failure(error)),
            playbackProgressRepository = FakePlaybackProgressRepository(emptyList()),
        )

        val result = subject("profile").first()

        assertSame(error, (result as AppResult.Failure).error)
    }

    @Test
    fun `observe content state reacts to both independent memberships`() = runTest {
        val repository = FakeLibraryRepository(AppResult.Success(emptyList()))
        val subject = ObserveContentLibraryStateUseCase(repository)

        repository.entries.value = AppResult.Success(
            listOf(
                LibraryEntryModel(
                    content = content("content"),
                    likedAtMillis = 1L,
                    addedToMyListAtMillis = null,
                ),
            ),
        )
        val likedOnly = (subject("profile", "content").first() as AppResult.Success).value
        assertTrue(likedOnly.isLiked)
        assertFalse(likedOnly.isInMyList)

        repository.entries.value = AppResult.Success(
            listOf(
                LibraryEntryModel(
                    content = content("content"),
                    likedAtMillis = 1L,
                    addedToMyListAtMillis = 2L,
                ),
            ),
        )
        val both = (subject("profile", "content").first() as AppResult.Success).value
        assertTrue(both.isLiked)
        assertTrue(both.isInMyList)
    }

    @Test
    fun `set use cases forward desired state and deterministic timestamp`() = runTest {
        val repository = FakeLibraryRepository(AppResult.Success(emptyList()))
        val content = content("content")

        SetContentLikedUseCase(repository)("profile", content, true, changedAtMillis = 10L)
        assertEquals(Mutation("profile", "content", true, 10L), repository.likedMutation)

        SetContentInMyListUseCase(repository)("profile", content, false, changedAtMillis = 20L)
        assertEquals(Mutation("profile", "content", false, 20L), repository.myListMutation)
    }

    private fun content(id: String): ContentModel {
        return ContentModel(
            id = id,
            title = id,
            description = "",
            rating = 0,
            pgRatingName = "",
            pgRatingLevel = 0,
            poster = "",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
        )
    }

    private class FakeLibraryRepository(
        initial: AppResult<List<LibraryEntryModel>>,
    ) : LibraryRepository {
        val entries = MutableStateFlow(initial)
        var likedMutation: Mutation? = null
        var myListMutation: Mutation? = null

        override fun observe(profileId: String): Flow<AppResult<List<LibraryEntryModel>>> {
            return entries
        }

        override suspend fun setLiked(
            profileId: String,
            content: ContentModel,
            isLiked: Boolean,
            changedAtMillis: Long,
        ): AppResult<Unit> {
            likedMutation = Mutation(profileId, content.id, isLiked, changedAtMillis)
            return AppResult.Success(Unit)
        }

        override suspend fun setInMyList(
            profileId: String,
            content: ContentModel,
            isInMyList: Boolean,
            changedAtMillis: Long,
        ): AppResult<Unit> {
            myListMutation = Mutation(profileId, content.id, isInMyList, changedAtMillis)
            return AppResult.Success(Unit)
        }
    }

    private class FakePlaybackProgressRepository(
        private val entries: List<PlaybackProgressEntryModel>,
    ) : PlaybackProgressRepository {
        override fun observe(profileId: String): Flow<List<PlaybackProgressEntryModel>> {
            return MutableStateFlow(entries)
        }

        override suspend fun get(
            profileId: String,
            contentId: String,
        ): PlaybackProgressEntryModel? {
            return entries.firstOrNull { entry -> entry.contentId == contentId }
        }

        override suspend fun upsert(entry: PlaybackProgressEntryModel) {
            return Unit
        }

        override suspend fun remove(profileId: String, contentId: String) {
            return Unit
        }
    }

    private data class Mutation(
        val profileId: String,
        val contentId: String,
        val value: Boolean,
        val changedAtMillis: Long,
    )
}
