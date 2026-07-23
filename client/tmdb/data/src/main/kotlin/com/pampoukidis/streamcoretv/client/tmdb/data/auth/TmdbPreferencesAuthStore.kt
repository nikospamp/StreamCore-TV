package com.pampoukidis.streamcoretv.client.tmdb.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

internal class TmdbPreferencesAuthStore(
    private val dataStore: DataStore<Preferences>,
) : TmdbAuthStore {

    private val preferences = dataStore.data.catch { throwable ->
        if (throwable is IOException) {
            emit(emptyPreferences())
        } else {
            throw throwable
        }
    }

    override suspend fun currentSessionId(): String? {
        return preferences.first()[Keys.SessionId]?.takeIf { sessionId ->
            sessionId.isNotBlank()
        }
    }

    override suspend fun saveSession(
        sessionId: String,
        account: AuthAccountModel?,
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.SessionId] = sessionId
            if (account == null) {
                prefs.remove(Keys.AccountId)
                prefs.remove(Keys.AccountUsername)
                prefs.remove(Keys.AccountDisplayName)
            } else {
                prefs[Keys.AccountId] = account.id
                prefs[Keys.AccountUsername] = account.username
                account.displayName?.let { displayName ->
                    prefs[Keys.AccountDisplayName] = displayName
                } ?: prefs.remove(Keys.AccountDisplayName)
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.SessionId)
            prefs.remove(Keys.AccountId)
            prefs.remove(Keys.AccountUsername)
            prefs.remove(Keys.AccountDisplayName)
        }
    }

    private object Keys {
        val SessionId = stringPreferencesKey("session_id")
        val AccountId = intPreferencesKey("account_id")
        val AccountUsername = stringPreferencesKey("account_username")
        val AccountDisplayName = stringPreferencesKey("account_display_name")
    }
}