package com.pampoukidis.streamcoretv.feature.profiles.domain

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.UpdateProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(draft: ProfileDraftModel): AppResult<ProfileModel> {
        return draft.profileId?.let { profileId ->
            profileRepository.updateProfile(
                UpdateProfileModel(
                    profileId = profileId,
                    displayName = draft.displayName.trim(),
                    avatarId = draft.avatarId,
                    parentalLevelId = draft.parentalLevelId,
                ),
            )
        } ?: AppResult.Failure(AppError.Unknown())
    }
}
