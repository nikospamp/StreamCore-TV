package com.pampoukidis.streamcoretv.data.client.profile

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.CreateProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.UpdateProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.core.model.error.ErrorSource
import com.pampoukidis.streamcoretv.data.client.model.ProfileAvatarDto
import com.pampoukidis.streamcoretv.data.client.model.ProfileDto
import com.pampoukidis.streamcoretv.data.client.model.ProfileParentalLevelDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientBProfileRepository @Inject constructor() : ProfileRepository {

    private val avatars = listOf(
        ProfileAvatarDto(id = "client-b-primary", imageUrl = null),
        ProfileAvatarDto(id = "client-b-sports", imageUrl = null),
        ProfileAvatarDto(id = "client-b-junior", imageUrl = null),
    )

    private val parentalLevels = listOf(
        ProfileParentalLevelDto(id = "all", label = "All maturity", rank = 100),
        ProfileParentalLevelDto(id = "family", label = "Family", rank = 50),
        ProfileParentalLevelDto(id = "kids", label = "Kids", rank = 20),
    )

    private val profiles = mutableListOf(
        ProfileDto(
            id = "client-b-profile-owner",
            displayName = "Primary",
            avatarId = avatars[0].id,
            avatarUrl = avatars[0].imageUrl,
            parentalLevelId = parentalLevels[0].id,
            parentalLevelLabel = parentalLevels[0].label,
            parentalLevelRank = parentalLevels[0].rank,
            canDelete = false,
            isKidsProfile = false,
        ),
        ProfileDto(
            id = "client-b-profile-family",
            displayName = "Family",
            avatarId = avatars[1].id,
            avatarUrl = avatars[1].imageUrl,
            parentalLevelId = parentalLevels[1].id,
            parentalLevelLabel = parentalLevels[1].label,
            parentalLevelRank = parentalLevels[1].rank,
            canDelete = true,
            isKidsProfile = false,
        ),
    )

    private var nextProfileNumber = 1
    private var activeProfileId: String? = null

    override suspend fun getProfiles(): AppResult<List<ProfileModel>> {
        return AppResult.Success(profiles.map { it.toModel() })
    }

    override suspend fun getProfileEditorOptions(): AppResult<ProfileEditorOptionsModel> =
        AppResult.Success(
            ProfileEditorOptionsModel(
                avatars = avatars.map { it.toModel() },
                parentalLevels = parentalLevels.map { it.toModel() },
            ),
        )

    override suspend fun createProfile(profile: CreateProfileModel): AppResult<ProfileModel> {
        val avatar = findAvatar(profile.avatarId)
            ?: return profileFailure(CREATE_PROFILE_OPERATION, "AVATAR_NOT_FOUND")
        val parentalLevel = findParentalLevel(profile.parentalLevelId)
            ?: return profileFailure(CREATE_PROFILE_OPERATION, "PARENTAL_LEVEL_NOT_FOUND")

        val request = profile.toDto()
        val dto = ProfileDto(
            id = "client-b-profile-created-${nextProfileNumber++}",
            displayName = request.displayName,
            avatarId = avatar.id,
            avatarUrl = avatar.imageUrl,
            parentalLevelId = parentalLevel.id,
            parentalLevelLabel = parentalLevel.label,
            parentalLevelRank = parentalLevel.rank,
            canDelete = true,
            isKidsProfile = parentalLevel.id == KIDS_PARENTAL_LEVEL_ID,
        )
        profiles += dto
        return AppResult.Success(dto.toModel())
    }

    override suspend fun updateProfile(profile: UpdateProfileModel): AppResult<ProfileModel> {
        val index = profiles.indexOfFirst { it.id == profile.profileId }
        if (index == -1) {
            return profileFailure(UPDATE_PROFILE_OPERATION, "PROFILE_NOT_FOUND")
        }

        val avatar = findAvatar(profile.avatarId)
            ?: return profileFailure(UPDATE_PROFILE_OPERATION, "AVATAR_NOT_FOUND")
        val parentalLevel = findParentalLevel(profile.parentalLevelId)
            ?: return profileFailure(UPDATE_PROFILE_OPERATION, "PARENTAL_LEVEL_NOT_FOUND")

        val request = profile.toDto()
        val updated = profiles[index].copy(
            displayName = request.displayName,
            avatarId = avatar.id,
            avatarUrl = avatar.imageUrl,
            parentalLevelId = parentalLevel.id,
            parentalLevelLabel = parentalLevel.label,
            parentalLevelRank = parentalLevel.rank,
            isKidsProfile = parentalLevel.id == KIDS_PARENTAL_LEVEL_ID,
        )
        profiles[index] = updated
        return AppResult.Success(updated.toModel())
    }

    override suspend fun deleteProfile(profileId: String): AppResult<Unit> {
        val profile = profiles.firstOrNull { it.id == profileId }
            ?: return profileFailure(DELETE_PROFILE_OPERATION, "PROFILE_NOT_FOUND")

        if (!profile.canDelete) {
            return profileFailure(DELETE_PROFILE_OPERATION, "PROFILE_LOCKED")
        }

        profiles.remove(profile)
        if (activeProfileId == profileId) {
            activeProfileId = null
        }
        return AppResult.Success(Unit)
    }

    override suspend fun selectProfile(profileId: String): AppResult<ProfileModel> {
        val profile = profiles.firstOrNull { it.id == profileId }
            ?: return profileFailure(SELECT_PROFILE_OPERATION, "PROFILE_NOT_FOUND")

        activeProfileId = profile.id
        return AppResult.Success(profile.toModel())
    }

    private fun findAvatar(id: String): ProfileAvatarDto? =
        avatars.firstOrNull { it.id == id }

    private fun findParentalLevel(id: String): ProfileParentalLevelDto? =
        parentalLevels.firstOrNull { it.id == id }

    private fun <T> profileFailure(
        operation: String,
        backendCode: String,
    ): AppResult<T> =
        AppResult.Failure(
            AppError.Unknown(
                source = ErrorSource(
                    client = CLIENT,
                    operation = operation,
                    backendCode = backendCode,
                ),
            ),
        )

    private companion object {
        const val CLIENT = "clientB"
        const val CREATE_PROFILE_OPERATION = "createProfile"
        const val UPDATE_PROFILE_OPERATION = "updateProfile"
        const val DELETE_PROFILE_OPERATION = "deleteProfile"
        const val SELECT_PROFILE_OPERATION = "selectProfile"
        const val KIDS_PARENTAL_LEVEL_ID = "kids"
    }
}
