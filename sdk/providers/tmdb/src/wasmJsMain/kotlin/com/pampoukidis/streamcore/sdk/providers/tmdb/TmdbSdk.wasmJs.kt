package com.pampoukidis.streamcore.sdk.providers.tmdb

import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.runtime.storage.createWebSdkStorage
import com.pampoukidis.streamcoretv.client.tmdb.data.network.createTmdbHttpClient
import io.ktor.client.engine.js.Js
import kotlinx.serialization.json.Json

fun TmdbSdk.createWeb( config: TmdbSdkConfiguration, useSessionStorage: Boolean = false): StreamCoreClient {
    val storage = createWebSdkStorage( config.common, "tmdb_auth.preferences_pb", config.legacyApplicationStorage, useSessionStorage)
    return try {
        val client = createTmdbHttpClient(Js.create(), config.connection, Json { ignoreUnknownKeys = true })
        createTmdbSdk(config, storage, client)
    } catch (throwable: Throwable) {
        storage.close()
        throw throwable
    }
}

