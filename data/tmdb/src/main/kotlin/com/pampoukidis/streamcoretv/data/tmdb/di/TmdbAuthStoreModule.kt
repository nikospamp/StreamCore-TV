package com.pampoukidis.streamcoretv.data.tmdb.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pampoukidis.streamcoretv.data.tmdb.auth.TmdbAuthStore
import com.pampoukidis.streamcoretv.data.tmdb.auth.TmdbPreferencesAuthStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TmdbAuthStoreModule {

    @Provides
    @Singleton
    fun provideTmdbAuthDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = {
                context.preferencesDataStoreFile(TMDB_AUTH_STORE_FILE)
            },
        )
    }

    @Provides
    @Singleton
    fun provideTmdbAuthStore(
        dataStore: DataStore<Preferences>,
    ): TmdbAuthStore {
        return TmdbPreferencesAuthStore(dataStore = dataStore)
    }

    private const val TMDB_AUTH_STORE_FILE = "tmdb_auth.preferences_pb"
}