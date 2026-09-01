package com.pampoukidis.streamcoretv.client.clientb.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val clientBAndroidDataModule = module {
    single<DataStore<Preferences>>(named(CLIENT_B_AUTH_STORE_QUALIFIER)) {
        PreferenceDataStoreFactory.create(
            produceFile = {
                androidContext().applicationContext.preferencesDataStoreFile(CLIENT_B_AUTH_STORE_FILE_NAME)
            },
        )
    }
}

private const val CLIENT_B_AUTH_STORE_FILE_NAME = "client_b_auth"
