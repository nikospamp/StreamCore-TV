package com.pampoukidis.streamcoretv.client.tmdb.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
        return preferences.first()[TmdbAuthPreferencesKeys.SessionId]?.takeIf { sessionId ->
            sessionId.isNotBlank()
        }
    }

    override suspend fun saveSession(
        sessionId: String,
        account: AuthAccountModel?,
    ) {
        dataStore.edit { prefs ->
            prefs[TmdbAuthPreferencesKeys.SessionId] = sessionId
            if (account == null) {
                prefs.removeIfPresent(TmdbAuthPreferencesKeys.AccountId)
                prefs.removeIfPresent(TmdbAuthPreferencesKeys.AccountUsername)
                prefs.removeIfPresent(TmdbAuthPreferencesKeys.AccountDisplayName)
            } else {
                prefs[TmdbAuthPreferencesKeys.AccountId] = account.id
                prefs[TmdbAuthPreferencesKeys.AccountUsername] = account.username
                account.displayName?.takeIf { displayName -> displayName.isNotBlank() }?.let { displayName ->
                    prefs[TmdbAuthPreferencesKeys.AccountDisplayName] = displayName
                } ?: prefs.removeIfPresent(TmdbAuthPreferencesKeys.AccountDisplayName)
            }
        }
    }

    override suspend fun clear() {
        clearTmdbAuthSessionPreferences(dataStore)
    }
}
