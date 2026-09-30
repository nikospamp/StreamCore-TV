package com.pampoukidis.streamcore.sdk.runtime

import com.pampoukidis.streamcore.sdk.runtime.storage.library.LibraryStore
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibrary
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibraryEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RuntimeLibraryServiceTest {

    @Test
    fun `observe library creates independently ordered rows with playback progress`() = runTest {
        val shared = content("shared")
        val libraryRepository = FakeLibraryRepository(
            StreamCoreResult.Success(
                listOf(
                    StreamCoreLibraryEntry(
                        content = shared,
                        likedAtMillis = 10L,
                        addedToMyListAtMillis = 30L,
                    ),
                    StreamCoreLibraryEntry(
                        content = content("liked-newer"),
                        likedAtMillis = 20L,
                    ),
                    StreamCoreLibraryEntry(
                        content = content("list-older"),
                        addedToMyListAtMillis = 5L,
                    ),
                ),
            ),
        )
        val progress = StreamCorePlaybackProgressEntry(
            profileId = "profile",
            contentId = "watching",
            contentSnapshot = content("watching"),
            positionMillis = 40_000L,
            durationMillis = 100_000L,
            updatedAtMillis = 100L,
        )
        val subject = RuntimeLibraryService(
            library = libraryRepository,
            progress = FakePlaybackProgressRepository(listOf(progress)),
        )

        val result = subject.observe("profile").first() as StreamCoreResult.Success
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
        val error = StreamCoreError.Parsing()
        val subject = RuntimeLibraryService(
            library = FakeLibraryRepository(StreamCoreResult.Failure(error)),
            progress = FakePlaybackProgressRepository(emptyList()),
        )

        val result = subject.observe("profile").first()

        assertSame(error, (result as StreamCoreResult.Failure).error)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun libraryFailureTakesPrecedenceAndCollectionRecoversAfterBothSources(): TestResult {
        return runTest {
            val libraryFailure = StreamCoreResult.Failure(StreamCoreError.Parsing())
            val progressFailure = StreamCoreResult.Failure(StreamCoreError.Storage())
            val libraryRepository = FakeLibraryRepository(libraryFailure)
            val progressOperations = FakePlaybackProgressRepository(emptyList())
            progressOperations.observedEntries.value = progressFailure
            val subject = RuntimeLibraryService(libraryRepository, progressOperations)
            val emissions = mutableListOf<StreamCoreResult<StreamCoreLibrary>>()

            backgroundScope.launch {
                subject.observe("profile").toList(emissions)
            }
            runCurrent()
            assertEquals(listOf<StreamCoreResult<StreamCoreLibrary>>(libraryFailure), emissions)

            libraryRepository.entries.value = StreamCoreResult.Success(emptyList())
            runCurrent()
            assertEquals(listOf<StreamCoreResult<StreamCoreLibrary>>(libraryFailure, progressFailure), emissions)

            progressOperations.observedEntries.value = StreamCoreResult.Success(emptyList())
            runCurrent()
            assertEquals(
                listOf(libraryFailure, progressFailure, StreamCoreResult.Success(StreamCoreLibrary())),
                emissions,
            )
        }
    }

    @Test
    fun `observe content state reacts to both independent memberships`() = runTest {
        val repository = FakeLibraryRepository(StreamCoreResult.Success(emptyList()))
        val subject = RuntimeLibraryService(repository, FakePlaybackProgressRepository(emptyList()))

        repository.entries.value = StreamCoreResult.Success(
            listOf(
                StreamCoreLibraryEntry(
                    content = content("content"),
                    likedAtMillis = 1L,
                    addedToMyListAtMillis = null,
                ),
            ),
        )
        val likedOnly = (subject.observeContentState("profile", "content").first() as StreamCoreResult.Success).value
        assertTrue(likedOnly.isLiked)
        assertFalse(likedOnly.isInMyList)

        repository.entries.value = StreamCoreResult.Success(
            listOf(
                StreamCoreLibraryEntry(
                    content = content("content"),
                    likedAtMillis = 1L,
                    addedToMyListAtMillis = 2L,
                ),
            ),
        )
        val both = (subject.observeContentState("profile", "content").first() as StreamCoreResult.Success).value
        assertTrue(both.isLiked)
        assertTrue(both.isInMyList)
    }

    @Test
    fun `mutations own their epoch timestamp`() = runTest {
        val repository = FakeLibraryRepository(StreamCoreResult.Success(emptyList()))
        val service = RuntimeLibraryService(repository, FakePlaybackProgressRepository(emptyList()), nowMillis = { 1_725_000_000_123L })
        service.setLiked("profile", content("content"), true)
        service.setInMyList("profile", content("content"), false)
        assertEquals(Mutation("profile", "content", true, 1_725_000_000_123L), repository.likedMutation)
        assertEquals(Mutation("profile", "content", false, 1_725_000_000_123L), repository.myListMutation)
    }

    private fun content(id: String): StreamCoreContent {
        return StreamCoreContent(
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
        initial: StreamCoreResult<List<StreamCoreLibraryEntry>>,
    ) : LibraryStore {
        val entries = MutableStateFlow(initial)
        var likedMutation: Mutation? = null
        var myListMutation: Mutation? = null

        override fun observe(profileId: String): Flow<StreamCoreResult<List<StreamCoreLibraryEntry>>> {
            return entries
        }

        override suspend fun setLiked(
            profileId: String,
            content: StreamCoreContent,
            isLiked: Boolean,
            changedAtMillis: Long,
        ): StreamCoreResult<Unit> {
            likedMutation = Mutation(profileId, content.id, isLiked, changedAtMillis)
            return StreamCoreResult.Success(Unit)
        }

        override suspend fun setInMyList(
            profileId: String,
            content: StreamCoreContent,
            isInMyList: Boolean,
            changedAtMillis: Long,
        ): StreamCoreResult<Unit> {
            myListMutation = Mutation(profileId, content.id, isInMyList, changedAtMillis)
            return StreamCoreResult.Success(Unit)
        }
    }

    private class FakePlaybackProgressRepository(
        entries: List<StreamCorePlaybackProgressEntry>,
    ) : PlaybackProgressOperations {
        val observedEntries = MutableStateFlow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>>(
            StreamCoreResult.Success(entries),
        )

        override fun observeProgress(profileId: String): Flow<StreamCoreResult<List<StreamCorePlaybackProgressEntry>>> {
            return observedEntries
        }

        override suspend fun updateProgress(entry: StreamCorePlaybackProgressEntry): StreamCoreResult<Unit> {
            return StreamCoreResult.Success(Unit)
        }

        override suspend fun removeProgress(profileId: String, contentId: String): StreamCoreResult<Unit> {
            return StreamCoreResult.Success(Unit)
        }
    }

    private data class Mutation(
        val profileId: String,
        val contentId: String,
        val value: Boolean,
        val changedAtMillis: Long,
    )

}
