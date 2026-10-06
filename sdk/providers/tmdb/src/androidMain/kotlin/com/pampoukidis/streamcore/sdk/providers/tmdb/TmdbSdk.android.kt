package com.pampoukidis.streamcore.sdk.providers.tmdb

import android.content.Context
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.providers.tmdb.network.createTmdbHttpClient
import com.pampoukidis.streamcore.sdk.runtime.storage.createAndroidSdkStorage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json

fun TmdbSdk.createAndroid(context: Context, config: TmdbSdkConfiguration): StreamCoreClient {
        val storage = createAndroidSdkStorage(context, config.common, "tmdb_auth.preferences_pb")
    return try {
        val client = createTmdbHttpClient(OkHttp.create(), config.connection, Json { ignoreUnknownKeys = true })
        createTmdbSdk(config, storage, client)
    } catch (throwable: Throwable) {
        storage.close()
        throw throwable
    }
}

