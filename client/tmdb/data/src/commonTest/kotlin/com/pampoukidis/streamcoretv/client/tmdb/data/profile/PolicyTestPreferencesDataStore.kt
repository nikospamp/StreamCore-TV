package com.pampoukidis.streamcoretv.client.tmdb.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

internal class PolicyTestPreferencesDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    private val mutex = Mutex()
    private val state = MutableStateFlow(initial)
    var failure: Throwable? = null
    var updates: Int = 0
        private set

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        return mutex.withLock {
            updates += 1
            failure?.let { throw it }
            transform(state.value).also { state.value = it }
        }
    }
}

internal fun policyTestProfileRepository(
    dataStore: DataStore<Preferences> = PolicyTestPreferencesDataStore(),
): TmdbProfileRepository {
    return TmdbProfileRepository(dataStore = dataStore, json = Json, accountId = "policy-test")
}
