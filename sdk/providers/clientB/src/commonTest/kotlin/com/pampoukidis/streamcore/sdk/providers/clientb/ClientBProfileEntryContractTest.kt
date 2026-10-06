package com.pampoukidis.streamcore.sdk.providers.clientb

import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryChooseProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryPinRequired
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryReady
import com.pampoukidis.streamcore.sdk.testing.contractValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest

/** Public-client reference checks; PIN `1234` belongs only to the simulated provider. */
class ClientBProfileEntryContractTest {
    @Test
    fun defaultScenarioKeepsExistingUnprotectedProfiles(): TestResult {
        return runTest {
            val client = client()
            try {
                login(client)
                val entry = assertIs<StreamCoreProfileEntryChooseProfile>(client.profiles.beginEntry().contractValue())
                assertEquals(listOf("Primary", "Family"), entry.profiles.map { it.displayName })
                assertTrue(entry.profiles.all { it.pinPolicy == null })
                assertTrue(client.capabilities.profilePinVerification)
                assertNull(client.context.value.profile)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun singleUnprotectedProfileEntersWithoutASelectionStep(): TestResult {
        return runTest {
            val client = client(ClientBReferenceProfileScenario.Single)
            try {
                login(client)
                val entry = assertIs<StreamCoreProfileEntryReady>(client.profiles.beginEntry().contractValue())
                assertEquals(entry.profile, client.context.value.profile)
                assertTrue(client.home.getCollections(entry.profile.id).contractValue().isNotEmpty())
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun protectedSingleProfileRequiresProviderVerificationBeforeContentAccess(): TestResult {
        return runTest {
            val client = client(ClientBReferenceProfileScenario.SingleProtected)
            try {
                login(client)
                val challenge = assertIs<StreamCoreProfileEntryPinRequired>(client.profiles.beginEntry().contractValue()).challenge
                assertEquals(4, challenge.digitCount)
                assertEquals(4, challenge.profile.pinPolicy?.digitCount)
                assertNull(client.context.value.profile)
                assertIs<StreamCoreResult.Failure>(client.home.getCollections(challenge.profile.id))

                val rejected = assertIs<StreamCoreResult.Failure>(client.profiles.confirmPin(challenge.challengeId, "0000"))
                assertIs<StreamCoreError.PinRejected>(rejected.error)
                assertEquals("clientb:viewer", client.context.value.account?.id)
                assertNull(client.context.value.profile)

                val profile = client.profiles.confirmPin(challenge.challengeId, "1234").contractValue()
                assertEquals(profile, client.context.value.profile)
                assertTrue(client.home.getCollections(profile.id).contractValue().isNotEmpty())
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun switchingToKidsRevokesAdultAccessAndRequiresANewChallenge(): TestResult {
        return runTest {
            val client = client(ClientBReferenceProfileScenario.HouseholdProtected)
            try {
                login(client)
                val entry = assertIs<StreamCoreProfileEntryChooseProfile>(client.profiles.beginEntry().contractValue())
                val adult = entry.profiles.single { !it.isKidsProfile }
                val kids = entry.profiles.single { it.isKidsProfile }
                assertNull(kids.pinPolicy)
                val first = assertIs<StreamCoreProfileEntryPinRequired>(client.profiles.selectProfile(adult.id).contractValue()).challenge
                client.profiles.confirmPin(first.challengeId, "1234").contractValue()
                val adultContent = client.home.getCollections(adult.id).contractValue().flatMap { it.content }.first()
                client.library.setLiked(adult.id, adultContent, true).contractValue()

                assertIs<StreamCoreProfileEntryReady>(client.profiles.selectProfile(kids.id).contractValue())
                assertTrue(client.home.getCollections(kids.id).contractValue().isNotEmpty())
                assertIs<StreamCoreResult.Failure>(client.home.getCollections(adult.id))
                assertIs<StreamCoreResult.Failure>(client.library.observe(adult.id).first())

                val second = assertIs<StreamCoreProfileEntryPinRequired>(client.profiles.selectProfile(adult.id).contractValue()).challenge
                assertNotEquals(first.challengeId, second.challengeId)
                assertIs<StreamCoreResult.Failure>(client.profiles.confirmPin(first.challengeId, "1234"))
                client.profiles.confirmPin(second.challengeId, "1234").contractValue()
                assertTrue(client.library.observeContentState(adult.id, adultContent.id).first().contractValue().isLiked)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun cancellationLeavesProfileLockedAndAnotherEntryCanRetry(): TestResult {
        return runTest {
            val client = client(ClientBReferenceProfileScenario.SingleProtected)
            try {
                login(client)
                val first = assertIs<StreamCoreProfileEntryPinRequired>(client.profiles.beginEntry().contractValue()).challenge
                client.profiles.cancelPin(first.challengeId).contractValue()
                assertIs<StreamCoreResult.Failure>(client.profiles.confirmPin(first.challengeId, "1234"))
                assertNull(client.context.value.profile)

                val second = assertIs<StreamCoreProfileEntryPinRequired>(client.profiles.beginEntry().contractValue()).challenge
                assertNotEquals(first.challengeId, second.challengeId)
                client.profiles.confirmPin(second.challengeId, "1234").contractValue()
                assertEquals(second.profile.id, client.context.value.profile?.id)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun overlappingProfileIdsDoNotShareAuthorizationOrSavedLibraryAcrossAccounts(): TestResult {
        return runTest {
            val client = client(ClientBReferenceProfileScenario.SingleProtected)
            try {
                login(client, "Alice")
                val alice = assertIs<StreamCoreProfileEntryPinRequired>(client.profiles.beginEntry().contractValue()).challenge
                client.profiles.confirmPin(alice.challengeId, "1234").contractValue()
                val content = client.home.getCollections(alice.profile.id).contractValue().flatMap { it.content }.first()
                client.library.setLiked(alice.profile.id, content, true).contractValue()
                client.auth.logout().contractValue()

                client.auth.login("Bob", "password").contractValue()
                val bob = assertIs<StreamCoreProfileEntryPinRequired>(client.profiles.beginEntry().contractValue()).challenge
                assertEquals(alice.profile.id, bob.profile.id)
                assertIs<StreamCoreResult.Failure>(client.home.getCollections(bob.profile.id))
                assertIs<StreamCoreResult.Failure>(client.profiles.confirmPin(alice.challengeId, "1234"))
                client.profiles.confirmPin(bob.challengeId, "1234").contractValue()
                assertTrue(client.library.observe(bob.profile.id).first().contractValue().likedContent.isEmpty())
                client.auth.logout().contractValue()

                client.auth.login("Alice", "password").contractValue()
                val returning = assertIs<StreamCoreProfileEntryPinRequired>(client.profiles.beginEntry().contractValue()).challenge
                client.profiles.confirmPin(returning.challengeId, "1234").contractValue()
                assertTrue(client.library.observeContentState(returning.profile.id, content.id).first().contractValue().isLiked)
            } finally {
                client.close()
            }
        }
    }

    private fun client(
        scenario: ClientBReferenceProfileScenario = ClientBReferenceProfileScenario.Standard,
    ): StreamCoreClient {
        return ClientBSdk.createInMemory(
            ClientBSdkConfiguration(
                common = StreamCoreConfiguration("clientb-reference", "profile-entry-contract"),
                referenceProfileScenario = scenario,
            ),
        )
    }

    private suspend fun login(client: StreamCoreClient, identifier: String = "Viewer") {
        client.auth.restoreSession().contractValue()
        client.auth.login(identifier, "password").contractValue()
    }
}
