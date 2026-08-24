package com.pampoukidis.streamcoretv.feature.library.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
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
abstract class LibraryDataModule {

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(
        implementation: PreferencesLibraryRepository,
    ): LibraryRepository

    companion object {
        @Provides
        @Singleton
        @LibraryStore
        fun provideLibraryDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> {
            return PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("library.preferences_pb") },
            )
        }

        @Provides
        @Singleton
        @LibraryJson
        fun provideLibraryJson(): Json {
            return Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        }
    }
}
