package com.pampoukidis.streamcore.sdk.runtime

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcore.sdk.model.auth.*
import com.pampoukidis.streamcore.sdk.model.profile.*
import com.pampoukidis.streamcore.sdk.model.catalog.*
import com.pampoukidis.streamcore.sdk.model.error.*
import com.pampoukidis.streamcore.sdk.model.playback.*
import com.pampoukidis.streamcore.sdk.model.*
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.AuthProvider
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.HomeProvider
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.DetailsProvider
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.SearchProvider
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.PlaybackProvider
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ContentPolicyProvider
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProfileProvider
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProviderSessionFactory
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProviderSessionServices
import com.pampoukidis.streamcore.sdk.runtime.storage.PreferencesSdkStorage
import com.pampoukidis.streamcore.sdk.runtime.storage.SdkPlatformStorage
import com.pampoukidis.streamcore.sdk.model.search.StreamCoreSearchInteraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.json.Json
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RuntimeStreamCoreClientTest {
    @Test
    fun loginPublishesAccountOnlyAfterProfileEntryIsReady(): TestResult {
        return runTest {
            val f = Fixture()
            f.client.bootstrap().valueOrThrow()
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            f.contextStore.beforeWrite = { started.complete(Unit); release.await() }
            val observed = mutableListOf<StreamCoreContext>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                f.client.context.collect { observed.add(it) }
            }

            val login = async { f.client.auth.login("A", "password") }
            started.await()
            assertIs<StreamCoreAuthState.LoggedIn>(f.auth.authState.value)
            assertNull(f.client.context.value.account)
            assertFalse(f.client.context.value.isBootstrapped)
            assertTrue(observed.all { it.account == null })

            release.complete(Unit)
            login.await().valueOrThrow()
            assertEquals("A", f.client.context.value.account?.id)
            assertTrue(f.client.context.value.isBootstrapped)
            assertTrue(observed.all { it.account == null || it.isBootstrapped })
            assertIs<StreamCoreProfileEntryReady>(f.client.profiles.beginEntry().valueOrThrow())
        }
    }

    @Test
    fun cancelledAccountInstallationReconcilesCommittedAccountWithoutOldGrant(): TestResult {
        return runTest {
            val f = Fixture()
            f.login("A")
            val started = CompletableDeferred<Unit>()
            f.contextStore.beforeWrite = {
                f.contextStore.beforeWrite = {}
                started.complete(Unit)
                awaitCancellation()
            }
            val login = async { f.client.auth.login("B", "password") }
            started.await()
            assertNull(f.client.context.value.account)
            assertNull(f.client.context.value.profile)
            assertNull(f.client.context.value.profileActivationId)

            login.cancel()
            assertFailsWith<CancellationException> { login.await() }
            assertEquals("B", f.client.context.value.account?.id)
            assertTrue(f.client.context.value.isBootstrapped)
            assertNull(f.client.context.value.profile)
            assertIs<StreamCoreProfileEntryReady>(f.client.profiles.beginEntry().valueOrThrow())
        }
    }

    @Test
    fun accountInstallationStorageFailureRemainsHiddenAndBootstrapCanRetry(): TestResult {
        return runTest {
            val f = Fixture()
            f.client.bootstrap().valueOrThrow()
            f.contextStore.failWrites = true
            val failed = assertIs<StreamCoreResult.Failure>(f.client.auth.login("A", "password"))
            assertIs<StreamCoreError.Storage>(failed.error)
            assertNull(f.client.context.value.account)
            assertFalse(f.client.context.value.isBootstrapped)

            f.contextStore.failWrites = false
            f.client.bootstrap().valueOrThrow()
            assertEquals("A", f.client.context.value.account?.id)
            assertTrue(f.client.context.value.isBootstrapped)
            assertIs<StreamCoreProfileEntryReady>(f.client.profiles.beginEntry().valueOrThrow())
        }
    }

    @Test
    fun lazyConstructionValidationAndUnsupportedOperations() = runTest {
        val f = Fixture()
        assertEquals(0, f.auth.bootstrapCalls)
        assertIs<StreamCoreError.Validation>(assertIs<StreamCoreResult.Failure>(f.client.auth.login(" ", "secret")).error)
        assertEquals(0, f.auth.loginCalls)
        assertIs<StreamCoreError.Unsupported>(assertIs<StreamCoreResult.Failure>(f.client.auth.loginWithQr("code")).error)
        assertIs<StreamCoreError.Unsupported>(assertIs<StreamCoreResult.Failure>(f.client.auth.recoverPassword("user@example.org")).error)
        f.client.bootstrap()
        assertTrue(f.client.context.value.isBootstrapped)
    }

    @Test
    fun successiveAccountsWithOverlappingProfilesRetainIsolatedData() = runTest {
        val f = Fixture()
        f.login("A")
        f.client.search.recordHistory("profile", " Some   movie ")
        f.client.library.setLiked("profile", content, true)
        f.client.playback.updateProgress(progress())
        f.client.auth.logout()
        f.login("B")
        assertEquals(emptyList(), f.client.search.observeHistory("profile").first().valueOrThrow())
        assertEquals(emptyList(), (f.client.library.observe("profile").first() as StreamCoreResult.Success).value.likedContent)
        assertNull(f.client.playback.getProgress("profile", "movie").valueOrThrow())
        f.client.auth.logout()
        f.login("A")
        assertEquals(listOf("Some movie"), f.client.search.observeHistory("profile").first().valueOrThrow())
        assertEquals(1, (f.client.library.observe("profile").first() as StreamCoreResult.Success).value.likedContent.size)
        assertEquals(40_000L, f.client.playback.getProgress("profile", "movie").valueOrThrow()?.positionMillis)
    }

    @Test
    fun logoutFailureAndCancellationPreserveAccountForRetry() = runTest {
        val f = Fixture(); f.login("A")
        f.auth.logoutResult = StreamCoreResult.Failure(StreamCoreError.Network())
        assertIs<StreamCoreResult.Failure>(f.client.auth.logout())
        assertEquals("A", f.client.context.value.account?.id)
        f.auth.cancelLogout = true
        assertFailsWith<CancellationException> { f.client.auth.logout() }
        assertEquals("A", f.client.context.value.account?.id)
        f.auth.cancelLogout = false; f.auth.logoutResult = StreamCoreResult.Success(Unit)
        f.client.auth.logout()
        assertNull(f.client.context.value.account)
    }

    @Test
    fun obsoleteSessionRejectionCannotInvalidateNewSession() = runTest {
        val f = Fixture(); f.login("A")
        val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        f.catalogue.beforeDetails = { started.complete(Unit); release.await() }
        f.catalogue.detailsResult = StreamCoreResult.Failure(StreamCoreError.SessionExpired())
        val pending = async { f.client.details.getDetails("profile", "movie") }
        started.await(); f.client.auth.logout(); f.login("B"); release.complete(Unit)
        assertIs<StreamCoreError.InvalidContext>(assertIs<StreamCoreResult.Failure>(pending.await()).error)
        assertEquals("B", f.client.context.value.account?.id)
        assertEquals(0, f.auth.invalidations)
    }

    @Test
    fun authoritativeRejectionInvalidatesButNetworkAndCredentialsDoNot() = runTest {
        val f = Fixture(); f.login("A")
        f.catalogue.detailsResult = StreamCoreResult.Failure(StreamCoreError.Network())
        f.client.details.getDetails("profile", "movie")
        assertEquals("A", f.client.context.value.account?.id)
        f.auth.loginResult = StreamCoreResult.Failure(StreamCoreError.Authentication())
        f.client.auth.login("bad", "bad")
        assertEquals("A", f.client.context.value.account?.id)
        f.catalogue.detailsResult = StreamCoreResult.Failure(StreamCoreError.SessionExpired())
        f.client.details.getDetails("profile", "movie")
        assertNull(f.client.context.value.account)
        assertEquals(1, f.auth.invalidations)
    }

    @Test
    fun policiesApplyToDirectSourcesAndSavedCollectionsAndDeletedProfiles() = runTest {
        val f = Fixture(); f.login("A")
        f.client.profiles.selectProfile("profile")
        f.client.library.setInMyList("profile", content, true)
        f.catalogue.allowed = false
        assertIs<StreamCoreError.Unauthorized>(assertIs<StreamCoreResult.Failure>(f.client.details.getDetails("profile", "movie")).error)
        assertIs<StreamCoreError.Unauthorized>(assertIs<StreamCoreResult.Failure>(f.client.playback.resolveSource(StreamCorePlaybackRequest("profile", "movie", content))).error)
        assertEquals(0, f.catalogue.sourceCalls)
        assertEquals(emptyList(), (f.client.library.observe("profile").first() as StreamCoreResult.Success).value.likedContent)
        assertEquals(emptyList(), (f.client.search.search("profile", " Some   movie ") as StreamCoreResult.Success).value)
        assertEquals("Some movie", f.catalogue.lastQuery)
        f.profiles.values = emptyList()
        assertIs<StreamCoreError.InvalidContext>(assertIs<StreamCoreResult.Failure>(f.client.details.getDetails("profile", "movie")).error)
        assertNull(f.client.context.value.profile)
    }

    @Test
    fun separateDomainProvidersApplySharedPolicyToEveryContentPath() = runTest {
        val f = Fixture()
        f.login("A")
        f.catalogue.homeResult = StreamCoreResult.Success(listOf(StreamCoreCollection("home", "Home", "", listOf(content))))
        f.client.library.setLiked("profile", content, true).valueOrThrow()
        f.client.library.setInMyList("profile", content, true).valueOrThrow()
        f.client.playback.updateProgress(progress()).valueOrThrow()

        f.catalogue.allowed = false
        assertEquals(emptyList(), f.client.home.getCollections("profile").valueOrThrow().single().content)
        assertEquals(emptyList(), f.client.details.getRecommendations("profile", "movie").valueOrThrow())
        assertEquals(emptyList(), f.client.search.search("profile", "movie").valueOrThrow())
        assertEquals(emptyList(), f.client.search.loadTrending("profile").valueOrThrow())
        val blockedResults = listOf(
            f.client.details.getDetails("profile", "movie"),
            f.client.playback.resolveSource(StreamCorePlaybackRequest("profile", "movie", content)),
            f.client.library.setLiked("profile", content, true),
            f.client.library.setInMyList("profile", content, true),
            f.client.playback.updateProgress(progress()),
        )
        for (result in blockedResults) {
            assertIs<StreamCoreError.Unauthorized>(assertIs<StreamCoreResult.Failure>(result).error)
        }
        assertEquals(0, f.catalogue.sourceCalls)
        val hiddenLibrary = f.client.library.observe("profile").first().valueOrThrow()
        assertEquals(emptyList(), hiddenLibrary.likedContent)
        assertEquals(emptyList(), hiddenLibrary.myListContent)
        assertEquals(emptyList(), hiddenLibrary.continueWatching)
        assertNull(f.client.playback.getProgress("profile", "movie").valueOrThrow())
        assertEquals(emptyList(), f.client.playback.observeProgress("profile").first().valueOrThrow())

        // Filtering hides saved content without deleting it. Source resolution uses backend details.
        f.catalogue.allowed = true
        assertEquals(1, f.client.library.observe("profile").first().valueOrThrow().myListContent.size)
        assertNotNull(f.client.playback.getProgress("profile", "movie").valueOrThrow())
        f.client.playback.resolveSource(StreamCorePlaybackRequest("profile", "movie", content.copy(title = "Untrusted snapshot"))).valueOrThrow()
        assertEquals(content, f.catalogue.lastSourceRequest?.contentSnapshot)
    }

    @Test
    fun progressThresholdsAreThirtySecondsAndNinetyFivePercent() = runTest {
        val f = Fixture(); f.login("A")
        f.client.playback.updateProgress(progress(29_999)); assertNull(f.client.playback.getProgress("profile", "movie").valueOrThrow())
        f.client.playback.updateProgress(progress(30_000)); assertNotNull(f.client.playback.getProgress("profile", "movie").valueOrThrow())
        f.client.playback.updateProgress(progress(95_000)); assertNull(f.client.playback.getProgress("profile", "movie").valueOrThrow())
    }

    @Test
    fun playbackFacadePreservesRecorderVersusDirectUnknownDurationSemantics(): TestResult {
        return runTest {
            val f = Fixture()
            f.login("A")
            val playback = f.client.playback
            val request = StreamCorePlaybackRequest("profile", "movie", content)
            assertEquals("demo", playback.resolveSource(request).valueOrThrow().assetId)
            assertEquals(1, f.catalogue.sourceCalls)
            playback.updateProgress(progress()).valueOrThrow()
            assertEquals(40_000L, playback.observeProgress("profile").first().valueOrThrow().single().positionMillis)

            val recorder = playback.createProgressRecorder(request, initialPositionMillis = 40_000L)
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Periodic, 80_000L, 0L).valueOrThrow()
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 0L, 0L).valueOrThrow()
            assertEquals(40_000L, playback.getProgress("profile", "movie").valueOrThrow()?.positionMillis)

            playback.updateProgress(progress().copy(durationMillis = 0L)).valueOrThrow()
            assertNull(playback.getProgress("profile", "movie").valueOrThrow())
            playback.updateProgress(progress()).valueOrThrow()
            recorder.reportEvent(StreamCorePlaybackProgressEvent.Completed, 0L, 0L).valueOrThrow()
            assertNull(playback.getProgress("profile", "movie").valueOrThrow())
            playback.updateProgress(progress()).valueOrThrow()
            playback.removeProgress("profile", "movie").valueOrThrow()
            assertEquals(emptyList(), playback.observeProgress("profile").first().valueOrThrow())
        }
    }

    @Test
    fun searchFacadeKeepsHistoryManagementWithinAuthorizedProfile(): TestResult {
        return runTest {
            val f = Fixture()
            f.login("A")
            val search = f.client.search
            search.recordHistory("profile", " First   movie ").valueOrThrow()
            search.recordHistory("profile", "Second movie").valueOrThrow()
            assertEquals(listOf("Second movie", "First movie"), search.observeHistory("profile").first().valueOrThrow())
            search.removeHistoryQuery("profile", " first MOVIE ").valueOrThrow()
            assertEquals(listOf("Second movie"), search.observeHistory("profile").first().valueOrThrow())
            search.clearHistory("profile").valueOrThrow()
            assertEquals(emptyList(), search.observeHistory("profile").first().valueOrThrow())

            f.client.profiles.clearSelection().valueOrThrow()
            val rejected = assertIs<StreamCoreResult.Failure>(search.recordHistory("profile", "Unauthorized movie"))
            assertEquals(StreamCoreContextFailureReason.NoActiveProfile, assertIs<StreamCoreError.InvalidContext>(rejected.error).reason)
        }
    }

    @Test
    fun closeIsIdempotentAndRestartRequiresFreshProfileEntry() = runTest {
        val f = Fixture(); f.login("A")
        f.client.profiles.selectProfile("profile")
        f.client.search.recordHistory("profile", "movie")
        f.client.close(); f.client.close()
        assertEquals(1, f.closeCalls); assertEquals(0, f.auth.logoutCalls)
        assertIs<StreamCoreError.Closed>(assertIs<StreamCoreResult.Failure>(f.client.details.getDetails("profile", "movie")).error)
        val next = f.newClient(); next.bootstrap()
        assertNull(next.context.value.profile)
        assertNull(next.context.value.profileActivationId)
        assertIs<StreamCoreResult.Failure>(next.search.observeHistory("profile").first())
        assertIs<StreamCoreProfileEntryReady>(next.profiles.beginEntry().valueOrThrow())
        assertEquals(listOf("movie"), next.search.observeHistory("profile").first().valueOrThrow())
        next.profiles.clearSelection(); next.close()
        val cleared = f.newClient(); cleared.bootstrap(); assertNull(cleared.context.value.profile)
    }

    @Test
    fun legacyMigrationIsOwnedRestartSafeAndPreservesUnknownBytes() = runTest {
        val f = Fixture(); val key = stringPreferencesKey("recent_searches_json")
        f.storage.search.edit { it[key] = """{"queriesByProfile":{"profile":["Ταινία 日本語"]}}""" }
        f.auth.legacyOwner = "λογαριασμός"; f.client.bootstrap(); f.login("other")
        assertEquals(emptyList(), f.client.search.observeHistory("profile").first().valueOrThrow())
        f.client.auth.logout(); f.login("λογαριασμός")
        assertEquals(listOf("Ταινία 日本語"), f.client.search.observeHistory("profile").first().valueOrThrow())
        f.client.close(); val restarted = f.newClient(); restarted.bootstrap(); restarted.profiles.beginEntry()
        assertEquals(listOf("Ταινία 日本語"), restarted.search.observeHistory("profile").first().valueOrThrow())
        val unknown = Fixture()
        unknown.storage.search.edit { it[key] = """{"queriesByProfile":{"profile":["unowned"]}}""" }
        unknown.client.bootstrap(); unknown.login("next")
        assertEquals(emptyList(), unknown.client.search.observeHistory("profile").first().valueOrThrow())
        assertNotNull(unknown.storage.search.data.first()[stringPreferencesKey("sdk_v2_unowned_recent_searches_json")])
    }

    @Test
    fun closeCancelsOwnedInFlightRequests() = runTest {
        val f = Fixture(); f.login("A")
        val started = CompletableDeferred<Unit>()
        f.catalogue.beforeDetails = { started.complete(Unit); awaitCancellation() }
        val pending = async { f.client.details.getDetails("profile", "movie") }
        started.await(); f.client.close()
        assertFailsWith<CancellationException> { pending.await() }
        assertTrue(f.client.context.value.isClosed)
    }

    @Test
    fun credentialCleanupFailureStillInvalidatesObservableSession() = runTest {
        val f = Fixture(); f.login("A")
        f.auth.failInvalidation = true
        f.catalogue.detailsResult = StreamCoreResult.Failure(StreamCoreError.SessionExpired())
        assertIs<StreamCoreResult.Failure>(f.client.details.getDetails("profile", "movie"))
        assertNull(f.client.context.value.account)
    }

    @Test
    fun providerCommittedLogoutOnLoginFailureIsReflected() = runTest {
        val f = Fixture(); f.login("A")
        f.auth.clearDuringLogin = true
        f.auth.loginResult = StreamCoreResult.Failure(StreamCoreError.Storage())
        assertIs<StreamCoreResult.Failure>(f.client.auth.login("B", "password"))
        assertNull(f.client.context.value.account)
    }

    @Test
    fun selectionAndClearRevokeAuthorizationBeforeStorageFailure() = runTest {
        val f = Fixture(); f.login("A")
        f.profiles.values = listOf(profile, profile.copy(id = "other"))
        f.client.profiles.selectProfile("profile")
        f.contextStore.failWrites = true
        assertIs<StreamCoreResult.Failure>(f.client.profiles.selectProfile("other"))
        assertNull(f.client.context.value.profile)
        f.contextStore.failWrites = false
        f.client.profiles.selectProfile("profile").valueOrThrow()
        f.contextStore.failWrites = true
        assertIs<StreamCoreResult.Failure>(f.client.profiles.clearSelection())
        assertNull(f.client.context.value.profile)
        assertNull(f.client.context.value.profileActivationId)
        assertIs<StreamCoreResult.Success<*>>(f.client.auth.logout())
        assertNull(f.client.context.value.profile)
    }

    @Test
    fun legacyUnicodeSelectionNeverBecomesRestoredAuthorization() = runTest {
        val f = Fixture(); val account = "λογαριασμός"
        f.auth.authState.value = StreamCoreAuthState.LoggedIn(StreamCoreAuthAccount(account, account, null))
        val suffix = account.encodeToByteArray().joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        val key = stringPreferencesKey("web_selected_profile_id.$suffix")
        f.storage.auth.edit { it[key] = "profile" }
        f.contextStore.failReads = true
        assertIs<StreamCoreResult.Success<*>>(f.client.bootstrap())
        assertEquals(account, f.client.context.value.account?.id)
        assertNull(f.client.context.value.profile)
        assertNull(f.client.context.value.profileActivationId)
        assertTrue(f.client.context.value.isBootstrapped)
        assertEquals("profile", f.storage.auth.data.first()[key])
        f.contextStore.failReads = false
        assertIs<StreamCoreProfileEntryReady>(f.client.profiles.beginEntry().valueOrThrow())
        assertEquals("profile", f.client.context.value.profile?.id)
    }

    @Test
    fun loginWithoutBootstrapMigratesBeforeDiscoveringNewAccount() = runTest {
        val f = Fixture(); val key = stringPreferencesKey("recent_searches_json")
        f.storage.search.edit { it[key] = """{"queriesByProfile":{"profile":["Original owner"]}}""" }
        f.auth.legacyOwner = "A"
        f.login("B")
        assertEquals(emptyList(), f.client.search.observeHistory("profile").first().valueOrThrow())
        f.client.auth.logout(); f.login("A")
        assertEquals(listOf("Original owner"), f.client.search.observeHistory("profile").first().valueOrThrow())
    }

    @Test
    fun interruptedStoreMigrationResumesWithoutReencodingCompletedPartitions() = runTest {
        val f = Fixture()
        val key = stringPreferencesKey("recent_searches_json")
        f.storage.search.edit { it[key] = """{"queriesByProfile":{"profile":["Legacy movie"]}}""" }
        f.auth.legacyOwner = "A"
        f.searchStore.failWrites = true
        assertIs<StreamCoreResult.Failure>(f.client.bootstrap())
        assertEquals("empty", f.storage.library.data.first()[stringPreferencesKey("sdk_v2_migration_owner_library_json")])
        f.searchStore.failWrites = false
        assertIs<StreamCoreResult.Success<*>>(f.client.bootstrap())
        f.login("A")
        assertEquals(listOf("Legacy movie"), f.client.search.observeHistory("profile").first().valueOrThrow())
        f.client.close(); val next = f.newClient(); next.bootstrap(); next.profiles.beginEntry()
        assertEquals(listOf("Legacy movie"), next.search.observeHistory("profile").first().valueOrThrow())
    }

    @Test
    fun oldPlaybackRecorderCannotWriteIntoNewAccountWithSameProfile() = runTest {
        val f = Fixture(); f.login("A")
        val recorder = f.client.playback.createProgressRecorder(StreamCorePlaybackRequest("profile", "movie", content))
        f.client.auth.logout(); f.login("B")
        val result = recorder.reportEvent(com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEvent.Checkpoint, 40_000, 100_000)
        assertIs<StreamCoreError.InvalidContext>(assertIs<StreamCoreResult.Failure>(result).error)
        assertNull(f.client.playback.getProgress("profile", "movie").valueOrThrow())
    }

    @Test
    fun searchMinimumAndProfileMutationValidationRemainBehindPublicOperations() = runTest {
        val f = Fixture(); f.login("A")
        assertEquals(emptyList(), (f.client.search.search("profile", " ab ") as StreamCoreResult.Success).value)
        assertEquals("", f.catalogue.lastQuery)
        assertIs<StreamCoreResult.Success<*>>(f.client.profiles.createProfile(StreamCoreCreateProfile(" New name ", "avatar", "adult")))
        assertEquals("New name", f.profiles.lastCreate?.displayName)
        assertIs<StreamCoreResult.Success<*>>(f.client.profiles.updateProfile(StreamCoreUpdateProfile("profile", " Edited name ", "avatar", "adult")))
        assertEquals("Edited name", f.profiles.lastUpdate?.displayName)
        assertIs<StreamCoreError.Validation>(assertIs<StreamCoreResult.Failure>(f.client.profiles.createProfile(StreamCoreCreateProfile(" ", "avatar", "adult"))).error)
        assertIs<StreamCoreError.Validation>(assertIs<StreamCoreResult.Failure>(f.client.profiles.createProfile(StreamCoreCreateProfile("Name", "unknown", "adult"))).error)
        assertIs<StreamCoreError.Validation>(assertIs<StreamCoreResult.Failure>(f.client.profiles.updateProfile(StreamCoreUpdateProfile("", "Name", "avatar", "adult"))).error)
    }

    @Test
    fun libraryCombinationCannotPairRetainedAccountADataWithAccountBProgress() = runTest {
        val f = Fixture(); f.login("A")
        f.client.library.setLiked("profile", content, true)
        val emissions = mutableListOf<Pair<String?, List<String>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            f.client.library.observe("profile").collect { result ->
                if (result is StreamCoreResult.Success) emissions += f.client.context.value.account?.id to result.value.likedContent.map { it.id }
            }
        }
        runCurrent()
        assertTrue(emissions.any { it.first == "A" && it.second == listOf("movie") })
        val releaseB = CompletableDeferred<Unit>()
        f.libraryStore.readGate = releaseB
        f.client.auth.logout(); f.login("B")
        runCurrent()
        assertFalse(emissions.any { it.first == "B" })
        releaseB.complete(Unit); runCurrent()
        // The old A observer cannot automatically attach to B's activation.
        assertFalse(emissions.any { it.first == "B" })
        assertTrue(f.client.library.observe("profile").first().valueOrThrow().likedContent.isEmpty())
    }

    @Test
    fun unavailableAuthStorageFailsBeforeAuthenticationAndCanRetry() = runTest {
        val fixture = Fixture()
        fixture.contextStore.failWrites = true
        assertIs<StreamCoreError.Storage>(assertIs<StreamCoreResult.Failure>(fixture.client.bootstrap()).error)
        assertIs<StreamCoreError.Storage>(assertIs<StreamCoreResult.Failure>(fixture.client.auth.login("A", "password")).error)
        assertNull(fixture.client.context.value.account)
        assertFalse(fixture.client.context.value.isBootstrapped)
        assertEquals(StreamCoreAuthState.LoggedOut, fixture.auth.authState.value)
        fixture.contextStore.failWrites = false
        assertIs<StreamCoreResult.Success<*>>(fixture.client.bootstrap())
        fixture.login("A")
        assertEquals("A", fixture.client.context.value.account?.id)
    }

    @Test
    fun entryDecisionsAndProtectedActivationAreEnforcedByPublicApi(): TestResult {
        return runTest {
            val f = Fixture(); f.authenticate()
            f.profiles.values = emptyList()
            assertEquals(StreamCoreProfileEntryNoProfiles, f.client.profiles.beginEntry().valueOrThrow())
            f.profiles.values = listOf(profile, profile.copy(id = "other"))
            assertIs<StreamCoreProfileEntryChooseProfile>(f.client.profiles.beginEntry().valueOrThrow())
            assertNull(f.client.context.value.profile)
            f.profiles.values = listOf(profile)
            assertIs<StreamCoreProfileEntryReady>(f.client.profiles.beginEntry().valueOrThrow())
            assertNotNull(f.client.context.value.profileActivationId)
            f.profiles.values = listOf(profile.copy(pinPolicy = StreamCoreProfilePinPolicy(4)))
            val challenge = assertIs<StreamCoreProfileEntryPinRequired>(f.client.profiles.beginEntry().valueOrThrow()).challenge
            assertNull(f.client.context.value.profile)
            assertIs<StreamCoreResult.Failure>(f.client.details.getDetails("profile", "movie"))
            assertIs<StreamCoreError.Validation>(assertIs<StreamCoreResult.Failure>(f.client.profiles.confirmPin(challenge.challengeId, "12x4")).error)
            assertEquals(0, f.profiles.verifyCalls)
            assertIs<StreamCoreError.PinRejected>(assertIs<StreamCoreResult.Failure>(f.client.profiles.confirmPin(challenge.challengeId, "1111")).error)
            assertNull(f.client.context.value.profile)
            assertEquals("profile", f.client.profiles.confirmPin(challenge.challengeId, "1234").valueOrThrow().id)
            assertEquals("profile", f.client.context.value.profile?.id)
            assertEquals(2, f.profiles.verifyCalls)
        }
    }

    @Test
    fun profileScopedOperationsRejectUnselectedAndMismatchedProfiles(): TestResult {
        return runTest {
            val f = Fixture(); f.authenticate()
            val results = listOf(
                f.client.home.getCollections("profile"), f.client.details.getDetails("profile", "movie"),
                f.client.search.search("profile", "movie"), f.client.search.recordHistory("profile", "movie"),
                f.client.library.setLiked("profile", content, true), f.client.playback.updateProgress(progress()),
                f.client.playback.resolveSource(StreamCorePlaybackRequest("profile", "movie", content)),
            )
            results.forEach { assertEquals(StreamCoreContextFailureReason.NoActiveProfile, assertIs<StreamCoreError.InvalidContext>(assertIs<StreamCoreResult.Failure>(it).error).reason) }
            // Profile management is account-scoped even before activation.
            assertIs<StreamCoreResult.Success<*>>(f.client.profiles.updateProfile(StreamCoreUpdateProfile("profile", "Name", "avatar", "adult")))
            f.profiles.values = listOf(profile, profile.copy(id = "other"))
            f.client.profiles.selectProfile("other").valueOrThrow()
            assertEquals(StreamCoreContextFailureReason.ProfileMismatch, assertIs<StreamCoreError.InvalidContext>(assertIs<StreamCoreResult.Failure>(f.client.details.getDetails("profile", "movie")).error).reason)
            assertIs<StreamCoreResult.Failure>(f.client.search.observeHistory("profile").first())
        }
    }

    @Test
    fun switchingRestartAndUnsupportedProvidersCannotReusePinAuthorization(): TestResult {
        return runTest {
            val f = Fixture(); f.authenticate()
            f.profiles.values = listOf(profile.copy(pinPolicy = StreamCoreProfilePinPolicy(4)), profile.copy(id = "other"))
            val first = assertIs<StreamCoreProfileEntryPinRequired>(f.client.profiles.selectProfile("profile").valueOrThrow()).challenge
            f.client.profiles.confirmPin(first.challengeId, "1234").valueOrThrow()
            val activation = f.client.context.value.profileActivationId
            val oldRecorder = f.client.playback.createProgressRecorder(StreamCorePlaybackRequest("profile", "movie", content))
            f.client.profiles.selectProfile("other").valueOrThrow()
            assertIs<StreamCoreResult.Failure>(oldRecorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 40_000, 100_000))
            val second = assertIs<StreamCoreProfileEntryPinRequired>(f.client.profiles.selectProfile("profile").valueOrThrow()).challenge
            assertNotEquals(first.challengeId, second.challengeId)
            assertIs<StreamCoreResult.Failure>(f.client.profiles.confirmPin(first.challengeId, "1234"))
            f.client.profiles.confirmPin(second.challengeId, "1234").valueOrThrow()
            assertNotEquals(activation, f.client.context.value.profileActivationId)
            f.client.close()
            val reopened = f.newClient(); reopened.bootstrap().valueOrThrow()
            assertNull(reopened.context.value.profile)
            assertIs<StreamCoreProfileEntryPinRequired>(reopened.profiles.selectProfile("profile").valueOrThrow())
            val unsupported = Fixture(pinSupported = false); unsupported.authenticate()
            unsupported.profiles.values = listOf(profile.copy(pinPolicy = StreamCoreProfilePinPolicy(4)))
            assertIs<StreamCoreError.Unsupported>(assertIs<StreamCoreResult.Failure>(unsupported.client.profiles.beginEntry()).error)
            assertNull(unsupported.client.context.value.profile)
        }
    }

    @Test
    fun cancelPinRejectsLateVerificationAndOldObserversNeverRebind(): TestResult {
        return runTest {
            val f = Fixture(); f.authenticate()
            f.profiles.values = listOf(profile.copy(pinPolicy = StreamCoreProfilePinPolicy(4)))
            val challenge = assertIs<StreamCoreProfileEntryPinRequired>(f.client.profiles.beginEntry().valueOrThrow()).challenge
            val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
            f.profiles.beforeVerify = { started.complete(Unit); release.await() }
            val pending = async { f.client.profiles.confirmPin(challenge.challengeId, "1234") }
            started.await()
            assertEquals(StreamCoreResult.Success(Unit), f.client.profiles.cancelPin(challenge.challengeId))
            assertNull(f.client.context.value.profile)
            release.complete(Unit)
            assertIs<StreamCoreError.InvalidContext>(assertIs<StreamCoreResult.Failure>(pending.await()).error)
            assertNull(f.client.context.value.profile)
            f.profiles.beforeVerify = {}
            val next = assertIs<StreamCoreProfileEntryPinRequired>(f.client.profiles.beginEntry().valueOrThrow()).challenge
            f.client.profiles.confirmPin(next.challengeId, "1234").valueOrThrow()
            val emissions = mutableListOf<StreamCoreResult<List<String>>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.client.search.observeHistory("profile").collect { emissions += it } }
            runCurrent()
            f.client.profiles.clearSelection().valueOrThrow()
            val reentry = assertIs<StreamCoreProfileEntryPinRequired>(f.client.profiles.beginEntry().valueOrThrow()).challenge
            f.client.profiles.confirmPin(reentry.challengeId, "1234").valueOrThrow()
            f.client.search.recordHistory("profile", "fresh grant").valueOrThrow(); runCurrent()
            assertIs<StreamCoreResult.Failure>(emissions.last())
            assertTrue(emissions.filterIsInstance<StreamCoreResult.Success<List<String>>>().none { "fresh grant" in it.value })
            assertEquals(listOf("fresh grant"), f.client.search.observeHistory("profile").first().valueOrThrow())
        }
    }

    @Test
    fun pinVerificationNetworkAndStorageFailuresRemainRetryableWithoutGrant(): TestResult {
        return runTest {
            val f = Fixture(); f.authenticate()
            f.profiles.values = listOf(profile.copy(pinPolicy = StreamCoreProfilePinPolicy(4)))
            val challenge = assertIs<StreamCoreProfileEntryPinRequired>(f.client.profiles.beginEntry().valueOrThrow()).challenge
            f.profiles.verificationResult = StreamCoreResult.Failure(StreamCoreError.Network())
            assertIs<StreamCoreError.Network>(assertIs<StreamCoreResult.Failure>(f.client.profiles.confirmPin(challenge.challengeId, "1234")).error)
            assertNull(f.client.context.value.profile)
            f.profiles.verificationResult = null; f.contextStore.failWrites = true
            assertIs<StreamCoreError.Storage>(assertIs<StreamCoreResult.Failure>(f.client.profiles.confirmPin(challenge.challengeId, "1234")).error)
            assertNull(f.client.context.value.profile)
            f.contextStore.failWrites = false
            assertIs<StreamCoreResult.Success<*>>(f.client.profiles.confirmPin(challenge.challengeId, "1234"))
        }
    }

    @Test
    fun logoutCancellationReflectsAuthoritativeProviderCleanup(): TestResult {
        return runTest {
            val f = Fixture(); f.login("A")
            f.auth.cancelLogout = true; f.auth.clearBeforeLogoutCancellation = true
            assertFailsWith<CancellationException> { f.client.auth.logout() }
            assertNull(f.client.context.value.account)
            assertNull(f.client.context.value.profileActivationId)
            assertIs<StreamCoreResult.Failure>(f.client.details.getDetails("profile", "movie"))
        }
    }

    @Test
    fun searchPreservesItsAuthoritativeSessionExpiredFailure(): TestResult {
        return runTest {
            val f = Fixture(); f.login("A")
            f.catalogue.searchResult = StreamCoreResult.Failure(StreamCoreError.SessionExpired())
            assertIs<StreamCoreError.SessionExpired>(assertIs<StreamCoreResult.Failure>(f.client.search.search("profile", "movie")).error)
            assertNull(f.client.context.value.account)
        }
    }

    @Test
    fun successfulCancellationNeverLeavesTheRacingPinGrantActive(): TestResult {
        return runTest {
            repeat(16) {
                val f = Fixture(); f.authenticate()
                f.profiles.values = listOf(profile.copy(pinPolicy = StreamCoreProfilePinPolicy(4)))
                val challenge = assertIs<StreamCoreProfileEntryPinRequired>(f.client.profiles.beginEntry().valueOrThrow()).challenge
                val started = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
                f.profiles.beforeVerify = { started.complete(Unit); release.await() }
                val confirmation = async(Dispatchers.Default) { f.client.profiles.confirmPin(challenge.challengeId, "1234") }
                started.await()
                val cancellation = async(Dispatchers.Default) { release.await(); f.client.profiles.cancelPin(challenge.challengeId) }
                release.complete(Unit)
                val cancelled = cancellation.await(); confirmation.await()
                if (cancelled is StreamCoreResult.Success) assertNull(f.client.context.value.profile)
                f.client.close()
            }
        }
    }

    @Test
    fun searchInteractionHistorySequencesRunBehindTheMergedService(): TestResult {
        return runTest {
            val f = Fixture(); f.login("A")
            f.client.search.search("profile", "typed", StreamCoreSearchInteraction.Typing)
            f.catalogue.searchResult = StreamCoreResult.Success(emptyList())
            f.client.search.search("profile", "empty", StreamCoreSearchInteraction.Submitted)
            f.catalogue.searchResult = StreamCoreResult.Failure(StreamCoreError.Network())
            f.client.search.search("profile", "failed", StreamCoreSearchInteraction.Submitted)
            assertEquals(emptyList(), f.client.search.observeHistory("profile").first().valueOrThrow())
            f.catalogue.searchResult = StreamCoreResult.Success(listOf(content))
            f.client.search.search("profile", "submitted", StreamCoreSearchInteraction.Submitted)
            f.client.search.search("profile", "recent", StreamCoreSearchInteraction.RecentSelected)
            f.client.search.displayedResults("profile", "cached", listOf(content), StreamCoreSearchInteraction.Submitted)
            f.client.search.displayedResults("profile", "empty cache", emptyList(), StreamCoreSearchInteraction.Submitted)
            f.client.search.resultSelected("profile", "opened")
            assertEquals(listOf("opened", "cached", "recent", "submitted"), f.client.search.observeHistory("profile").first().valueOrThrow())
        }
    }

    @Test
    fun historyFailureKeepsSuccessfulSearchWhileCancellationPropagates(): TestResult {
        return runTest {
            val f = Fixture(); f.login("A")
            f.searchStore.failWrites = true
            assertIs<StreamCoreResult.Success<*>>(f.client.search.search("profile", "successful", StreamCoreSearchInteraction.Submitted))
            val cancelled = CancellationException("cancelled history")
            f.searchStore.writeFailure = cancelled
            val propagated = assertFailsWith<CancellationException> {
                f.client.search.resultSelected("profile", "cancelled")
            }
            assertEquals(cancelled.message, propagated.message)
        }
    }

    private class Fixture(private val pinSupported: Boolean = true) {
        val auth = TestAuthentication(); val profiles = TestProfiles(); val catalogue = TestCatalogue()
        val storage = SdkPlatformStorage.inMemory(); val contextStore = ToggleStore(storage.auth); val libraryStore = ToggleStore(storage.library); val searchStore = ToggleStore(storage.search); var closeCalls = 0
        val client = newClient()
        fun newClient(): RuntimeStreamCoreClient {
            return RuntimeStreamCoreClient(StreamCoreConfiguration("test", "isolated"), StreamCoreCapabilities(playback = StreamCorePlaybackSupport.DemoMedia, profilePinVerification = pinSupported), auth,
                ProviderSessionFactory {
                    ProviderSessionServices(
                        profiles = profiles,
                        home = object : HomeProvider by catalogue {},
                        details = object : DetailsProvider by catalogue {},
                        search = object : SearchProvider by catalogue {},
                        playback = object : PlaybackProvider by catalogue {},
                        contentPolicy = object : ContentPolicyProvider by catalogue {},
                    )
                },
                PreferencesSdkStorage.create(libraryStore, searchStore, storage.progress, Json { ignoreUnknownKeys = true; encodeDefaults = true }, contextStore),
                { closeCalls++ })
        }
        suspend fun authenticate(id: String = "A") { assertIs<StreamCoreResult.Success<*>>(client.auth.login(id, "password")) }
        suspend fun login(id: String) {
            authenticate(id)
            assertIs<StreamCoreResult.Success<*>>(client.profiles.selectProfile("profile"))
        }
    }
    private class ToggleStore(private val delegate: DataStore<Preferences>) : DataStore<Preferences> {
        var failReads = false; var failWrites = false; var writeFailure: Exception? = null; var readGate: CompletableDeferred<Unit>? = null
        var beforeWrite: suspend () -> Unit = {}
        override val data: Flow<Preferences> = flow { if (failReads) error("read failed"); readGate?.await(); emitAll(delegate.data) }
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            beforeWrite()
            writeFailure?.let { throw it }
            if (failWrites) error("write failed")
            return delegate.updateData(transform)
        }
    }
    private class TestAuthentication : AuthProvider {
        override val authState = MutableStateFlow<StreamCoreAuthState>(StreamCoreAuthState.LoggedOut)
        var bootstrapCalls = 0; var loginCalls = 0; var logoutCalls = 0; var invalidations = 0
        var legacyOwner: String? = null; var cancelLogout = false; var clearBeforeLogoutCancellation = false; var failInvalidation = false; var clearDuringLogin = false
        var loginResult: StreamCoreResult<Unit> = StreamCoreResult.Success(Unit); var logoutResult: StreamCoreResult<Unit> = StreamCoreResult.Success(Unit)
        override suspend fun legacyAccountId(): String? { return legacyOwner }
        override suspend fun bootstrapAuth(): StreamCoreResult<StreamCoreAuthState> { bootstrapCalls++; return StreamCoreResult.Success(authState.value) }
        override suspend fun login(identifier: String, password: String): StreamCoreResult<Unit> {
            loginCalls++; if (clearDuringLogin) authState.value = StreamCoreAuthState.LoggedOut
            if (loginResult is StreamCoreResult.Success) authState.value = StreamCoreAuthState.LoggedIn(StreamCoreAuthAccount(identifier, identifier, null)); return loginResult
        }
        override suspend fun loginWithQr(qrCode: String): StreamCoreResult<Unit> { return StreamCoreResult.Success(Unit) }
        override suspend fun recoverPassword(email: String, otp: String?): StreamCoreResult<Unit> { return StreamCoreResult.Success(Unit) }
        override suspend fun logout(): StreamCoreResult<Unit> {
            logoutCalls++; if (clearBeforeLogoutCancellation) authState.value = StreamCoreAuthState.LoggedOut
            if (cancelLogout) throw CancellationException("cancelled")
            if (logoutResult is StreamCoreResult.Success) authState.value = StreamCoreAuthState.LoggedOut; return logoutResult
        }
        override suspend fun invalidateSession() { invalidations++; if (failInvalidation) error("storage failed"); authState.value = StreamCoreAuthState.LoggedOut }
    }
    private class TestProfiles : ProfileProvider {
        var values = listOf(profile)
        var beforeVerify: suspend () -> Unit = {}
        var verifyCalls = 0
        var verificationResult: StreamCoreResult<Unit>? = null
        var lastCreate: StreamCoreCreateProfile? = null; var lastUpdate: StreamCoreUpdateProfile? = null
        override suspend fun verifyProfilePin(profileId: String, pin: String): StreamCoreResult<Unit> {
            verifyCalls++
            beforeVerify()
            return verificationResult ?: if (pin == "1234") StreamCoreResult.Success(Unit) else StreamCoreResult.Failure(StreamCoreError.PinRejected(remainingAttempts = 2))
        }
        override suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>> { return StreamCoreResult.Success(values) }
        override suspend fun getProfileEditorOptions(): StreamCoreResult<StreamCoreProfileEditorOptions> { return StreamCoreResult.Success(StreamCoreProfileEditorOptions(listOf(profile.avatar), listOf(profile.parentalLevel))) }
        override suspend fun createProfile(profile: StreamCoreCreateProfile): StreamCoreResult<StreamCoreProfile> { lastCreate = profile; return StreamCoreResult.Success(values.first()) }
        override suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile> { lastUpdate = profile; return StreamCoreResult.Success(values.first()) }
        override suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit> { values = values.filterNot { it.id == profileId }; return StreamCoreResult.Success(Unit) }
        override suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfile> { return StreamCoreResult.Success(values.first { it.id == profileId }) }
    }
    private class TestCatalogue : HomeProvider, DetailsProvider, SearchProvider, PlaybackProvider, ContentPolicyProvider {
        var allowed = true; var sourceCalls = 0; var lastQuery = ""
        var lastSourceRequest: StreamCorePlaybackRequest? = null
        var homeResult: StreamCoreResult<List<StreamCoreCollection>> = StreamCoreResult.Success(emptyList())
        var searchResult: StreamCoreResult<List<StreamCoreContent>> = StreamCoreResult.Success(listOf(content))
        var detailsResult: StreamCoreResult<StreamCoreContent> = StreamCoreResult.Success(content); var beforeDetails: suspend () -> Unit = {}
        override suspend fun isContentAllowed(profile: StreamCoreProfile, content: StreamCoreContent): Boolean { return allowed }
        override suspend fun getDetails(profileId: String, contentId: String): StreamCoreResult<StreamCoreContent> { beforeDetails(); return detailsResult }
        override suspend fun getRecommendations(profileId: String, contentId: String): StreamCoreResult<List<StreamCoreContent>> { return StreamCoreResult.Success(listOf(content)) }
        override suspend fun getCollections(profileId: String): StreamCoreResult<List<StreamCoreCollection>> { return homeResult }
        override suspend fun search(profileId: String, query: String): StreamCoreResult<List<StreamCoreContent>> { lastQuery = query; return searchResult }
        override suspend fun loadTrending(profileId: String): StreamCoreResult<List<StreamCoreContent>> { return StreamCoreResult.Success(listOf(content)) }
        override suspend fun resolveSource(request: StreamCorePlaybackRequest): StreamCorePlaybackMedia {
            sourceCalls++
            lastSourceRequest = request
            return StreamCorePlaybackMedia("demo", "Demo", "https://example.invalid/demo.mp4")
        }
    }
    private fun <T> StreamCoreResult<T>.valueOrThrow(): T {
        return when (this) {
            is StreamCoreResult.Success -> value
            is StreamCoreResult.Failure -> error("Unexpected SDK failure: $error")
        }
    }
    private companion object {
        val profile = StreamCoreProfile("profile", "Primary", StreamCoreProfileAvatar("avatar", null), StreamCoreProfileParentalLevel("adult", "All", 0), true, false)
        val content = StreamCoreContent("movie", "Movie", "", 0, "", 0, "", null, emptyList(), 0, emptyList())
        fun progress(position: Long = 40_000): StreamCorePlaybackProgressEntry { return StreamCorePlaybackProgressEntry("profile", "movie", content, position, 100_000, 10) }
    }
}
