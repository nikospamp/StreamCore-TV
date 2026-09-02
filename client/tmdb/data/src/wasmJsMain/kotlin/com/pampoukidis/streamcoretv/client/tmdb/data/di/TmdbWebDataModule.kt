package com.pampoukidis.streamcoretv.client.tmdb.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.core.okio.WebSessionStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import com.pampoukidis.streamcoretv.client.tmdb.data.auth.TMDB_AUTH_STORE_FILE_NAME
import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
import com.pampoukidis.streamcoretv.client.tmdb.data.network.createTmdbHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun tmdbWebDataModule(useSessionStorage: Boolean) = module {
    single<HttpClient> {
        createTmdbHttpClient(
            engine = Js.create(),
            config = get<TmdbRuntimeConfig>(),
            json = get<Json>(named(TMDB_JSON_QUALIFIER)),
        )
    }
    single<DataStore<Preferences>>(named(TMDB_AUTH_STORE_QUALIFIER)) {
        DataStoreFactory.create(
            storage = webPreferencesStorage(
                name = TMDB_AUTH_STORE_FILE_NAME,
                useSessionStorage = useSessionStorage,
            ),
            corruptionHandler = ReplaceFileCorruptionHandler {
                emptyPreferences()
            },
        )
    }
}

private fun webPreferencesStorage(
    name: String,
    useSessionStorage: Boolean,
): Storage<Preferences> {
    return if (useSessionStorage) {
        WebSessionStorage(
            serializer = PreferencesSerializer,
            name = name,
        )
    } else {
        WebLocalStorage(
            serializer = PreferencesSerializer,
            name = name,
        )
    }
}
