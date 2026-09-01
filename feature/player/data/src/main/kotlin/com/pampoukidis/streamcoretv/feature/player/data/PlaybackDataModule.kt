package com.pampoukidis.streamcoretv.feature.player.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val playbackDataModule = module {
    single<DataStore<Preferences>>(named(PLAYBACK_PROGRESS_STORE_QUALIFIER)) {
        PreferenceDataStoreFactory.create(
            produceFile = {
                androidContext().preferencesDataStoreFile("playback_progress.preferences_pb")
            },
        )
    }
    single {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<PlaybackProgressRepository> {
        PreferencesPlaybackProgressRepository(
            dataStore = get(named(PLAYBACK_PROGRESS_STORE_QUALIFIER)),
            json = get(),
        )
    }
}
