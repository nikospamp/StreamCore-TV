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

/**
 * Creates Android DataStores for authentication, library, search history and playback progress.
 * In-memory mode returns memory-only stores instead.
 *
 * Persistent filenames start with `sdk_<hex(authFileName)>_<hex(backend)>_<hex(storageNamespace)>_`.
 * [authFileName] names the auth store and also separates providers across all four stores.
 * The returned [SdkPlatformStorage] owns the I/O scope and reserves the files against duplicate
 * instances. Close it to cancel storage work and release reservations without deleting saved data.
 */
fun createAndroidSdkStorage(
    context: Context,
    config: StreamCoreConfiguration,
    authFileName: String,
): SdkPlatformStorage {
    if (config.persistence == StreamCorePersistenceMode.InMemory) return SdkPlatformStorage.inMemory()
    val appContext = context.applicationContext
    val prefix = "sdk_${encodeStorageName(authFileName)}_${encodeStorageName(config.backend)}_${
        encodeStorageName(config.storageNamespace)
    }_"
    val names = listOf(
        authFileName,
        "library.preferences_pb",
        "search_history.preferences_pb",
        "playback_progress.preferences_pb"
    )
    // preferencesDataStoreFile appends .preferences_pb; keep the established doubled suffix.
    val files = names.map { appContext.preferencesDataStoreFile(prefix + it).canonicalFile }
    StorageOwners.acquire(files)
    val job = SupervisorJob()
    val scope = CoroutineScope(Dispatchers.IO + job)
    return try {
        val stores = files.map { file ->
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file })
        }
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

/** Encodes UTF-8 bytes as filename-safe lowercase hex; for example, `tmdb` becomes `746d6462`. */
private fun encodeStorageName(value: String): String {
    return value.encodeToByteArray()
        .joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
}

/** Prevents this process from opening the same files through two factory-owned storage instances. */
private object StorageOwners {
    private val files = mutableSetOf<String>()

    /**
     * Reserves all supplied file paths atomically; callers pass canonical files.
     * Throws if any path is already owned, leaving the reservations unchanged.
     */
    @Synchronized
    fun acquire(requested: List<File>) {
        check(requested.none { it.path in files }) { "An SDK instance already owns this storage namespace." }
        files.addAll(requested.map { it.path })
    }

    /**
     * Releases reservations when storage work finishes or construction fails.
     * Unreserved paths are ignored; this only updates memory and never deletes files.
     */
    @Synchronized
    fun release(requested: List<File>) {
        files.removeAll(requested.map { it.path }.toSet())
    }
}
