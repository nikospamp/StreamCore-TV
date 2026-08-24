package com.pampoukidis.streamcoretv.feature.library.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcoretv.core.domain.LibraryRepository
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.core.model.library.LibraryEntryModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesLibraryRepository @Inject constructor(
    @param:LibraryStore private val dataStore: DataStore<Preferences>,
    @param:LibraryJson private val json: Json,
) : LibraryRepository {

    override fun observe(profileId: String): Flow<AppResult<List<LibraryEntryModel>>> {
        if (profileId.isBlank()) {
            return flowOf(AppResult.Success(emptyList()))
        }

        return dataStore.data
            .map<Preferences, AppResult<List<LibraryEntryModel>>> { preferences ->
                val entries = decode(preferences[EntriesKey])
                    .entriesByProfile[profileId]
                    .orEmpty()
                    .sortedWith(
                        compareByDescending<LibraryEntryPreferences> { entry -> entry.lastChangedAtMillis() }
                            .thenBy { entry -> entry.content.id },
                    )
                    .map(LibraryEntryPreferences::toModel)
                AppResult.Success(entries)
            }
            .catch { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }

                emit(AppResult.Failure(throwable.toAppError(ObserveOperation)))
            }
    }

    override suspend fun setLiked(
        profileId: String,
        content: ContentModel,
        isLiked: Boolean,
        changedAtMillis: Long,
    ): AppResult<Unit> {
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
        content: ContentModel,
        isInMyList: Boolean,
        changedAtMillis: Long,
    ): AppResult<Unit> {
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
        content: ContentModel,
        isIncluded: Boolean,
        changedAtMillis: Long,
        membership: Membership,
        operation: String,
    ): AppResult<Unit> {
        if (profileId.isBlank() || content.id.isBlank()) {
            return AppResult.Failure(
                AppError.Unknown(source = ErrorSource(operation = "$operation.invalidInput")),
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
            AppResult.Success(Unit)
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable.toAppError(operation))
        }
    }

    private fun List<LibraryEntryPreferences>.updateMembership(
        content: ContentModel,
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

    private fun Throwable.toAppError(operation: String): AppError {
        val source = ErrorSource(operation = operation)
        return if (this is SerializationException) {
            AppError.Parsing(source = source)
        } else {
            AppError.Unknown(source = source)
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
