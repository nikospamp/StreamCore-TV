package com.pampoukidis.streamcore.sdk.runtime.storage

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.core.okio.WebSessionStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import com.pampoukidis.streamcore.sdk.model.StreamCorePersistenceMode
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

fun createWebSdkStorage(
    config: StreamCoreConfiguration,
    authFileName: String,
    useSessionStorage: Boolean = false,
): SdkPlatformStorage {
    if (config.persistence == StreamCorePersistenceMode.InMemory) return SdkPlatformStorage.inMemory()
    val prefix = "sdk_${encodeStorageName(authFileName)}_${encodeStorageName(config.backend)}_${encodeStorageName(config.storageNamespace)}_"
    val names = listOf(authFileName, "library.preferences_pb", "search_history.preferences_pb", "playback_progress.preferences_pb").map { prefix + it }
    val ownerKeys = names.map { "${if (useSessionStorage) "session" else "local"}:$it" }
    check(ownerKeys.none { it in webStorageOwners }) { "An SDK instance already owns this storage namespace." }
    webStorageOwners.addAll(ownerKeys)
    val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.Default + job)
    return try {
        val stores = names.map { name ->
            val storage: Storage<Preferences> = if (useSessionStorage) {
                WebSessionStorage(serializer = PreferencesSerializer, name = name)
            } else {
                WebLocalStorage(serializer = PreferencesSerializer, name = name)
            }
            // Corrupt data must be preserved for recovery; never replace it with empty preferences.
            DataStoreFactory.create(storage = storage, scope = scope)
        }
        SdkPlatformStorage(stores[0], stores[1], stores[2], stores[3]) {
            job.invokeOnCompletion { webStorageOwners.removeAll(ownerKeys.toSet()) }
            scope.cancel()
        }
    } catch (throwable: Throwable) {
        scope.cancel()
        webStorageOwners.removeAll(ownerKeys.toSet())
        throw throwable
    }
}

private val webStorageOwners = mutableSetOf<String>()
private fun encodeStorageName(value: String): String {
    return value.encodeToByteArray().joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
}
