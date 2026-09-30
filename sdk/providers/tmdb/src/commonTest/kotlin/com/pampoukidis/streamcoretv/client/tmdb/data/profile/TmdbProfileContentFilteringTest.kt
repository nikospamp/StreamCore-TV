package com.pampoukidis.streamcoretv.client.tmdb.data.profile

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.FakeTmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.TmdbCatalogRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.catalog.TmdbDetailsRepository
import com.pampoukidis.streamcoretv.client.tmdb.data.model.ProfileDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbMovieDetailsDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.TmdbMovieListResponseDto
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbApi
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbCallExecutor
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbErrorMapper
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbReferenceDataSource
import com.pampoukidis.streamcoretv.client.tmdb.data.network.TmdbTrendingTimeWindow
import com.pampoukidis.streamcoretv.client.tmdb.data.search.TmdbSearchRepository
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreUpdateProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

class TmdbProfileContentFilteringTest {

    @Test
    fun defaultsUseTheirSavedPolicyAcrossEveryContentSurface() {
        runTest {
            val fixture = Fixture()
            fixture.assertContent("tmdb-profile-kids", includeAdult = false)
            fixture.assertContent("tmdb-profile-owner", includeAdult = true)
        }
    }

    @Test
    fun createdKidsProfileUsesPersistedPolicyAfterRepositoryRecreation() {
        runTest {
            val store = PolicyTestPreferencesDataStore()
            val created = policyTestProfileRepository(store).createProfile(
                StreamCoreCreateProfile("Child", "tmdb-avatar-01", "kids"),
            ).successValue()

            assertFalse(created.id.contains("kids"))
            Fixture(store).assertContent(created.id, includeAdult = false)
        }
    }

    @Test
    fun subsequentLoadsObserveBothDirectionsOfAnEditedPolicy() {
        runTest {
            val fixture = Fixture()
            val profileId = "tmdb-profile-owner"
            fixture.assertContent(profileId, includeAdult = true)
            fixture.updatePolicy(profileId, parentalLevelId = "kids")
            fixture.assertContent(profileId, includeAdult = false)
            fixture.updatePolicy(profileId, parentalLevelId = "teen")
            fixture.assertContent(profileId, includeAdult = true)
        }
    }

    @Test
    fun kidsInTheIdDoesNotOverrideAnEditedNonKidsSetting() {
        runTest {
            val fixture = Fixture()
            fixture.updatePolicy("tmdb-profile-kids", parentalLevelId = "all")
            fixture.assertContent("tmdb-profile-kids", includeAdult = true)
        }
    }

    @Test
    fun arbitraryPersistedIdsUsePolicyInsteadOfNamingConventions() {
        runTest {
            val fixture = Fixture(snapshotStore(id = "7daeb68c", isKids = true))
            fixture.assertContent("7daeb68c", includeAdult = false)
            fixture.updatePolicy("7daeb68c", parentalLevelId = "all")
            fixture.assertContent("7daeb68c", includeAdult = true)
        }
    }

    @Test
    fun missingProfileFailsEverySurfaceBeforeNetworkRequests() {
        runTest {
            val fixture = Fixture()
            fixture.assertFailures("missing-kids", "PROFILE_NOT_FOUND")
        }
    }

    @Test
    fun storageFailureDoesNotReuseAPreviouslyPermissivePolicy() {
        runTest {
            val fixture = Fixture()
            fixture.assertContent("tmdb-profile-owner", includeAdult = true)
            fixture.store.failure = IllegalStateException("storage unavailable")
            fixture.assertFailures("tmdb-profile-owner", "PROFILE_STORAGE_FAILURE")
        }
    }

    @Test
    fun corruptPolicySnapshotFailsEverySurface() {
        runTest {
            val fixture = Fixture(
                PolicyTestPreferencesDataStore(
                    mutablePreferencesOf(tmdbProfilesPreferencesKey("policy-test") to "{invalid"),
                ),
            )
            fixture.assertFailures("tmdb-profile-owner", "PROFILE_STORAGE_CORRUPTED")
        }
    }

    @Test
    fun cancellationPropagatesWithoutRequestingContent() {
        runTest {
            val fixture = Fixture()
            fixture.store.failure = CancellationException("cancelled")
            assertFailsWith<CancellationException> { fixture.search.search("tmdb-profile-owner", "film") }
            assertFailsWith<CancellationException> { fixture.search.loadTrending("tmdb-profile-owner") }
            assertFailsWith<CancellationException> { fixture.home.getCollections("tmdb-profile-owner") }
            assertFailsWith<CancellationException> { fixture.details.getDetails("tmdb-profile-owner", "1") }
            assertFailsWith<CancellationException> { fixture.details.getRecommendations("tmdb-profile-owner", "1") }
            assertEquals(0, fixture.api.contentCalls)
        }
    }

    @Test
    fun directDetailsCannotBypassAnUnavailableProfilePolicy() {
        runTest {
            val fixture = Fixture()
            fixture.store.failure = IllegalStateException("storage unavailable")
            val failure = assertIs<StreamCoreResult.Failure>(fixture.details.getDetails("unknown-profile", "1"))
            assertEquals("PROFILE_STORAGE_FAILURE", failure.error.source?.backendCode)
            assertEquals(0, fixture.api.contentCalls)
        }
    }

    @Test
    fun directDetailsRejectsAdultContentForKidsAndObservesAnEditedPolicy() {
        runTest {
            val fixture = Fixture()
            fixture.api.adultDetails = true

            val blocked = assertIs<StreamCoreResult.Failure>(fixture.details.getDetails("tmdb-profile-kids", "1"))
            assertIs<StreamCoreError.InvalidContext>(blocked.error)
            assertIs<StreamCoreResult.Success<*>>(fixture.details.getDetails("tmdb-profile-owner", "1"))

            fixture.updatePolicy("tmdb-profile-owner", parentalLevelId = "kids")
            assertIs<StreamCoreError.InvalidContext>(
                assertIs<StreamCoreResult.Failure>(fixture.details.getDetails("tmdb-profile-owner", "1")).error,
            )
        }
    }

    @Test
    fun directDetailsRejectsDeletedProfileBeforeRequestingContent() {
        runTest {
            val store = PolicyTestPreferencesDataStore()
            policyTestProfileRepository(store).deleteProfile("tmdb-profile-kids").successValue()
            val fixture = Fixture(store)

            val blocked = assertIs<StreamCoreResult.Failure>(fixture.details.getDetails("tmdb-profile-kids", "1"))
            assertEquals("PROFILE_NOT_FOUND", blocked.error.source?.backendCode)
            assertEquals(0, fixture.api.contentCalls)
        }
    }

    @Test
    fun sharedProfileStorageAppliesMutationsToAllContentRepositories() {
        runTest {
            val store = PolicyTestPreferencesDataStore()
            val profiles = policyTestProfileRepository(store)
            val created = profiles.createProfile(
                StreamCoreCreateProfile("Reference profile", "tmdb-avatar-01", "all"),
            ).successValue()
            val fixture = Fixture(store)
            fixture.assertContent(created.id, includeAdult = true)

            profiles.updateProfile(
                StreamCoreUpdateProfile(created.id, "Reference child", "tmdb-avatar-01", "kids"),
            ).successValue()
            fixture.assertContent(created.id, includeAdult = false)

            profiles.deleteProfile(created.id).successValue()
            fixture.assertFailures(created.id, "PROFILE_NOT_FOUND")
        }
    }

    private fun snapshotStore(id: String, isKids: Boolean): PolicyTestPreferencesDataStore {
        val snapshot = TmdbProfilesPreferences(
            schemaVersion = TMDB_PROFILES_SCHEMA_VERSION,
            nextProfileNumber = 1L,
            profiles = listOf(
                ProfileDto(
                    id = id,
                    displayName = "Saved profile",
                    avatarId = "tmdb-avatar-01",
                    avatarUrl = null,
                    parentalLevelId = if (isKids) "kids" else "all",
                    parentalLevelLabel = if (isKids) "Kids" else "All maturity",
                    parentalLevelRank = if (isKids) 20 else 100,
                    canDelete = true,
                    isKidsProfile = isKids,
                ),
            ),
        )
        return PolicyTestPreferencesDataStore(
            mutablePreferencesOf(tmdbProfilesPreferencesKey("policy-test") to Json.encodeToString(snapshot)),
        )
    }

    private class Fixture(val store: PolicyTestPreferencesDataStore = PolicyTestPreferencesDataStore()) {
        val api = PolicyApi()
        private val profiles = policyTestProfileRepository(store)
        private val references = TmdbReferenceDataSource(api)
        private val executor = TmdbCallExecutor(TmdbErrorMapper())
        val home = TmdbCatalogRepository(api, references, executor, profiles)
        val details = TmdbDetailsRepository(api, references, executor, profiles)
        val search = TmdbSearchRepository(api, references, executor, profiles)

        suspend fun updatePolicy(profileId: String, parentalLevelId: String) {
            profiles.updateProfile(
                StreamCoreUpdateProfile(profileId, "Edited", "tmdb-avatar-01", parentalLevelId),
            ).successValue()
        }

        suspend fun assertContent(profileId: String, includeAdult: Boolean) {
            val expected = if (includeAdult) listOf("1", "99", "2", "3") else listOf("1", "2", "3")
            var reads = store.updates
            assertEquals(expected, search.search(profileId, "film").successValue().map { it.id })
            assertEquals(includeAdult, api.delegate.lastSearchIncludeAdult)
            assertEquals(++reads, store.updates)
            assertEquals(expected, search.loadTrending(profileId).successValue().map { it.id })
            assertEquals(++reads, store.updates)
            val rows = home.getCollections(profileId).successValue()
            assertEquals(4, rows.size)
            rows.forEach { row -> assertEquals(expected, row.content.map { it.id }) }
            assertEquals(++reads, store.updates)
            assertEquals(expected - "1", details.getRecommendations(profileId, "1").successValue().map { it.id })
            assertEquals(++reads, store.updates)
            assertEquals("1", details.getDetails(profileId, "1").successValue().id)
            assertEquals(++reads, store.updates)
        }

        suspend fun assertFailures(profileId: String, backendCode: String) {
            val requests = api.contentCalls
            val configurationRequests = api.delegate.configurationCalls
            val results = listOf(
                search.search(profileId, "film"),
                search.loadTrending(profileId),
                home.getCollections(profileId),
                details.getRecommendations(profileId, "1"),
                details.getDetails(profileId, "1"),
            )
            results.forEach { result ->
                val error = assertIs<StreamCoreResult.Failure>(result).error
                assertEquals(backendCode, error.source?.backendCode)
                if (backendCode == "PROFILE_STORAGE_CORRUPTED") assertIs<StreamCoreError.Parsing>(error)
            }
            assertEquals(requests, api.contentCalls)
            assertEquals(configurationRequests, api.delegate.configurationCalls)
        }
    }

    // Keep counters and adult fixtures local to this ticket; other provider tests share FakeTmdbApi.
    private class PolicyApi(val delegate: FakeTmdbApi = FakeTmdbApi()) : TmdbApi by delegate {
        var adultDetails = false
        var contentCalls = 0
            private set

        override suspend fun getMovieDetails(
            movieId: Int,
            language: String,
            appendToResponse: List<String>,
        ): TmdbMovieDetailsDto {
            contentCalls += 1
            return delegate.getMovieDetails(movieId, language, appendToResponse).copy(adult = adultDetails)
        }

        override suspend fun searchMovies(query: String, includeAdult: Boolean, language: String, page: Int): TmdbMovieListResponseDto {
            contentCalls += 1
            return delegate.searchMovies(query, includeAdult, language, page)
        }

        override suspend fun getTrendingMovies(timeWindow: TmdbTrendingTimeWindow, language: String, page: Int): TmdbMovieListResponseDto {
            return contentResponse()
        }

        override suspend fun getPopularMovies(language: String, page: Int, region: String?): TmdbMovieListResponseDto {
            return contentResponse()
        }

        override suspend fun getNowPlayingMovies(language: String, page: Int, region: String?): TmdbMovieListResponseDto {
            return contentResponse()
        }

        override suspend fun getMovieRecommendations(movieId: Int, language: String, page: Int): TmdbMovieListResponseDto {
            return contentResponse()
        }

        private fun contentResponse(): TmdbMovieListResponseDto {
            contentCalls += 1
            return TmdbMovieListResponseDto(1, delegate.searchMoviesResults, 1, delegate.searchMoviesResults.size)
        }
    }
}

private fun <T> StreamCoreResult<T>.successValue(): T {
    return assertIs<StreamCoreResult.Success<T>>(this).value
}
