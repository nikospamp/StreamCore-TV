package com.pampoukidis.streamcoretv.client.clientb.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

internal class ClientBPreferencesAuthStore(
    private val dataStore: DataStore<Preferences>,
) : ClientBAuthStore {

    override suspend fun isLoggedIn(): Boolean {
        return dataStore.data.first()[Keys.IsLoggedIn] == true
    }

    override suspend fun setLoggedIn() {
        dataStore.edit { preferences ->
            preferences[Keys.IsLoggedIn] = true
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.IsLoggedIn)
        }
    }

    private object Keys {
        val IsLoggedIn = booleanPreferencesKey("is_logged_in")
    }
}
