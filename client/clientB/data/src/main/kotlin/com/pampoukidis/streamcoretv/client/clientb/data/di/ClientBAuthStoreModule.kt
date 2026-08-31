package com.pampoukidis.streamcoretv.client.clientb.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pampoukidis.streamcoretv.client.clientb.data.auth.ClientBAuthStore
import com.pampoukidis.streamcoretv.client.clientb.data.auth.ClientBPreferencesAuthStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ClientBAuthStoreModule {

    @Provides
    @Singleton
    fun provideClientBAuthDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = {
                context.preferencesDataStoreFile(CLIENT_B_AUTH_STORE_FILE)
            },
        )
    }

    @Provides
    @Singleton
    fun provideClientBAuthStore(
        dataStore: DataStore<Preferences>,
    ): ClientBAuthStore {
        return ClientBPreferencesAuthStore(dataStore = dataStore)
    }

    private const val CLIENT_B_AUTH_STORE_FILE = "client_b_auth"
}
