package com.pampoukidis.streamcoretv.feature.library.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val libraryDataModule = module {
    single<DataStore<Preferences>>(named(LIBRARY_STORE_QUALIFIER)) {
        PreferenceDataStoreFactory.create(
            produceFile = {
                androidContext().preferencesDataStoreFile("library.preferences_pb")
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
