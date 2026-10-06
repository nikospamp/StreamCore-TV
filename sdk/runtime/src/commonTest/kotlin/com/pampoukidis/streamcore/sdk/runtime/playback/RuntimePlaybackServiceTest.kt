package com.pampoukidis.streamcore.sdk.runtime.playback

import com.pampoukidis.streamcore.sdk.model.StreamCoreCapabilities
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthState
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreCollection
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEvent
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackMedia
import com.pampoukidis.streamcore.sdk.model.profile.*
import com.pampoukidis.streamcore.sdk.runtime.auth.AuthProvider
import com.pampoukidis.streamcore.sdk.runtime.content.ContentPolicyProvider
import com.pampoukidis.streamcore.sdk.runtime.details.DetailsProvider
import com.pampoukidis.streamcore.sdk.runtime.error.ProviderOperationException
import com.pampoukidis.streamcore.sdk.runtime.home.HomeProvider
import com.pampoukidis.streamcore.sdk.runtime.profile.ProfileProvider
import com.pampoukidis.streamcore.sdk.runtime.search.SearchProvider
import com.pampoukidis.streamcore.sdk.runtime.session.AccountSession
import com.pampoukidis.streamcore.sdk.runtime.session.AuthorizedProfile
import com.pampoukidis.streamcore.sdk.runtime.session.ClientSessionState
import com.pampoukidis.streamcore.sdk.runtime.session.ProfileSelectionAttempt
import com.pampoukidis.streamcore.sdk.runtime.session.ProviderSessionServices
import com.pampoukidis.streamcore.sdk.runtime.session.RuntimeSession
import com.pampoukidis.streamcore.sdk.runtime.storage.accountStorageKey
import com.pampoukidis.streamcore.sdk.runtime.storage.playback.PlaybackProgressStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RuntimePlaybackServiceTest {
    @Test
    fun failedPeriodicAttemptKeepsBucketUntilForwardAdvanceAndCheckpointStillWrites(): TestResult {
        return runTest {
            val fixture = PlaybackFixture()
            val recorder = fixture.service.createProgressRecorder(request(), 0L)
            fixture.store.failure = ProviderOperationException(StreamCoreError.Storage())
            assertIs<StreamCoreResult.Failure>(recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 40_000L, 100_000L))

            fixture.store.failure = null
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 49_999L, 100_000L)
            assertEquals(emptyList(), fixture.store.writes)
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 35_000L, 100_000L)
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 40_001L, 100_000L)
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 50_000L, 100_000L)
            assertEquals(listOf(35_000L, 50_000L), fixture.store.writes.map { it.positionMillis })
        }
    }

    @Test
    fun periodicSamplesKeepTheEstablishedTenSecondCadenceAndResumeBucket() = runTest {
        val fixture = PlaybackFixture(nowMillis = { 123L })
        val recorder = fixture.service.createProgressRecorder(request(), 45_000L)

        listOf(45_000L, 49_999L, 50_000L, 50_001L, 59_999L, 60_000L).forEach { position ->
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, position, 100_000L)
        }

        assertEquals(listOf(50_000L, 60_000L), fixture.store.writes.map { it.positionMillis })
        assertEquals(listOf(123L, 123L), fixture.store.writes.map { it.updatedAtMillis })
        assertEquals(listOf(fixture.physicalProfileId, fixture.physicalProfileId), fixture.store.writes.map { it.profileId })
    }

    @Test
    fun explicitCheckpointsAreNotSuppressedByCadenceAndUnknownDurationPreservesData() = runTest {
        val fixture = PlaybackFixture()
        val recorder = fixture.service.createProgressRecorder(request(), 40_000L)

        recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 40_001L, 100_000L)
        recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 40_002L, 100_000L)
        recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 0L, 0L)
        recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 60_000L, 0L)

        assertEquals(listOf(40_001L, 40_002L), fixture.store.writes.map { it.positionMillis })
        assertEquals(0, fixture.store.removalAttempts)
        val saved = assertIs<StreamCoreResult.Success<StreamCorePlaybackProgressEntry?>>(
            fixture.service.getProgress("profile", "film"),
        )
        assertEquals(40_002L, saved.value?.positionMillis)
    }

    @Test
    fun completionRemovesProgressAndStorageFailureDoesNotThrowOrBlockLaterEvents() = runTest {
        val fixture = PlaybackFixture()
        val recorder = fixture.service.createProgressRecorder(request(), 0L)
        fixture.store.failure = ProviderOperationException(StreamCoreError.Storage())

        assertIs<StreamCoreError.Storage>(assertIs<StreamCoreResult.Failure>(
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 40_000L, 100_000L),
        ).error)
        assertIs<StreamCoreResult.Failure>(recorder.reportEvent(StreamCorePlaybackProgressEvent.Completed, 100_000L, 100_000L))

        fixture.store.failure = null
        assertEquals(StreamCoreResult.Success(Unit), recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 50_000L, 100_000L))
        assertEquals(StreamCoreResult.Success(Unit), recorder.reportEvent(StreamCorePlaybackProgressEvent.Completed, 0L, 0L))
        assertEquals(2, fixture.store.removalAttempts)
        assertEquals(StreamCoreResult.Success(null), fixture.service.getProgress("profile", "film"))
    }

    @Test
    fun cancelledPersistenceRemainsCancellation() = runTest {
        val fixture = PlaybackFixture()
        val cancellation = CancellationException("cancelled checkpoint")
        fixture.store.failure = cancellation
        val recorder = fixture.service.createProgressRecorder(request(), 0L)

        val thrown = assertFailsWith<CancellationException> {
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 40_000L, 100_000L)
        }
        assertEquals(cancellation.message, thrown.message)
    }

    @Test
    fun twoRecordersFromOneServiceKeepIndependentPeriodicBuckets(): TestResult {
        return runTest {
            val fixture = PlaybackFixture()
            val first = fixture.service.createProgressRecorder(request("first"), 45_000L)
            val second = fixture.service.createProgressRecorder(request("second"), 0L)

            first.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 50_000L, 100_000L)
            second.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 40_000L, 100_000L)
            first.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 49_999L, 100_000L)
            second.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 50_000L, 100_000L)

            assertEquals(
                listOf("first" to 50_000L, "second" to 40_000L, "second" to 50_000L),
                fixture.store.writes.map { it.contentId to it.positionMillis },
            )
        }
    }

    private class RecordingProgressStore : PlaybackProgressStore {
        val writes = mutableListOf<StreamCorePlaybackProgressEntry>()
        private val savedEntries = MutableStateFlow<List<StreamCorePlaybackProgressEntry>>(emptyList())
        var removalAttempts = 0
        var failure: Exception? = null
        override fun observe(profileId: String): Flow<List<StreamCorePlaybackProgressEntry>> {
            return savedEntries.map { entries -> entries.filter { it.profileId == profileId } }
        }
        override suspend fun get(profileId: String, contentId: String): StreamCorePlaybackProgressEntry? {
            failure?.let { throw it }
            return savedEntries.value.find { it.profileId == profileId && it.contentId == contentId }
        }
        override suspend fun upsert(entry: StreamCorePlaybackProgressEntry) {
            failure?.let { throw it }
            writes += entry
            savedEntries.value = savedEntries.value.filterNot { it.profileId == entry.profileId && it.contentId == entry.contentId } + entry
        }
        override suspend fun remove(profileId: String, contentId: String) {
            removalAttempts += 1
            failure?.let { throw it }
            savedEntries.value = savedEntries.value.filterNot { it.profileId == profileId && it.contentId == contentId }
        }
    }

    private class PlaybackFixture(nowMillis: () -> Long = { 123L }) {
        private val configuration = StreamCoreConfiguration("test-backend", "playback-test")
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
        private val runtimeSession = RuntimeSession(configuration, TestProviders, {})
        val physicalProfileId = accountStorageKey(configuration, TestProviders.account.id, TestProviders.profile.id)
        val store = RecordingProgressStore()

        init {
            runtimeSession.state.value = ClientSessionState(
                accountSession = accountSession,
                authorizedProfile = AuthorizedProfile("test-authorization", selection, TestProviders.profile, null),
                profileSelection = selection,
                selectionVersion = 1L,
                isAuthInitialized = true,
            )
        }

        val service = RuntimePlaybackService(runtimeSession, StreamCoreCapabilities(), store, nowMillis)
    }

    private object TestProviders : AuthProvider, ProfileProvider, HomeProvider, DetailsProvider,
        SearchProvider, PlaybackProvider, ContentPolicyProvider {
        val account = StreamCoreAuthAccount("account", "Account", null)
        val profile = StreamCoreProfile(
            "profile", "Primary", StreamCoreProfileAvatar("avatar", null),
            StreamCoreProfileParentalLevel("adult", "All", 0), true, false,
        )
        override val authState = MutableStateFlow<StreamCoreAuthState>(StreamCoreAuthState.LoggedIn(account))

        override suspend fun restoreSession(): StreamCoreResult<StreamCoreAuthState> { return StreamCoreResult.Success(authState.value) }
        override suspend fun login(identifier: String, password: String): StreamCoreResult<Unit> { return unsupported() }
        override suspend fun loginWithQr(qrCode: String): StreamCoreResult<Unit> { return unsupported() }
        override suspend fun logout(): StreamCoreResult<Unit> { return unsupported() }
        override suspend fun recoverPassword(email: String, otp: String?): StreamCoreResult<Unit> { return unsupported() }
        override suspend fun invalidateSession() {}
        override suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>> { return StreamCoreResult.Success(listOf(profile)) }
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

private fun request(contentId: String = "film"): StreamCorePlaybackRequest {
    val content = StreamCoreContent(contentId, "Film", "", 0, "", 0, "", null, emptyList(), 0L, emptyList())
    return StreamCorePlaybackRequest("profile", content.id, content)
}
