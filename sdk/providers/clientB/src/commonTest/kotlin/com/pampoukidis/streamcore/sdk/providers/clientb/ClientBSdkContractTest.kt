package com.pampoukidis.streamcore.sdk.providers.clientb

import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.model.StreamCorePersistenceMode
import com.pampoukidis.streamcore.sdk.testing.ProviderContract
import kotlin.test.Test
import kotlin.test.assertNull
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest

/** Simulated-provider substitution checks, not a claim of production backend coverage. */
class ClientBSdkContractTest {
    @Test
    fun publicFactoryPassesSharedProviderJourney(): TestResult {
        return runTest {
            val client = ClientBSdk.createInMemory(configuration(demoPlayback = true))
            assertNull(client.context.value.account)
            ProviderContract.verifyReferenceJourney(client, "External", "password", "clientb:external")
        }
    }

    @Test
    fun demoSourcesAreUnsupportedByDefault(): TestResult {
        return runTest {
            ProviderContract.verifyPlaybackRequiresExplicitOptIn(
                ClientBSdk.createInMemory(configuration(demoPlayback = false)),
                "External", "password",
            )
        }
    }

    private fun configuration(demoPlayback: Boolean): ClientBSdkConfiguration {
        return ClientBSdkConfiguration(
            common = StreamCoreConfiguration("clientb-reference", "provider-contract", persistence = StreamCorePersistenceMode.InMemory),
            demoPlayback = demoPlayback,
        )
    }
}
