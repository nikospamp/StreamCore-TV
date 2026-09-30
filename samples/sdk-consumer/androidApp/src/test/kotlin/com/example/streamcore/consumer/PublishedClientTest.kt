package com.example.streamcore.consumer

import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBReferenceProfileScenario
import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBSdk
import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBSdkConfiguration
import com.pampoukidis.streamcore.sdk.model.StreamCorePersistenceMode
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PublishedClientTest {
    @Test
    fun externalApplicationExercisesPublishedProvider() {
        runBlocking {
            val client = ClientBSdk.createInMemory(
                ClientBSdkConfiguration(
                    common = StreamCoreConfiguration("clientb-reference", "android-test", persistence = StreamCorePersistenceMode.InMemory),
                    demoPlayback = true,
                ),
            )
            exerciseClient(client)
        }
    }

    @Test
    fun publishedSingleProtectedProfileRequiresPin() {
        runBlocking {
            exerciseReferencePinJourney(pinClient(ClientBReferenceProfileScenario.SingleProtected), multipleProfiles = false)
        }
    }

    @Test
    fun publishedHouseholdProfileSelectionEnforcesPinAndActiveProfile() {
        runBlocking {
            exerciseReferencePinJourney(pinClient(ClientBReferenceProfileScenario.HouseholdProtected), multipleProfiles = true)
        }
    }

    private fun pinClient(scenario: ClientBReferenceProfileScenario): com.pampoukidis.streamcore.sdk.api.StreamCoreClient {
        return ClientBSdk.createInMemory(
            ClientBSdkConfiguration(
                common = StreamCoreConfiguration("clientb-reference", "published-pin-test"),
                referenceProfileScenario = scenario,
            ),
        )
    }
}
