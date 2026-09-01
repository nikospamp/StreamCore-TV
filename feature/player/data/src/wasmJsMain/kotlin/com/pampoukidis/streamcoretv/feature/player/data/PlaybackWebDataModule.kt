package com.pampoukidis.streamcoretv.feature.player.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.core.okio.WebSessionStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun playbackWebDataModule(useSessionStorage: Boolean) = module {
    single<DataStore<Preferences>>(named(PLAYBACK_PROGRESS_STORE_QUALIFIER)) {
        DataStoreFactory.create(
            storage = webPreferencesStorage(
                name = PLAYBACK_PROGRESS_STORE_FILE,
                useSessionStorage = useSessionStorage,
            ),
        )
    }
    single<Json>(named(PLAYBACK_PROGRESS_JSON_QUALIFIER)) {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<PlaybackProgressRepository> {
        PreferencesPlaybackProgressRepository(
            dataStore = get(named(PLAYBACK_PROGRESS_STORE_QUALIFIER)),
            json = get(named(PLAYBACK_PROGRESS_JSON_QUALIFIER)),
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
