package com.pampoukidis.streamcore.sdk.runtime.storage.library

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcore.sdk.runtime.storage.library.LibraryStore
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import com.pampoukidis.streamcore.sdk.model.library.StreamCoreLibraryEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class PreferencesLibraryRepository constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : LibraryStore {

    override fun observe(profileId: String): Flow<StreamCoreResult<List<StreamCoreLibraryEntry>>> {
        if (profileId.isBlank()) {
            return flowOf(StreamCoreResult.Success(emptyList()))
        }

        return dataStore.data
            .map<Preferences, StreamCoreResult<List<StreamCoreLibraryEntry>>> { preferences ->
                val entries = decode(preferences[EntriesKey])
                    .entriesByProfile[profileId]
                    .orEmpty()
                    .sortedWith(
                        compareByDescending<LibraryEntryPreferences> { entry -> entry.lastChangedAtMillis() }
                            .thenBy { entry -> entry.content.id },
                    )
                    .map(LibraryEntryPreferences::toModel)
                StreamCoreResult.Success(entries)
            }
            .catch { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }

                emit(StreamCoreResult.Failure(throwable.toAppError(ObserveOperation)))
            }
    }

    override suspend fun setLiked(
        profileId: String,
        content: StreamCoreContent,
        isLiked: Boolean,
        changedAtMillis: Long,
    ): StreamCoreResult<Unit> {
        return updateMembership(
            profileId = profileId,
            content = content,
            isIncluded = isLiked,
            changedAtMillis = changedAtMillis,
            membership = Membership.Liked,
            operation = SetLikedOperation,
        )
    }

    override suspend fun setInMyList(
        profileId: String,
        content: StreamCoreContent,
        isInMyList: Boolean,
        changedAtMillis: Long,
    ): StreamCoreResult<Unit> {
        return updateMembership(
            profileId = profileId,
            content = content,
            isIncluded = isInMyList,
            changedAtMillis = changedAtMillis,
            membership = Membership.MyList,
            operation = SetMyListOperation,
        )
    }

    private suspend fun updateMembership(
        profileId: String,
        content: StreamCoreContent,
        isIncluded: Boolean,
        changedAtMillis: Long,
        membership: Membership,
        operation: String,
    ): StreamCoreResult<Unit> {
        if (profileId.isBlank() || content.id.isBlank()) {
            return StreamCoreResult.Failure(
                StreamCoreError.Unknown(source = StreamCoreErrorSource(operation = "$operation.invalidInput")),
            )
        }

        return try {
            dataStore.edit { preferences ->
                val current = decode(preferences[EntriesKey])
                val profileEntries = current.entriesByProfile[profileId].orEmpty()
                val updatedEntries = profileEntries.updateMembership(
                    content = content,
                    isIncluded = isIncluded,
                    changedAtMillis = changedAtMillis,
                    membership = membership,
                )

                if (updatedEntries == profileEntries) {
                    return@edit
                }

                val updatedByProfile = if (updatedEntries.isEmpty()) {
                    current.entriesByProfile - profileId
                } else {
                    current.entriesByProfile + (profileId to updatedEntries)
                }
                preferences[EntriesKey] = json.encodeToString(
                    current.copy(entriesByProfile = updatedByProfile),
                )
            }
            StreamCoreResult.Success(Unit)
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            StreamCoreResult.Failure(throwable.toAppError(operation))
        }
    }

    private fun List<LibraryEntryPreferences>.updateMembership(
        content: StreamCoreContent,
        isIncluded: Boolean,
        changedAtMillis: Long,
        membership: Membership,
    ): List<LibraryEntryPreferences> {
        val existingIndex = indexOfFirst { entry -> entry.content.id == content.id }
        val existing = getOrNull(existingIndex)
        if (existing == null && !isIncluded) {
            return this
        }

        val updated = when (membership) {
            Membership.Liked -> LibraryEntryPreferences(
                content = if (isIncluded) content.toLibraryPreferences() else existing!!.content,
                likedAtMillis = when {
                    !isIncluded -> null
                    existing?.likedAtMillis != null -> existing.likedAtMillis
                    else -> changedAtMillis
                },
                addedToMyListAtMillis = existing?.addedToMyListAtMillis,
            )

            Membership.MyList -> LibraryEntryPreferences(
                content = if (isIncluded) content.toLibraryPreferences() else existing!!.content,
                likedAtMillis = existing?.likedAtMillis,
                addedToMyListAtMillis = when {
                    !isIncluded -> null
                    existing?.addedToMyListAtMillis != null -> existing.addedToMyListAtMillis
                    else -> changedAtMillis
                },
            )
        }

        if (updated.likedAtMillis == null && updated.addedToMyListAtMillis == null) {
            return filterIndexed { index, _ -> index != existingIndex }
        }

        if (existingIndex < 0) {
            return this + updated
        }

        return mapIndexed { index, entry ->
            if (index == existingIndex) updated else entry
        }
    }

    private fun decode(encoded: String?): LibraryPreferences {
        if (encoded.isNullOrBlank()) {
            return LibraryPreferences()
        }

        return json.decodeFromString<LibraryPreferences>(encoded)
    }

    private fun LibraryEntryPreferences.lastChangedAtMillis(): Long {
        return maxOf(
            likedAtMillis ?: Long.MIN_VALUE,
            addedToMyListAtMillis ?: Long.MIN_VALUE,
        )
    }

    private fun Throwable.toAppError(operation: String): StreamCoreError {
        if (this is com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProviderOperationException) return error
        val source = StreamCoreErrorSource(operation = operation)
        return if (this is SerializationException) {
            StreamCoreError.Parsing(source = source)
        } else {
            StreamCoreError.Unknown(source = source)
        }
    }

    private enum class Membership {
        Liked,
        MyList,
    }

    private companion object {
        val EntriesKey = stringPreferencesKey("library_json")
        const val ObserveOperation = "library.observe"
        const val SetLikedOperation = "library.setLiked"
        const val SetMyListOperation = "library.setInMyList"
    }
}
