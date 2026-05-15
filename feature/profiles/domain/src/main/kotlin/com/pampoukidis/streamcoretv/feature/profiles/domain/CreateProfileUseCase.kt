package com.pampoukidis.streamcoretv.feature.profiles.domain

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.CreateProfileModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import javax.inject.Inject

class CreateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(draft: ProfileDraftModel): AppResult<ProfileModel> {
        return profileRepository.createProfile(
            CreateProfileModel(
                displayName = draft.displayName.trim(),
                avatarId = draft.avatarId,
                parentalLevelId = draft.parentalLevelId,
            )
        )
    }
}
