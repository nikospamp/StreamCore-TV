package com.pampoukidis.streamcoretv.client.tmdb.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal const val TMDB_AUTH_STORE_FILE_NAME = "tmdb_auth.preferences_pb"
internal const val TMDB_SESSION_ID_KEY = "session_id"
internal const val TMDB_ACCOUNT_ID_KEY = "account_id"
internal const val TMDB_ACCOUNT_USERNAME_KEY = "account_username"
internal const val TMDB_ACCOUNT_DISPLAY_NAME_KEY = "account_display_name"

suspend fun clearTmdbAuthSessionPreferences(dataStore: DataStore<Preferences>) {
    dataStore.edit { preferences ->
        preferences.clearTmdbAuthSessionPreferences()
    }
}

fun MutablePreferences.clearTmdbAuthSessionPreferences() {
    removeIfPresent(TmdbAuthPreferencesKeys.SessionId)
    removeIfPresent(TmdbAuthPreferencesKeys.AccountId)
    removeIfPresent(TmdbAuthPreferencesKeys.AccountUsername)
    removeIfPresent(TmdbAuthPreferencesKeys.AccountDisplayName)
}

internal object TmdbAuthPreferencesKeys {
    val SessionId = stringPreferencesKey(TMDB_SESSION_ID_KEY)
    val AccountId = intPreferencesKey(TMDB_ACCOUNT_ID_KEY)
    val AccountUsername = stringPreferencesKey(TMDB_ACCOUNT_USERNAME_KEY)
    val AccountDisplayName = stringPreferencesKey(TMDB_ACCOUNT_DISPLAY_NAME_KEY)
}

internal fun <T> MutablePreferences.removeIfPresent(key: Preferences.Key<T>) {
    if (this[key] != null) {
        remove(key)
    }
}
