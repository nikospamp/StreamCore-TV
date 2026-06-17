package com.pampoukidis.streamcoretv.data.tmdb.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.core.model.auth.AuthAccountModel
import com.pampoukidis.streamcoretv.core.model.auth.AuthStateModel
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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

    override val authState: Flow<AuthStateModel> = preferences.map { prefs ->
        val sessionId = prefs[Keys.SessionId]
        if (sessionId.isNullOrBlank()) {
            return@map AuthStateModel.LoggedOut
        }

        AuthStateModel.LoggedIn(account = prefs.toAccountModel())
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

    private fun Preferences.toAccountModel(): AuthAccountModel? {
        val accountId = this[Keys.AccountId] ?: return null
        val username = this[Keys.AccountUsername]?.takeIf { it.isNotBlank() } ?: return null

        return AuthAccountModel(
            id = accountId,
            username = username,
            displayName = this[Keys.AccountDisplayName],
        )
    }

    private object Keys {
        val SessionId = stringPreferencesKey("session_id")
        val AccountId = intPreferencesKey("account_id")
        val AccountUsername = stringPreferencesKey("account_username")
        val AccountDisplayName = stringPreferencesKey("account_display_name")
    }
}