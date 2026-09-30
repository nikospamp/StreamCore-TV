package com.pampoukidis.streamcore.sdk.providers.clientb

import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.runtime.storage.createWebSdkStorage

fun ClientBSdk.createWeb( config: ClientBSdkConfiguration, useSessionStorage: Boolean = false): StreamCoreClient {
    val storage = createWebSdkStorage( config.common, "client_b_auth", config.legacyApplicationStorage, useSessionStorage)
    return try {
        createClientBSdk(config, storage)
    } catch (throwable: Throwable) {
        storage.close()
        throw throwable
    }
}

