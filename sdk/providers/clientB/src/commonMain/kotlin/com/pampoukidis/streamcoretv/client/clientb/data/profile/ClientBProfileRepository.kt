package com.pampoukidis.streamcoretv.client.clientb.data.profile

import com.pampoukidis.streamcoretv.client.clientb.data.model.ProfileAvatarDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ProfileDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ProfileParentalLevelDto
import com.pampoukidis.streamcore.sdk.runtime.integration.provider.ProfileProvider
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreUpdateProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreErrorSource
import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBReferenceProfileScenario

internal class ClientBProfileRepository(
    private val scenario: ClientBReferenceProfileScenario = ClientBReferenceProfileScenario.Standard,
) : ProfileProvider {

    private val avatars = ClientBProfileAvatarCatalog.avatars

    private val parentalLevels = listOf(
        ProfileParentalLevelDto(id = "all", label = "All maturity", rank = 100, isKids = false),
        ProfileParentalLevelDto(id = "family", label = "Family", rank = 50, isKids = false),
        ProfileParentalLevelDto(id = "kids", label = "Kids", rank = 20, isKids = true),
    )

    private val profiles = initialProfiles()

    private fun initialProfiles(): MutableList<ProfileDto> {
        val protectedOwner = scenario == ClientBReferenceProfileScenario.SingleProtected ||
            scenario == ClientBReferenceProfileScenario.HouseholdProtected
        val result = mutableListOf(
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
                pinProtected = protectedOwner,
            ),
        )
        if (scenario == ClientBReferenceProfileScenario.Standard ||
            scenario == ClientBReferenceProfileScenario.HouseholdProtected
        ) {
            val kids = scenario == ClientBReferenceProfileScenario.HouseholdProtected
            val level = parentalLevels[if (kids) 2 else 1]
            result += ProfileDto(
                id = if (kids) "client-b-profile-kids" else "client-b-profile-family",
                displayName = if (kids) "Kids" else "Family",
                avatarId = avatars[1].id,
                avatarUrl = avatars[1].imageUrl,
                parentalLevelId = level.id,
                parentalLevelLabel = level.label,
                parentalLevelRank = level.rank,
                canDelete = true,
                isKidsProfile = kids,
            )
        }
        return result
    }

    private var nextProfileNumber = 1
    private var activeProfileId: String? = null

    override suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>> {
        return StreamCoreResult.Success(profiles.map { it.toModel() })
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
        return StreamCoreResult.Success(dto.toModel())
    }

    override suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile> {
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
        return StreamCoreResult.Success(updated.toModel())
    }

    override suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit> {
        val profile = profiles.firstOrNull { it.id == profileId }
            ?: return profileFailure(DELETE_PROFILE_OPERATION, "PROFILE_NOT_FOUND")

        if (!profile.canDelete) {
            return profileFailure(DELETE_PROFILE_OPERATION, "PROFILE_LOCKED")
        }

        profiles.remove(profile)
        if (activeProfileId == profileId) {
            activeProfileId = null
        }
        return StreamCoreResult.Success(Unit)
    }

    override suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfile> {
        val profile = profiles.firstOrNull { it.id == profileId }
            ?: return profileFailure(SELECT_PROFILE_OPERATION, "PROFILE_NOT_FOUND")

        activeProfileId = profile.id
        return StreamCoreResult.Success(profile.toModel())
    }

    override suspend fun verifyProfilePin(profileId: String, pin: String): StreamCoreResult<Unit> {
        val profile = profiles.firstOrNull { it.id == profileId }
            ?: return profileFailure(VERIFY_PIN_OPERATION, "PROFILE_NOT_FOUND")
        if (!profile.pinProtected) {
            return StreamCoreResult.Failure(StreamCoreError.Unsupported("profiles.verifyPin"))
        }
        if (pin != DEMONSTRATION_PIN) {
            return StreamCoreResult.Failure(
                StreamCoreError.PinRejected(
                    source = StreamCoreErrorSource(client = CLIENT, operation = VERIFY_PIN_OPERATION),
                ),
            )
        }
        return StreamCoreResult.Success(Unit)
    }

    private fun findAvatar(id: String): ProfileAvatarDto? {
        return avatars.firstOrNull { it.id == id }
    }

    private fun findParentalLevel(id: String): ProfileParentalLevelDto? {
        return parentalLevels.firstOrNull { it.id == id }
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

    private companion object {
        const val CLIENT = "clientB"
        const val CREATE_PROFILE_OPERATION = "createProfile"
        const val UPDATE_PROFILE_OPERATION = "updateProfile"
        const val DELETE_PROFILE_OPERATION = "deleteProfile"
        const val SELECT_PROFILE_OPERATION = "selectProfile"
        const val VERIFY_PIN_OPERATION = "verifyProfilePin"
        const val DEMONSTRATION_PIN = "1234"
        const val KIDS_PARENTAL_LEVEL_ID = "kids"
    }
}
