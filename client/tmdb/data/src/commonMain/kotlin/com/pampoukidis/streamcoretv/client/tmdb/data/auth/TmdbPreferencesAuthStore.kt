package com.pampoukidis.streamcoretv.client.tmdb.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.io.IOException

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
                prefs.removeIfPresent(Keys.AccountId)
                prefs.removeIfPresent(Keys.AccountUsername)
                prefs.removeIfPresent(Keys.AccountDisplayName)
            } else {
                prefs[Keys.AccountId] = account.id
                prefs[Keys.AccountUsername] = account.username
                account.displayName?.takeIf { displayName -> displayName.isNotBlank() }?.let { displayName ->
                    prefs[Keys.AccountDisplayName] = displayName
                } ?: prefs.removeIfPresent(Keys.AccountDisplayName)
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.removeIfPresent(Keys.SessionId)
            prefs.removeIfPresent(Keys.AccountId)
            prefs.removeIfPresent(Keys.AccountUsername)
            prefs.removeIfPresent(Keys.AccountDisplayName)
        }
    }

    private object Keys {
        val SessionId = stringPreferencesKey(TMDB_SESSION_ID_KEY)
        val AccountId = intPreferencesKey(TMDB_ACCOUNT_ID_KEY)
        val AccountUsername = stringPreferencesKey(TMDB_ACCOUNT_USERNAME_KEY)
        val AccountDisplayName = stringPreferencesKey(TMDB_ACCOUNT_DISPLAY_NAME_KEY)
    }
}

private fun <T> MutablePreferences.removeIfPresent(key: Preferences.Key<T>) {
    if (this[key] != null) {
        remove(key)
    }
}
