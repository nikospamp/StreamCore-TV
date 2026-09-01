package com.pampoukidis.streamcoretv.feature.search.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pampoukidis.streamcoretv.feature.search.domain.RecentSearchRepository
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val searchDataModule = module {
    single<DataStore<Preferences>>(named(SEARCH_HISTORY_STORE_QUALIFIER)) {
        PreferenceDataStoreFactory.create(
            produceFile = {
                androidContext().preferencesDataStoreFile("search_history.preferences_pb")
            },
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
