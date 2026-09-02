package com.pampoukidis.streamcoretv.feature.library.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.core.okio.WebSessionStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.emptyPreferences
import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun libraryWebDataModule(useSessionStorage: Boolean) = module {
    single<DataStore<Preferences>>(named(LIBRARY_STORE_QUALIFIER)) {
        DataStoreFactory.create(
            storage = webPreferencesStorage(
                name = LIBRARY_STORE_FILE,
                useSessionStorage = useSessionStorage,
            ),
            corruptionHandler = ReplaceFileCorruptionHandler {
                emptyPreferences()
            },
        )
    }
    single<Json>(named(LIBRARY_JSON_QUALIFIER)) {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<LibraryRepository> {
        PreferencesLibraryRepository(
            dataStore = get(named(LIBRARY_STORE_QUALIFIER)),
            json = get(named(LIBRARY_JSON_QUALIFIER)),
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
