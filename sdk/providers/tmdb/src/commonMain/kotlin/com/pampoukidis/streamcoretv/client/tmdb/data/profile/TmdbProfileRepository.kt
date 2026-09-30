package com.pampoukidis.streamcoretv.client.tmdb.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.pampoukidis.streamcoretv.client.tmdb.data.model.ProfileAvatarDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.ProfileDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.ProfileParentalLevelDto
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProfileProvider
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreUpdateProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class TmdbProfileRepository constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    accountId: String,
) : ProfileProvider {

    private val profilesPreferencesKey = tmdbProfilesPreferencesKey(accountId)
    private val avatars = TmdbProfileAvatarCatalog.avatars

    private val parentalLevels = listOf(
        ProfileParentalLevelDto(id = "all", label = "All maturity", rank = 100, isKids = false),
        ProfileParentalLevelDto(id = "teen", label = "Teen", rank = 60, isKids = false),
        ProfileParentalLevelDto(id = "kids", label = "Kids", rank = 20, isKids = true),
    )

    override suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>> {
        return readSnapshot(GET_PROFILES_OPERATION) { snapshot ->
            snapshot.profiles.map(ProfileDto::toModel)
        }
    }

    // Resolve each operation's policy from the validated, account-scoped profile snapshot.
    internal suspend fun getContentPolicy(profileId: String): StreamCoreResult<TmdbProfileContentPolicy> {
        return readSnapshot(GET_CONTENT_POLICY_OPERATION) { snapshot ->
            val profile = snapshot.profiles.firstOrNull { it.id == profileId }
                ?: return@readSnapshot profileFailure(
                    GET_CONTENT_POLICY_OPERATION,
                    "PROFILE_NOT_FOUND",
                )
            StreamCoreResult.Success(TmdbProfileContentPolicy(includeAdult = !profile.isKidsProfile))
        }.flatten()
    }

    override suspend fun getProfileEditorOptions(): StreamCoreResult<StreamCoreProfileEditorOptions> {
        return StreamCoreResult.Success(
            StreamCoreProfileEditorOptions(
                avatars = avatars.map { it.toModel() },
                parentalLevels = parentalLevels.map { it.toModel() },
            ),
        )
    }

    override suspend fun createProfile(profile: StreamCoreCreateProfile): StreamCoreResult<StreamCoreProfile> {
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
                result = StreamCoreResult.Success(created.toModel()),
            )
        }
    }

    override suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile> {
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
                result = StreamCoreResult.Success(updatedProfile.toModel()),
            )
        }
    }

    override suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit> {
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
                result = StreamCoreResult.Success(Unit),
            )
        }
    }

    override suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfile> {
        return readSnapshot(SELECT_PROFILE_OPERATION) { snapshot ->
            val profile = snapshot.profiles.firstOrNull { it.id == profileId }
                ?: return@readSnapshot profileFailure(
                    SELECT_PROFILE_OPERATION,
                    "PROFILE_NOT_FOUND",
                )
            StreamCoreResult.Success(profile.toModel())
        }.flatten()
    }

    override suspend fun verifyProfilePin(profileId: String, pin: String): StreamCoreResult<Unit> {
        return StreamCoreResult.Failure(StreamCoreError.Unsupported("profiles.verifyPin"))
    }

    private suspend fun <T> readSnapshot(
        operation: String,
        transform: (TmdbProfilesPreferences) -> T,
    ): StreamCoreResult<T> {
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
            StreamCoreResult.Success(value as T)
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            StreamCoreResult.Failure(throwable.toStorageError(operation))
        }
    }

    private suspend fun <T> mutateSnapshot(
        operation: String,
        transform: (TmdbProfilesPreferences) -> ProfileMutation<T>,
    ): StreamCoreResult<T> {
        return try {
            var result: StreamCoreResult<T>? = null
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
            StreamCoreResult.Failure(throwable.toStorageError(operation))
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

    private fun Throwable.toStorageError(operation: String): StreamCoreError {
        val isCorruption = this is SerializationException || this is IllegalArgumentException
        val source = StreamCoreErrorSource(
            client = CLIENT,
            operation = operation,
            backendCode = if (isCorruption) PROFILE_STORAGE_CORRUPTED else PROFILE_STORAGE_FAILURE,
        )
        return if (isCorruption) {
            StreamCoreError.Parsing(source = source)
        } else {
            StreamCoreError.Unknown(source = source)
        }
    }

    private fun <T> profileFailure(
        operation: String,
        backendCode: String,
    ): StreamCoreResult<T> {
        return StreamCoreResult.Failure(
            StreamCoreError.Unknown(
                source = StreamCoreErrorSource(
                    client = CLIENT,
                    operation = operation,
                    backendCode = backendCode,
                ),
            ),
        )
    }

    private fun <T> StreamCoreResult<StreamCoreResult<T>>.flatten(): StreamCoreResult<T> {
        return when (this) {
            is StreamCoreResult.Failure -> this
            is StreamCoreResult.Success -> value
        }
    }

    private data class ProfileMutation<T>(
        val updated: TmdbProfilesPreferences? = null,
        val result: StreamCoreResult<T>,
    )

    private companion object {
        const val CLIENT = "tmdb"
        const val GET_CONTENT_POLICY_OPERATION = "getContentPolicy"
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
