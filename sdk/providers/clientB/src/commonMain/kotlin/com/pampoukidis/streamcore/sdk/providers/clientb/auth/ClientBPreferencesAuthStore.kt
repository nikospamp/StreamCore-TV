package com.pampoukidis.streamcore.sdk.providers.clientb.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

internal class ClientBPreferencesAuthStore(
    private val dataStore: DataStore<Preferences>,
) : ClientBAuthStore {

    override suspend fun currentAccountId(): String? {
        val values = dataStore.data.first()
        return values[Keys.AccountId]?.takeIf { values[Keys.IsLoggedIn] == true && it.isNotBlank() }
    }

    override suspend fun setAccountId(id: String) {
        dataStore.edit { preferences ->
            preferences[Keys.IsLoggedIn] = true
            preferences[Keys.AccountId] = id
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.IsLoggedIn)
            preferences.remove(Keys.AccountId)
        }
    }

    private object Keys {
        val IsLoggedIn = booleanPreferencesKey("is_logged_in")
        val AccountId = stringPreferencesKey("account_id")
    }
}
