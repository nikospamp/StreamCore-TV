package com.pampoukidis.streamcoretv.feature.search.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.core.okio.WebSessionStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import com.pampoukidis.streamcoretv.feature.search.domain.RecentSearchRepository
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun searchWebDataModule(useSessionStorage: Boolean) = module {
    single<DataStore<Preferences>>(named(SEARCH_HISTORY_STORE_QUALIFIER)) {
        DataStoreFactory.create(
            storage = webPreferencesStorage(
                name = SEARCH_HISTORY_STORE_FILE,
                useSessionStorage = useSessionStorage,
            ),
        )
    }
    single<Json>(named(SEARCH_HISTORY_JSON_QUALIFIER)) {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<RecentSearchRepository> {
        PreferencesRecentSearchRepository(
            dataStore = get(named(SEARCH_HISTORY_STORE_QUALIFIER)),
            json = get(named(SEARCH_HISTORY_JSON_QUALIFIER)),
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
