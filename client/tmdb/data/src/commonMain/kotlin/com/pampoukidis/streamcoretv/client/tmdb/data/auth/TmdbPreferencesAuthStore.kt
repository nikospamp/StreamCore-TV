package com.pampoukidis.streamcoretv.client.tmdb.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import kotlinx.coroutines.flow.first

internal class TmdbPreferencesAuthStore(
    private val dataStore: DataStore<Preferences>,
) : TmdbAuthStore {

    override suspend fun currentSessionId(): String? {
        return dataStore.data.first()[TmdbAuthPreferencesKeys.SessionId]?.takeIf { sessionId ->
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
