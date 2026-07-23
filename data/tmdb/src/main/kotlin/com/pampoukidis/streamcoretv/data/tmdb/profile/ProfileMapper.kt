package com.pampoukidis.streamcoretv.data.tmdb.profile

import com.pampoukidis.streamcoretv.core.model.auth.CreateProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileParentalLevelModel
import com.pampoukidis.streamcoretv.core.model.auth.UpdateProfileModel
import com.pampoukidis.streamcoretv.data.tmdb.model.CreateProfileRequestDto
import com.pampoukidis.streamcoretv.data.tmdb.model.ProfileAvatarDto
import com.pampoukidis.streamcoretv.data.tmdb.model.ProfileDto
import com.pampoukidis.streamcoretv.data.tmdb.model.ProfileParentalLevelDto
import com.pampoukidis.streamcoretv.data.tmdb.model.UpdateProfileRequestDto

internal fun ProfileDto.toModel(): ProfileModel {
    return ProfileModel(
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
            isKids = isKidsProfile,
        ),
        canDelete = canDelete,
        isKidsProfile = isKidsProfile,
    )
}

internal fun ProfileAvatarDto.toModel(): ProfileAvatarModel {
    return ProfileAvatarModel(
        id = id,
        imageUrl = imageUrl,
    )
}

internal fun ProfileParentalLevelDto.toModel(): ProfileParentalLevelModel {
    return ProfileParentalLevelModel(
        id = id,
        label = label,
        rank = rank,
        isKids = isKids,
    )
}

internal fun CreateProfileModel.toDto(): CreateProfileRequestDto {
    return CreateProfileRequestDto(
        displayName = displayName,
        avatarId = avatarId,
        parentalLevelId = parentalLevelId,
    )
}

internal fun UpdateProfileModel.toDto(): UpdateProfileRequestDto {
    return UpdateProfileRequestDto(
        profileId = profileId,
        displayName = displayName,
        avatarId = avatarId,
        parentalLevelId = parentalLevelId,
    )
}
