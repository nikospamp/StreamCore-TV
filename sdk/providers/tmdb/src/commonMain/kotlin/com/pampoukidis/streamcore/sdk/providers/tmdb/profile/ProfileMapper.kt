package com.pampoukidis.streamcore.sdk.providers.tmdb.profile

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileAvatar
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileParentalLevel
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreUpdateProfile

internal fun ProfileDto.toModel(): StreamCoreProfile {
    return StreamCoreProfile(
        id = id,
        displayName = displayName,
        avatar = StreamCoreProfileAvatar(
            id = avatarId,
            imageUrl = avatarUrl,
        ),
        parentalLevel = StreamCoreProfileParentalLevel(
            id = parentalLevelId,
            label = parentalLevelLabel,
            rank = parentalLevelRank,
            isKids = isKidsProfile,
        ),
        canDelete = canDelete,
        isKidsProfile = isKidsProfile,
    )
}

internal fun ProfileAvatarDto.toModel(): StreamCoreProfileAvatar {
    return StreamCoreProfileAvatar(
        id = id,
        imageUrl = imageUrl,
    )
}

internal fun ProfileParentalLevelDto.toModel(): StreamCoreProfileParentalLevel {
    return StreamCoreProfileParentalLevel(
        id = id,
        label = label,
        rank = rank,
        isKids = isKids,
    )
}

internal fun StreamCoreCreateProfile.toDto(): CreateProfileRequestDto {
    return CreateProfileRequestDto(
        displayName = displayName,
        avatarId = avatarId,
        parentalLevelId = parentalLevelId,
    )
}

internal fun StreamCoreUpdateProfile.toDto(): UpdateProfileRequestDto {
    return UpdateProfileRequestDto(
        profileId = profileId,
        displayName = displayName,
        avatarId = avatarId,
        parentalLevelId = parentalLevelId,
    )
}
