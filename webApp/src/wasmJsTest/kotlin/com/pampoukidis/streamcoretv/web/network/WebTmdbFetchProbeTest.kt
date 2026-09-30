package com.pampoukidis.streamcoretv.web.network

import com.pampoukidis.streamcore.sdk.api.ProfileService
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileAvatar
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileParentalLevel
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreUpdateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryReady
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryPinRequired
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileSelectionResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfilePinChallenge
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfilePinPolicy
import com.pampoukidis.streamcore.sdk.model.search.StreamCoreSearchInteraction
import kotlinx.coroutines.flow.Flow
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.api.SearchService
import com.pampoukidis.streamcoretv.web.graph.WebGraphHandle
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

class WebTmdbFetchProbeTest {
    @Test
    fun validatedProfileLetsProbeReachPublicSearchWithItsQuery(): TestResult {
        return runTest {
            val fixture = Fixture()
            try {
                assertEquals(WebTmdbFetchProbeResult.Success, WebTmdbFetchProbe().run(fixture.graph))
                assertEquals(listOf("reference-profile" to "web-fetch-probe"), fixture.searchRequests)
                assertEquals(listOf("reference-profile"), fixture.selectionRequests)
                assertEquals(1, fixture.clearCalls)
            } finally {
                fixture.close()
            }
        }
    }

    @Test
    fun probeUsesTheProfileReturnedByTheSdkWithoutInventingOne(): TestResult {
        return runTest {
            val fixture = Fixture(profilesResult = StreamCoreResult.Success(listOf(profile("persisted-child", isKids = true))))
            try {
                assertEquals(WebTmdbFetchProbeResult.Success, WebTmdbFetchProbe().run(fixture.graph))
                assertEquals(listOf("persisted-child" to "web-fetch-probe"), fixture.searchRequests)
            } finally {
                fixture.close()
            }
        }
    }

    @Test
    fun profileLookupFailurePropagatesWithoutSearching(): TestResult {
        return runTest {
            val fixture = Fixture(profilesResult = StreamCoreResult.Failure(StreamCoreError.Parsing()))
            try {
                assertEquals(WebTmdbFetchProbeResult.Failure("parsing-error"), WebTmdbFetchProbe().run(fixture.graph))
                assertEquals(emptyList(), fixture.searchRequests)
            } finally {
                fixture.close()
            }
        }
    }

    @Test
    fun emptyProfilesReturnAnExplicitFailureWithoutSearching(): TestResult {
        return runTest {
            val fixture = Fixture(profilesResult = StreamCoreResult.Success(emptyList()))
            try {
                assertEquals(WebTmdbFetchProbeResult.Failure("profile-not-found"), WebTmdbFetchProbe().run(fixture.graph))
                assertEquals(emptyList(), fixture.searchRequests)
            } finally {
                fixture.close()
            }
        }
    }

    @Test
    fun searchFailureRetainsTheExistingProbeErrorMapping(): TestResult {
        return runTest {
            val fixture = Fixture(searchResult = StreamCoreResult.Failure(StreamCoreError.Server()))
            try {
                assertEquals(WebTmdbFetchProbeResult.Failure("server-error"), WebTmdbFetchProbe().run(fixture.graph))
                assertEquals(1, fixture.searchRequests.size)
            } finally {
                fixture.close()
            }
        }
    }

    @Test
    fun unauthenticatedSdkContextCannotBeBypassedByTheDiagnosticProbe(): TestResult {
        return runTest {
            val fixture = Fixture(profilesResult = StreamCoreResult.Failure(StreamCoreError.InvalidContext()))
            try {
                assertEquals(WebTmdbFetchProbeResult.Failure("invalid-context"), WebTmdbFetchProbe().run(fixture.graph))
                assertEquals(emptyList(), fixture.searchRequests)
            } finally {
                fixture.close()
            }
        }
    }

    @Test
    fun pinProtectedProfileStopsProbeWithoutSubmittingACredential(): TestResult {
        return runTest {
            val protected = profile("protected").copy(pinPolicy = StreamCoreProfilePinPolicy(digitCount = 4))
            val fixture = Fixture(
                profilesResult = StreamCoreResult.Success(listOf(protected)),
                selectionResult = StreamCoreResult.Success(
                    StreamCoreProfileEntryPinRequired(StreamCoreProfilePinChallenge("probe-challenge", protected, 4)),
                ),
            )
            try {
                assertEquals(WebTmdbFetchProbeResult.Failure("profile-pin-required"), WebTmdbFetchProbe().run(fixture.graph))
                assertEquals(emptyList(), fixture.searchRequests)
                assertEquals(listOf("probe-challenge"), fixture.cancelledChallengeIds)
                assertEquals(0, fixture.clearCalls)
            } finally {
                fixture.close()
            }
        }
    }

    private class Fixture(
        profilesResult: StreamCoreResult<List<StreamCoreProfile>> = StreamCoreResult.Success(listOf(profile("reference-profile"))),
        searchResult: StreamCoreResult<List<StreamCoreContent>> = StreamCoreResult.Success(emptyList()),
        selectionResult: StreamCoreResult<StreamCoreProfileSelectionResult>? = null,
    ) : AutoCloseable {
        val searchRequests = mutableListOf<Pair<String, String>>()
        val selectionRequests = mutableListOf<String>()
        val cancelledChallengeIds = mutableListOf<String>()
        var clearCalls = 0
            private set
        private var selectedProfileId: String? = null
        val graph = WebGraphHandle(
            application = koinApplication {
                modules(module {
                    single<ProfileService> {
                        object : ProfileService {
                            override suspend fun beginEntry(): StreamCoreResult<StreamCoreProfileEntryResult> { error("Unused") }
                            override suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>> { return profilesResult }
                            override suspend fun getProfileEditorOptions(): StreamCoreResult<StreamCoreProfileEditorOptions> { error("Unused") }
                            override suspend fun createProfile(profile: StreamCoreCreateProfile): StreamCoreResult<StreamCoreProfile> { error("Unused") }
                            override suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile> { error("Unused") }
                            override suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit> { error("Unused") }
                            override suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfileSelectionResult> {
                                selectionRequests += profileId
                                val result = selectionResult ?: StreamCoreResult.Success(StreamCoreProfileEntryReady(profile(profileId)))
                                if (result is StreamCoreResult.Success && result.value is StreamCoreProfileEntryReady) selectedProfileId = profileId
                                return result
                            }
                            override suspend fun confirmPin(challengeId: String, pin: String): StreamCoreResult<StreamCoreProfile> { error("Probe must never submit a PIN") }
                            override fun cancelPin(challengeId: String): StreamCoreResult<Unit> {
                                cancelledChallengeIds += challengeId
                                return StreamCoreResult.Success(Unit)
                            }
                            override suspend fun clearSelection(): StreamCoreResult<Unit> {
                                selectedProfileId = null
                                clearCalls += 1
                                return StreamCoreResult.Success(Unit)
                            }
                        }
                    }
                    single<SearchService> {
                        object : SearchService {
                            override suspend fun search(profileId: String, query: String, interaction: StreamCoreSearchInteraction): StreamCoreResult<List<StreamCoreContent>> {
                                if (selectedProfileId != profileId) return StreamCoreResult.Failure(StreamCoreError.InvalidContext())
                                searchRequests += profileId to query
                                return searchResult
                            }
                            override suspend fun loadTrending(profileId: String): StreamCoreResult<List<StreamCoreContent>> { error("Unused") }
                            override suspend fun displayedResults(profileId: String, query: String, results: List<StreamCoreContent>, interaction: StreamCoreSearchInteraction): StreamCoreResult<Unit> { error("Unused") }
                            override suspend fun resultSelected(profileId: String, query: String): StreamCoreResult<Unit> { error("Unused") }
                            override fun observeHistory(profileId: String): Flow<StreamCoreResult<List<String>>> { error("Unused") }
                            override suspend fun recordHistory(profileId: String, query: String): StreamCoreResult<Unit> { error("Unused") }
                            override suspend fun removeHistoryQuery(profileId: String, query: String): StreamCoreResult<Unit> { error("Unused") }
                            override suspend fun clearHistory(profileId: String): StreamCoreResult<Unit> { error("Unused") }
                        }
                    }
                })
            },
            resolvedDefinitions = emptyList(),
            storageNames = emptyList(),
        )

        override fun close() {
            graph.close()
        }
    }
}

private fun profile(id: String, isKids: Boolean = false): StreamCoreProfile {
    return StreamCoreProfile(
        id = id,
        displayName = id,
        avatar = StreamCoreProfileAvatar("avatar", null),
        parentalLevel = StreamCoreProfileParentalLevel(if (isKids) "kids" else "all", "Reference", 0),
        canDelete = true,
        isKidsProfile = isKids,
    )
}
