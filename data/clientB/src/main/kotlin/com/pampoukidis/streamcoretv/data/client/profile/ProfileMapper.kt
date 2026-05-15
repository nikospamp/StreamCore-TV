package com.pampoukidis.streamcoretv.data.client.profile

import com.pampoukidis.streamcoretv.core.model.auth.CreateProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileParentalLevelModel
import com.pampoukidis.streamcoretv.core.model.auth.UpdateProfileModel
import com.pampoukidis.streamcoretv.data.client.model.CreateProfileRequestDto
import com.pampoukidis.streamcoretv.data.client.model.ProfileAvatarDto
import com.pampoukidis.streamcoretv.data.client.model.ProfileDto
import com.pampoukidis.streamcoretv.data.client.model.ProfileParentalLevelDto
import com.pampoukidis.streamcoretv.data.client.model.UpdateProfileRequestDto

internal fun ProfileDto.toModel(): ProfileModel =
    ProfileModel(
        id = id,
        displayName = displayName,
        avatar = ProfileAvatarModel(
            id = avatarId,
            imageUrl = avatarUrl,
        ),
        parentalLevel = ProfileParentalLevelModel(
            id = parentalLevelId,
            label = parentalLevelLabel,
            rank = parentalLevelRank,
        ),
        canDelete = canDelete,
        isKidsProfile = isKidsProfile,
    )

internal fun ProfileAvatarDto.toModel(): ProfileAvatarModel =
    ProfileAvatarModel(
        id = id,
        imageUrl = imageUrl,
    )

internal fun ProfileParentalLevelDto.toModel(): ProfileParentalLevelModel =
    ProfileParentalLevelModel(
        id = id,
        label = label,
        rank = rank,
    )

internal fun CreateProfileModel.toDto(): CreateProfileRequestDto =
    CreateProfileRequestDto(
        displayName = displayName,
        avatarId = avatarId,
        parentalLevelId = parentalLevelId,
    )

internal fun UpdateProfileModel.toDto(): UpdateProfileRequestDto =
    UpdateProfileRequestDto(
        profileId = profileId,
        displayName = displayName,
        avatarId = avatarId,
        parentalLevelId = parentalLevelId,
    )
