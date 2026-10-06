package com.pampoukidis.streamcore.sdk.providers.tmdb

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import com.pampoukidis.streamcore.sdk.testing.ProviderContract
import com.pampoukidis.streamcore.sdk.testing.contractValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

class TmdbSdkContractTest {
    @Test
    fun wrappedFactoryPassesSharedProviderJourneyWithoutExpectedAccount(): TestResult {
        return runTest {
            withContext(Dispatchers.Default) {
                val fixture = TmdbSdkContractFixture()
                val client = fixture.create()
                assertTrue(fixture.requestedPaths.value.isEmpty())
                ProviderContract.verifyReferenceJourney(client, "External", "password", "42")
                assertTrue("/3/account" in fixture.requestedPaths.value)
                assertTrue(fixture.requestedPaths.value.none { it.startsWith("/3/account/") })
            }
        }
    }

    @Test
    fun demoSourcesAreUnsupportedWithoutOptIn(): TestResult {
        return runTest {
            withContext(Dispatchers.Default) {
                ProviderContract.verifyPlaybackRequiresExplicitOptIn(TmdbSdkContractFixture().create(false), "External", "password")
            }
        }
    }

    @Test
    fun failedLogoutAndRejectedLoginPreserveCurrentSessionForRetry(): TestResult {
        return runTest {
            withContext(Dispatchers.Default) {
                val fixture = TmdbSdkContractFixture()
                val client = fixture.create()
                try {
                    client.auth.restoreSession().contractValue()
                    client.auth.login("External", "password").contractValue()
                    fixture.rejectCredentials = true
                    assertTrue(client.auth.login("Wrong", "wrong") is StreamCoreResult.Failure)
                    assertEquals("42", client.context.value.account?.id)
                    fixture.rejectCredentials = false
                    fixture.failLogout = true
                    assertTrue(client.auth.logout() is StreamCoreResult.Failure)
                    assertEquals("42", client.context.value.account?.id)
                    fixture.failLogout = false
                    client.auth.logout().contractValue()
                    assertNull(client.context.value.account)
                } finally {
                    client.close()
                }
            }
        }
    }

    @Test
    fun kidPolicyCoversDirectSourcesAndPreviouslySavedContent(): TestResult {
        return runTest {
            withContext(Dispatchers.Default) {
                val fixture = TmdbSdkContractFixture()
                val client = fixture.create()
                try {
                    client.auth.restoreSession().contractValue()
                    client.auth.login("External", "password").contractValue()
                    val kid = client.profiles.getProfiles().contractValue().first { it.isKidsProfile }
                    client.profiles.selectProfile(kid.id).contractValue()
                    val content = client.details.getDetails(kid.id, "101").contractValue()
                    client.library.setInMyList(kid.id, content, true).contractValue()
                    fixture.adultContent = true
                    assertTrue(client.details.getDetails(kid.id, "101") is StreamCoreResult.Failure)
                    assertTrue(client.playback.resolveSource(StreamCorePlaybackRequest(kid.id, "101", content)) is StreamCoreResult.Failure)
                    assertTrue(client.library.observe(kid.id).first().contractValue().myListContent.isEmpty())
                } finally {
                    client.close()
                }
            }
        }
    }
}
