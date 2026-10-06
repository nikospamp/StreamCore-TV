package com.pampoukidis.streamcore.sdk.runtime.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.runtime.error.ProviderOperationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.SerializationException

internal class ErrorMappingPreferencesStore(private val delegate: DataStore<Preferences>) :
    DataStore<Preferences> {
    override val data: Flow<Preferences> = delegate.data.catch { failure ->
        if (failure is CancellationException) throw failure
        throw ProviderOperationException(StreamCoreError.Storage())
    }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        return try {
            delegate.updateData(transform)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: SerializationException) {
            throw failure
        } catch (_: Exception) {
            throw ProviderOperationException(StreamCoreError.Storage())
        }
    }
}
