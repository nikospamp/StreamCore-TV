package com.pampoukidis.streamcore.sdk.runtime.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Platform infrastructure for provider factories. Never exposed by StreamCoreClient. */
class SdkPlatformStorage(
    val auth: DataStore<Preferences>,
    val library: DataStore<Preferences>,
    val search: DataStore<Preferences>,
    val progress: DataStore<Preferences>,
    private val release: () -> Unit,
) {
    private val closed = MutableStateFlow(false)

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        release()
    }

    companion object {
        fun inMemory(): SdkPlatformStorage {
            return SdkPlatformStorage(MemoryPreferences(), MemoryPreferences(), MemoryPreferences(), MemoryPreferences()) {}
        }
    }
}

private class MemoryPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()
    override val data: Flow<Preferences> = state
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        return mutex.withLock {
            transform(state.value).also { state.value = it }
        }
    }
}
