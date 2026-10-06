package com.pampoukidis.streamcore.sdk.runtime.library

import com.pampoukidis.streamcore.sdk.runtime.storage.library.LibraryStore
import com.pampoukidis.streamcore.sdk.runtime.storage.accountStorageKey
import com.pampoukidis.streamcore.sdk.runtime.playback.RuntimePlaybackService
import com.pampoukidis.streamcore.sdk.runtime.storage.playback.PlaybackProgressStore
import com.pampoukidis.streamcore.sdk.runtime.session.AccountSession
import com.pampoukidis.streamcore.sdk.runtime.session.AuthorizedProfile
import com.pampoukidis.streamcore.sdk.runtime.session.ClientSessionState
import com.pampoukidis.streamcore.sdk.runtime.session.ProfileSelectionAttempt
import com.pampoukidis.streamcore.sdk.runtime.session.RuntimeSession
import com.pampoukidis.streamcore.sdk.runtime.auth.AuthProvider
import com.pampoukidis.streamcore.sdk.runtime.content.ContentPolicyProvider
import com.pampoukidis.streamcore.sdk.runtime.details.DetailsProvider
import com.pampoukidis.streamcore.sdk.runtime.error.ProviderOperationException
import com.pampoukidis.streamcore.sdk.runtime.home.HomeProvider
import com.pampoukidis.streamcore.sdk.runtime.playback.PlaybackProvider
import com.pampoukidis.streamcore.sdk.runtime.profile.ProfileProvider
import com.pampoukidis.streamcore.sdk.runtime.search.SearchProvider
import com.pampoukidis.streamcore.sdk.runtime.session.ProviderSessionServices
import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibrary
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibraryEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackMedia
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import com.pampoukidis.streamcore.sdk.model.profile.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
import kotlin.test.assertIs

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
        val fixture = LibraryFixture(
            library = libraryRepository,
            progress = FakePlaybackProgressStore(listOf(progress)),
        )

        val result = fixture.service.observe("profile").first() as StreamCoreResult.Success
        val library = result.value

        assertEquals(fixture.physicalProfileId, libraryRepository.observedProfileId)
        assertEquals(fixture.physicalProfileId, fixture.progress.observedProfileId)
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
        val subject = LibraryFixture(
            library = FakeLibraryRepository(StreamCoreResult.Failure(error)),
            progress = FakePlaybackProgressStore(emptyList()),
        ).service

        val result = subject.observe("profile").first()

        assertSame(error, (result as StreamCoreResult.Failure).error)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun libraryFailureTakesPrecedenceAndStorageRecoveryRequiresFreshObservation(): TestResult {
        return runTest {
            val libraryFailure = StreamCoreResult.Failure(StreamCoreError.Parsing())
            val progressFailure = StreamCoreResult.Failure(StreamCoreError.Storage())
            val libraryRepository = FakeLibraryRepository(libraryFailure)
            val progressStore = FakePlaybackProgressStore(emptyList())
            progressStore.failure = ProviderOperationException(progressFailure.error)
            val fixture = LibraryFixture(libraryRepository, progressStore)
            val subject = fixture.service
            val emissions = mutableListOf<StreamCoreResult<StreamCoreLibrary>>()

            backgroundScope.launch {
                subject.observe("profile").toList(emissions)
            }
            runCurrent()
            assertEquals(listOf<StreamCoreResult<StreamCoreLibrary>>(libraryFailure), emissions)

            libraryRepository.entries.value = StreamCoreResult.Success(emptyList())
            runCurrent()
            assertEquals(listOf<StreamCoreResult<StreamCoreLibrary>>(libraryFailure, progressFailure), emissions)

            progressStore.failure = null
            progressStore.entries.value = listOf(
                StreamCorePlaybackProgressEntry(
                    fixture.physicalProfileId, "recovered", content("recovered"),
                    40_000L, 100_000L, 100L,
                ),
            )
            runCurrent()
            // A thrown storage failure terminates that inner store collection. Clearing the
            // failure cannot emit recovery into it; a new public observation resubscribes.
            assertEquals(
                listOf<StreamCoreResult<StreamCoreLibrary>>(libraryFailure, progressFailure),
                emissions,
            )
            val recovered = assertIs<StreamCoreResult.Success<StreamCoreLibrary>>(subject.observe("profile").first()).value
            assertEquals(listOf("recovered"), recovered.continueWatching.map { it.id })
            assertEquals(40_000L, recovered.continueWatching.single().playbackProgress?.positionMillis)
        }
    }

    @Test
    fun `observe content state reacts to both independent memberships`() = runTest {
        val repository = FakeLibraryRepository(StreamCoreResult.Success(emptyList()))
        val subject = LibraryFixture(repository, FakePlaybackProgressStore(emptyList())).service

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
        var clockReads = 0
        val fixture = LibraryFixture(repository, FakePlaybackProgressStore(emptyList()), nowMillis = {
            clockReads += 1
            1_725_000_000_123L
        })
        val service = fixture.service
        service.setLiked("profile", content("content"), true)
        service.setInMyList("profile", content("content"), false)
        assertEquals(Mutation(fixture.physicalProfileId, "content", true, 1_725_000_000_123L), repository.likedMutation)
        assertEquals(Mutation(fixture.physicalProfileId, "content", false, 1_725_000_000_123L), repository.myListMutation)
        assertEquals(2, clockReads)

        val rejected = assertIs<StreamCoreResult.Failure>(service.setLiked("other", content("rejected"), true))
        assertIs<StreamCoreError.InvalidContext>(rejected.error)
        assertEquals(3, clockReads)
        assertEquals(Mutation(fixture.physicalProfileId, "content", true, 1_725_000_000_123L), repository.likedMutation)
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
        var observedProfileId: String? = null
        var likedMutation: Mutation? = null
        var myListMutation: Mutation? = null

        override fun observe(profileId: String): Flow<StreamCoreResult<List<StreamCoreLibraryEntry>>> {
            observedProfileId = profileId
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

    private class FakePlaybackProgressStore(
        initial: List<StreamCorePlaybackProgressEntry>,
    ) : PlaybackProgressStore {
        val entries = MutableStateFlow(initial)
        var observedProfileId: String? = null
        var failure: Exception? = null

        override fun observe(profileId: String): Flow<List<StreamCorePlaybackProgressEntry>> {
            return flow {
                observedProfileId = profileId
                failure?.let { throw it }
                emitAll(entries.map { values -> values.filter { it.profileId == profileId } })
            }
        }

        override suspend fun get(profileId: String, contentId: String): StreamCorePlaybackProgressEntry? {
            return observe(profileId).first().find { it.contentId == contentId }
        }

        override suspend fun upsert(entry: StreamCorePlaybackProgressEntry) {
            failure?.let { throw it }
            entries.value = entries.value.filterNot { it.profileId == entry.profileId && it.contentId == entry.contentId } + entry
        }

        override suspend fun remove(profileId: String, contentId: String) {
            failure?.let { throw it }
            entries.value = entries.value.filterNot { it.profileId == profileId && it.contentId == contentId }
        }
    }

    private data class Mutation(
        val profileId: String,
        val contentId: String,
        val value: Boolean,
        val changedAtMillis: Long,
    )

    private class LibraryFixture(
        library: LibraryStore,
        val progress: FakePlaybackProgressStore,
        nowMillis: () -> Long = { 1_725_000_000_123L },
    ) {
        private val configuration = StreamCoreConfiguration("test-backend", "library-test")
        private val accountSession = AccountSession(
            TestProviders.account,
            ProviderSessionServices(
                profiles = TestProviders,
                home = TestProviders,
                details = TestProviders,
                search = TestProviders,
                playback = TestProviders,
                contentPolicy = TestProviders,
            ),
        )
        private val selection = ProfileSelectionAttempt(accountSession, 1L)
        val authorization = AuthorizedProfile("test-authorization", selection, TestProviders.profile, null)
        private val runtimeSession = RuntimeSession(configuration, TestProviders, {})
        val physicalProfileId = accountStorageKey(configuration, TestProviders.account.id, TestProviders.profile.id)

        init {
            progress.entries.value = progress.entries.value.map { it.copy(profileId = physicalProfileId) }
            runtimeSession.state.value = ClientSessionState(
                accountSession = accountSession,
                authorizedProfile = authorization,
                profileSelection = selection,
                selectionVersion = 1L,
                isAuthInitialized = true,
            )
        }

        val service = RuntimeLibraryService(
            runtimeSession = runtimeSession,
            capabilities = StreamCoreCapabilities(),
            library = library,
            playback = RuntimePlaybackService(runtimeSession, StreamCoreCapabilities(), progress),
            nowMillis = nowMillis,
        )
    }

    private object TestProviders : AuthProvider, ProfileProvider, HomeProvider, DetailsProvider,
        SearchProvider, PlaybackProvider, ContentPolicyProvider {
        val account = StreamCoreAuthAccount("account", "Account", null)
        val profile = StreamCoreProfile(
            "profile", "Primary", StreamCoreProfileAvatar("avatar", null),
            StreamCoreProfileParentalLevel("adult", "All", 0), true, false,
        )
        override val authState = MutableStateFlow<StreamCoreAuthState>(StreamCoreAuthState.LoggedIn(account))

        override suspend fun restoreSession(): StreamCoreResult<StreamCoreAuthState> {
            return StreamCoreResult.Success(authState.value)
        }
        override suspend fun login(identifier: String, password: String): StreamCoreResult<Unit> {
            return unsupported()
        }
        override suspend fun loginWithQr(qrCode: String): StreamCoreResult<Unit> { return unsupported() }
        override suspend fun logout(): StreamCoreResult<Unit> { return unsupported() }
        override suspend fun recoverPassword(email: String, otp: String?): StreamCoreResult<Unit> { return unsupported() }
        override suspend fun invalidateSession() {}
        override suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>> {
            return StreamCoreResult.Success(listOf(profile))
        }
        override suspend fun getProfileEditorOptions(): StreamCoreResult<StreamCoreProfileEditorOptions> { return unsupported() }
        override suspend fun createProfile(profile: StreamCoreCreateProfile): StreamCoreResult<StreamCoreProfile> { return unsupported() }
        override suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile> { return unsupported() }
        override suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit> { return unsupported() }
        override suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfile> { return unsupported() }
        override suspend fun verifyProfilePin(profileId: String, pin: String): StreamCoreResult<Unit> { return unsupported() }
        override suspend fun getCollections(profileId: String): StreamCoreResult<List<StreamCoreCollection>> { return unsupported() }
        override suspend fun getDetails(profileId: String, contentId: String): StreamCoreResult<StreamCoreContent> { return unsupported() }
        override suspend fun getRecommendations(profileId: String, contentId: String): StreamCoreResult<List<StreamCoreContent>> { return unsupported() }
        override suspend fun search(profileId: String, query: String): StreamCoreResult<List<StreamCoreContent>> { return unsupported() }
        override suspend fun loadTrending(profileId: String): StreamCoreResult<List<StreamCoreContent>> { return unsupported() }
        override suspend fun resolveSource(request: StreamCorePlaybackRequest): StreamCorePlaybackMedia {
            throw ProviderOperationException(StreamCoreError.Unsupported("unused-test-provider"))
        }
        override suspend fun isContentAllowed(profile: StreamCoreProfile, content: StreamCoreContent): Boolean { return true }

        private fun unsupported(): StreamCoreResult.Failure {
            return StreamCoreResult.Failure(StreamCoreError.Unsupported("unused-test-provider"))
        }
    }
}
