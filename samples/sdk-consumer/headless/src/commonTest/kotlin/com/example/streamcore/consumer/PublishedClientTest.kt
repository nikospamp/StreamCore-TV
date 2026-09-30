package com.example.streamcore.consumer

import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBReferenceProfileScenario
import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBSdk
import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBSdkConfiguration
import com.pampoukidis.streamcore.sdk.model.StreamCorePersistenceMode
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test

class PublishedClientTest {
    @Test
    fun headlessWasmExercisesPublishedProvider(): TestResult {
        return runTest {
            val client = ClientBSdk.createInMemory(
                ClientBSdkConfiguration(
                    common = StreamCoreConfiguration("clientb-reference", "wasm-test", persistence = StreamCorePersistenceMode.InMemory),
                    demoPlayback = true,
                ),
            )
            exerciseClient(client)
        }
    }

    @Test
    fun publishedSingleProtectedProfileRequiresPin(): TestResult {
        return runTest {
            exerciseReferencePinJourney(pinClient(ClientBReferenceProfileScenario.SingleProtected), multipleProfiles = false)
        }
    }

    @Test
    fun publishedHouseholdProfileSelectionEnforcesPinAndActiveProfile(): TestResult {
        return runTest {
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
