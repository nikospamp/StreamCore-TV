package com.pampoukidis.streamcore.sdk.runtime.storage

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pampoukidis.streamcore.sdk.model.StreamCorePersistenceMode
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File

/** legacyNames must only be used by the original application installation. */
fun createAndroidSdkStorage(
    context: Context,
    config: StreamCoreConfiguration,
    authFileName: String,
    legacyNames: Boolean = false,
): SdkPlatformStorage {
    if (config.persistence == StreamCorePersistenceMode.InMemory) return SdkPlatformStorage.inMemory()
    val appContext = context.applicationContext
    val prefix = if (legacyNames) "" else "sdk_${encodeStorageName(authFileName)}_${encodeStorageName(config.backend)}_${encodeStorageName(config.storageNamespace)}_"
    val names = listOf(authFileName, "library.preferences_pb", "search_history.preferences_pb", "playback_progress.preferences_pb")
    // preferencesDataStoreFile appends .preferences_pb; the legacy doubled suffix is intentional.
    val files = names.map { appContext.preferencesDataStoreFile(prefix + it).canonicalFile }
    StorageOwners.acquire(files)
    val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.IO + job)
    return try {
        val stores = files.map { file -> PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }) }
        SdkPlatformStorage(stores[0], stores[1], stores[2], stores[3]) {
            job.invokeOnCompletion { StorageOwners.release(files) }
            scope.cancel()
        }
    } catch (throwable: Throwable) {
        scope.cancel()
        StorageOwners.release(files)
        throw throwable
    }
}

private fun encodeStorageName(value: String): String {
    return value.encodeToByteArray().joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
}

private object StorageOwners {
    private val files = mutableSetOf<String>()
    @Synchronized fun acquire(requested: List<File>) {
        check(requested.none { it.path in files }) { "An SDK instance already owns this storage namespace." }
        files.addAll(requested.map { it.path })
    }
    @Synchronized fun release(requested: List<File>) {
        files.removeAll(requested.map { it.path }.toSet())
    }
}
