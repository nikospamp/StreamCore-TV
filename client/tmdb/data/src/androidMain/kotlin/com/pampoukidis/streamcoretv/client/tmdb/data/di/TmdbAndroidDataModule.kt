package com.pampoukidis.streamcoretv.client.tmdb.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pampoukidis.streamcoretv.client.tmdb.data.auth.TMDB_AUTH_STORE_FILE_NAME
import com.pampoukidis.streamcoretv.client.tmdb.data.config.TmdbRuntimeConfig
import com.pampoukidis.streamcoretv.client.tmdb.data.network.createTmdbHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val tmdbAndroidDataModule = module {
    single<HttpClient> {
        createTmdbHttpClient(
            engine = OkHttp.create(),
            config = get<TmdbRuntimeConfig>(),
            json = get<Json>(named(TMDB_JSON_QUALIFIER)),
        )
    }
    single<DataStore<Preferences>>(named(TMDB_AUTH_STORE_QUALIFIER)) {
        PreferenceDataStoreFactory.create(
            produceFile = {
                androidContext().applicationContext.preferencesDataStoreFile(TMDB_AUTH_STORE_FILE_NAME)
            },
        )
    }
}
