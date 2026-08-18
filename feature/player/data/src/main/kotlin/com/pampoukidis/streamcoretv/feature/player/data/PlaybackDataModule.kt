package com.pampoukidis.streamcoretv.feature.player.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pampoukidis.streamcoretv.playback.api.PlaybackProgressRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackDataModule {

    @Binds
    @Singleton
    abstract fun bindPlaybackProgressRepository(
        implementation: PreferencesPlaybackProgressRepository,
    ): PlaybackProgressRepository

    companion object {
        @Provides
        @Singleton
        @PlaybackProgressStore
        fun providePlaybackProgressDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> {
            return PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("playback_progress.preferences_pb") },
            )
        }

        @Provides
        @Singleton
        fun providePlaybackJson(): Json {
            return Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        }
    }
}