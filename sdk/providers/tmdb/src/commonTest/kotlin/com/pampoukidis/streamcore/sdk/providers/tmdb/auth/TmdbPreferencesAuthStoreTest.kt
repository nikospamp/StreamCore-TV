package com.pampoukidis.streamcore.sdk.providers.tmdb.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcore.sdk.model.auth.StreamCoreAuthAccount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class TmdbPreferencesAuthStoreTest {

    @Test
    fun unreadableStorageIsNotReportedAsAnAbsentSession() {
        runTest {
            val failure = IOException("storage unavailable")
            val dataStore = object : DataStore<Preferences> {
                override val data: Flow<Preferences> = flow { throw failure }

                override suspend fun updateData(
                    transform: suspend (t: Preferences) -> Preferences,
                ): Preferences {
                    error("Read failure must not write preferences")
                }
            }

            assertEquals(failure, assertFailsWith<IOException> {
                TmdbPreferencesAuthStore(dataStore).currentSessionId()
            })
        }
    }

    @Test
    fun persistedFilenameAndKeysRemainExact() {
        assertEquals("tmdb_auth.preferences_pb", TMDB_AUTH_STORE_FILE_NAME)
        assertEquals("session_id", TMDB_SESSION_ID_KEY)
        assertEquals("account_id", TMDB_ACCOUNT_ID_KEY)
        assertEquals("account_username", TMDB_ACCOUNT_USERNAME_KEY)
        assertEquals("account_display_name", TMDB_ACCOUNT_DISPLAY_NAME_KEY)
    }

    @Test
    fun persistedSessionRemainsReadableAfterRepositoryRecreation() = runTest {
        val dataStore = TestPreferencesDataStore(
            mutablePreferencesOf(
                stringPreferencesKey("session_id") to "saved-session",
                intPreferencesKey("account_id") to 42,
                stringPreferencesKey("account_username") to "saved-user",
                stringPreferencesKey("account_display_name") to "Saved User",
            ),
        )

        assertEquals("saved-session", TmdbPreferencesAuthStore(dataStore).currentSessionId())
        assertEquals("saved-session", TmdbPreferencesAuthStore(dataStore).currentSessionId())
    }

    @Test
    fun saveAndClearUseTheSessionKeyAcrossRecreation() = runTest {
        val dataStore = TestPreferencesDataStore()
        TmdbPreferencesAuthStore(dataStore).saveSession(
            sessionId = "new-session",
            account = StreamCoreAuthAccount(
                id = "7",
                username = "user",
                displayName = "Display",
            ),
        )

        val recreated = TmdbPreferencesAuthStore(dataStore)
        assertEquals("new-session", recreated.currentSessionId())
        recreated.clear()
        assertNull(TmdbPreferencesAuthStore(dataStore).currentSessionId())
    }

    @Test
    fun emptyStoreAcceptsNullBlankAndMissingAccountWithRepeatedClear() = runTest {
        val dataStore = TestPreferencesDataStore()
        val store = TmdbPreferencesAuthStore(dataStore)

        store.saveSession(sessionId = "session-with-null-account", account = null)
        assertEquals("session-with-null-account", store.currentSessionId())
        store.clear()
        store.clear()

        store.saveSession(
            sessionId = "session-with-blank-name",
            account = StreamCoreAuthAccount(id = "7", username = "fixture-user", displayName = ""),
        )
        assertEquals("session-with-blank-name", store.currentSessionId())
        store.clear()
        assertNull(store.currentSessionId())
    }

    @Test
    fun clearPreservesProviderProfileSnapshot() = runTest {
        val profileSnapshotKey = stringPreferencesKey("profiles_json.account-scope")
        val dataStore = TestPreferencesDataStore(
            mutablePreferencesOf(
                stringPreferencesKey(TMDB_SESSION_ID_KEY) to "session",
                intPreferencesKey(TMDB_ACCOUNT_ID_KEY) to 7,
                stringPreferencesKey(TMDB_ACCOUNT_USERNAME_KEY) to "user",
                stringPreferencesKey(TMDB_ACCOUNT_DISPLAY_NAME_KEY) to "User",
                profileSnapshotKey to "profile-snapshot-sentinel",
            ),
        )

        TmdbPreferencesAuthStore(dataStore).clear()

        val preferences = dataStore.data.first()
        assertNull(preferences[stringPreferencesKey(TMDB_SESSION_ID_KEY)])
        assertNull(preferences[intPreferencesKey(TMDB_ACCOUNT_ID_KEY)])
        assertNull(preferences[stringPreferencesKey(TMDB_ACCOUNT_USERNAME_KEY)])
        assertNull(preferences[stringPreferencesKey(TMDB_ACCOUNT_DISPLAY_NAME_KEY)])
        assertEquals("profile-snapshot-sentinel", preferences[profileSnapshotKey])
    }

    private class TestPreferencesDataStore(
        initial: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val mutex = Mutex()
        private val state = MutableStateFlow(initial)

        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences {
            return mutex.withLock {
                transform(state.value).also { updated -> state.value = updated }
            }
        }
    }
}
