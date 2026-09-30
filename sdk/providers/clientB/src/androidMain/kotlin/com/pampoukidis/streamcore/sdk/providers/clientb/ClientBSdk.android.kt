package com.pampoukidis.streamcore.sdk.providers.clientb

import android.content.Context
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.runtime.storage.createAndroidSdkStorage

fun ClientBSdk.createAndroid(context: Context, config: ClientBSdkConfiguration): StreamCoreClient {
    val storage = createAndroidSdkStorage(context, config.common, "client_b_auth", config.legacyApplicationStorage)
    return try {
        createClientBSdk(config, storage)
    } catch (throwable: Throwable) {
        storage.close()
        throw throwable
    }
}

