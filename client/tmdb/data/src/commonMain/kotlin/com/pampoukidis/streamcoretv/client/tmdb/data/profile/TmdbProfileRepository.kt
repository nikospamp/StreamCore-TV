package com.pampoukidis.streamcoretv.client.tmdb.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.pampoukidis.streamcoretv.client.tmdb.data.model.ProfileAvatarDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.ProfileDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.ProfileParentalLevelDto
import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.CreateProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.UpdateProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class TmdbProfileRepository constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    accountId: String,
) : ProfileRepository {

    private val profilesPreferencesKey = tmdbProfilesPreferencesKey(accountId)
    private val avatars = TmdbProfileAvatarCatalog.avatars

    private val parentalLevels = listOf(
        ProfileParentalLevelDto(id = "all", label = "All maturity", rank = 100, isKids = false),
        ProfileParentalLevelDto(id = "teen", label = "Teen", rank = 60, isKids = false),
        ProfileParentalLevelDto(id = "kids", label = "Kids", rank = 20, isKids = true),
    )

    override suspend fun getProfiles(): AppResult<List<ProfileModel>> {
        return readSnapshot(GET_PROFILES_OPERATION) { snapshot ->
            snapshot.profiles.map(ProfileDto::toModel)
        }
    }

    override suspend fun getProfileEditorOptions(): AppResult<ProfileEditorOptionsModel> {
        return AppResult.Success(
            ProfileEditorOptionsModel(
                avatars = avatars.map { it.toModel() },
                parentalLevels = parentalLevels.map { it.toModel() },
            ),
        )
    }

    override suspend fun createProfile(profile: CreateProfileModel): AppResult<ProfileModel> {
        val avatar = findAvatar(profile.avatarId)
            ?: return profileFailure(CREATE_PROFILE_OPERATION, "AVATAR_NOT_FOUND")
        val parentalLevel = findParentalLevel(profile.parentalLevelId)
            ?: return profileFailure(CREATE_PROFILE_OPERATION, "PARENTAL_LEVEL_NOT_FOUND")

        return mutateSnapshot(CREATE_PROFILE_OPERATION) { snapshot ->
            if (snapshot.nextProfileNumber == Long.MAX_VALUE) {
                return@mutateSnapshot ProfileMutation(
                    result = profileFailure(CREATE_PROFILE_OPERATION, "PROFILE_ID_EXHAUSTED"),
                )
            }

            val request = profile.toDto()
            val created = ProfileDto(
                id = "$CREATED_PROFILE_ID_PREFIX${snapshot.nextProfileNumber}",
                displayName = request.displayName,
                avatarId = avatar.id,
                avatarUrl = avatar.imageUrl,
                parentalLevelId = parentalLevel.id,
                parentalLevelLabel = parentalLevel.label,
                parentalLevelRank = parentalLevel.rank,
                canDelete = true,
                isKidsProfile = parentalLevel.id == KIDS_PARENTAL_LEVEL_ID,
            )
            ProfileMutation(
                updated = snapshot.copy(
                    nextProfileNumber = snapshot.nextProfileNumber + 1L,
                    profiles = snapshot.profiles + created,
                ),
                result = AppResult.Success(created.toModel()),
            )
        }
    }

    override suspend fun updateProfile(profile: UpdateProfileModel): AppResult<ProfileModel> {
        val avatar = findAvatar(profile.avatarId)
            ?: return profileFailure(UPDATE_PROFILE_OPERATION, "AVATAR_NOT_FOUND")
        val parentalLevel = findParentalLevel(profile.parentalLevelId)
            ?: return profileFailure(UPDATE_PROFILE_OPERATION, "PARENTAL_LEVEL_NOT_FOUND")

        return mutateSnapshot(UPDATE_PROFILE_OPERATION) { snapshot ->
            val index = snapshot.profiles.indexOfFirst { it.id == profile.profileId }
            if (index == -1) {
                return@mutateSnapshot ProfileMutation(
                    result = profileFailure(UPDATE_PROFILE_OPERATION, "PROFILE_NOT_FOUND"),
                )
            }

            val request = profile.toDto()
            val updatedProfile = snapshot.profiles[index].copy(
                displayName = request.displayName,
                avatarId = avatar.id,
                avatarUrl = avatar.imageUrl,
                parentalLevelId = parentalLevel.id,
                parentalLevelLabel = parentalLevel.label,
                parentalLevelRank = parentalLevel.rank,
                isKidsProfile = parentalLevel.id == KIDS_PARENTAL_LEVEL_ID,
            )
            ProfileMutation(
                updated = snapshot.copy(
                    profiles = snapshot.profiles.mapIndexed { profileIndex, current ->
                        if (profileIndex == index) updatedProfile else current
                    },
                ),
                result = AppResult.Success(updatedProfile.toModel()),
            )
        }
    }

    override suspend fun deleteProfile(profileId: String): AppResult<Unit> {
        return mutateSnapshot(DELETE_PROFILE_OPERATION) { snapshot ->
            val profile = snapshot.profiles.firstOrNull { it.id == profileId }
                ?: return@mutateSnapshot ProfileMutation(
                    result = profileFailure(DELETE_PROFILE_OPERATION, "PROFILE_NOT_FOUND"),
                )

            if (!profile.canDelete) {
                return@mutateSnapshot ProfileMutation(
                    result = profileFailure(DELETE_PROFILE_OPERATION, "PROFILE_LOCKED"),
                )
            }

            ProfileMutation(
                updated = snapshot.copy(
                    profiles = snapshot.profiles.filterNot { it.id == profileId },
                ),
                result = AppResult.Success(Unit),
            )
        }
    }

    override suspend fun selectProfile(profileId: String): AppResult<ProfileModel> {
        return readSnapshot(SELECT_PROFILE_OPERATION) { snapshot ->
            val profile = snapshot.profiles.firstOrNull { it.id == profileId }
                ?: return@readSnapshot profileFailure(
                    SELECT_PROFILE_OPERATION,
                    "PROFILE_NOT_FOUND",
                )
            AppResult.Success(profile.toModel())
        }.flatten()
    }

    private suspend fun <T> readSnapshot(
        operation: String,
        transform: (TmdbProfilesPreferences) -> T,
    ): AppResult<T> {
        return try {
            var value: T? = null
            var valueWasSet = false
            dataStore.edit { preferences ->
                val encoded = preferences[profilesPreferencesKey]
                val snapshot = decodeSnapshot(encoded)
                if (encoded == null) {
                    preferences[profilesPreferencesKey] = json.encodeToString(snapshot)
                }
                value = transform(snapshot)
                valueWasSet = true
            }
            check(valueWasSet)
            @Suppress("UNCHECKED_CAST")
            AppResult.Success(value as T)
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable.toStorageError(operation))
        }
    }

    private suspend fun <T> mutateSnapshot(
        operation: String,
        transform: (TmdbProfilesPreferences) -> ProfileMutation<T>,
    ): AppResult<T> {
        return try {
            var result: AppResult<T>? = null
            dataStore.edit { preferences ->
                val mutation = transform(decodeSnapshot(preferences[profilesPreferencesKey]))
                mutation.updated?.let { updated ->
                    preferences[profilesPreferencesKey] = json.encodeToString(updated)
                }
                result = mutation.result
            }
            checkNotNull(result)
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            AppResult.Failure(throwable.toStorageError(operation))
        }
    }

    private fun decodeSnapshot(encoded: String?): TmdbProfilesPreferences {
        if (encoded == null) {
            return defaultSnapshot()
        }

        val snapshot = json.decodeFromString<TmdbProfilesPreferences>(encoded)
        require(snapshot.schemaVersion == TMDB_PROFILES_SCHEMA_VERSION)
        require(snapshot.nextProfileNumber > 0L)
        require(snapshot.profiles.map(ProfileDto::id).distinct().size == snapshot.profiles.size)
        val createdProfileNumbers = snapshot.profiles
            .filter { profile -> profile.id.startsWith(CREATED_PROFILE_ID_PREFIX) }
            .map { profile ->
                requireNotNull(profile.id.removePrefix(CREATED_PROFILE_ID_PREFIX).toLongOrNull())
            }
        require(createdProfileNumbers.all { profileNumber ->
            profileNumber > 0L && profileNumber < snapshot.nextProfileNumber
        })
        return snapshot
    }

    private fun defaultSnapshot(): TmdbProfilesPreferences {
        return TmdbProfilesPreferences(
            schemaVersion = TMDB_PROFILES_SCHEMA_VERSION,
            nextProfileNumber = 1L,
            profiles = listOf(
                ProfileDto(
                    id = "tmdb-profile-owner",
                    displayName = "Nikos",
                    avatarId = avatars[0].id,
                    avatarUrl = avatars[0].imageUrl,
                    parentalLevelId = parentalLevels[0].id,
                    parentalLevelLabel = parentalLevels[0].label,
                    parentalLevelRank = parentalLevels[0].rank,
                    canDelete = false,
                    isKidsProfile = false,
                ),
                ProfileDto(
                    id = "tmdb-profile-kids",
                    displayName = "Kids",
                    avatarId = avatars[2].id,
                    avatarUrl = avatars[2].imageUrl,
                    parentalLevelId = parentalLevels[2].id,
                    parentalLevelLabel = parentalLevels[2].label,
                    parentalLevelRank = parentalLevels[2].rank,
                    canDelete = true,
                    isKidsProfile = true,
                ),
            ),
        )
    }

    private fun findAvatar(id: String): ProfileAvatarDto? {
        return avatars.firstOrNull { it.id == id }
    }

    private fun findParentalLevel(id: String): ProfileParentalLevelDto? {
        return parentalLevels.firstOrNull { it.id == id }
    }

    private fun Throwable.toStorageError(operation: String): AppError {
        val isCorruption = this is SerializationException || this is IllegalArgumentException
        val source = ErrorSource(
            client = CLIENT,
            operation = operation,
            backendCode = if (isCorruption) PROFILE_STORAGE_CORRUPTED else PROFILE_STORAGE_FAILURE,
        )
        return if (isCorruption) {
            AppError.Parsing(source = source)
        } else {
            AppError.Unknown(source = source)
        }
    }

    private fun <T> profileFailure(
        operation: String,
        backendCode: String,
    ): AppResult<T> {
        return AppResult.Failure(
            AppError.Unknown(
                source = ErrorSource(
                    client = CLIENT,
                    operation = operation,
                    backendCode = backendCode,
                ),
            ),
        )
    }

    private fun <T> AppResult<AppResult<T>>.flatten(): AppResult<T> {
        return when (this) {
            is AppResult.Failure -> this
            is AppResult.Success -> value
        }
    }

    private data class ProfileMutation<T>(
        val updated: TmdbProfilesPreferences? = null,
        val result: AppResult<T>,
    )

    private companion object {
        const val CLIENT = "tmdb"
        const val GET_PROFILES_OPERATION = "getProfiles"
        const val CREATE_PROFILE_OPERATION = "createProfile"
        const val UPDATE_PROFILE_OPERATION = "updateProfile"
        const val DELETE_PROFILE_OPERATION = "deleteProfile"
        const val SELECT_PROFILE_OPERATION = "selectProfile"
        const val PROFILE_STORAGE_CORRUPTED = "PROFILE_STORAGE_CORRUPTED"
        const val PROFILE_STORAGE_FAILURE = "PROFILE_STORAGE_FAILURE"
        const val CREATED_PROFILE_ID_PREFIX = "tmdb-profile-created-"
        const val KIDS_PARENTAL_LEVEL_ID = "kids"
    }
}
