package com.pampoukidis.streamcore.sdk.providers.clientb

import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration

/** Simulated backend; demonstrates structural substitutability only. */
data class ClientBSdkConfiguration(
    val common: StreamCoreConfiguration,
    val demoPlayback: Boolean = false,
    val legacyApplicationStorage: Boolean = false,
    val referenceProfileScenario: ClientBReferenceProfileScenario = ClientBReferenceProfileScenario.Standard,
)
