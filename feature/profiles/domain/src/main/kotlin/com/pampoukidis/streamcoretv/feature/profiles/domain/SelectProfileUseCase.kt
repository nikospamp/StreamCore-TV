package com.pampoukidis.streamcoretv.feature.profiles.domain

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult

class SelectProfileUseCase constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(profileId: String): AppResult<ProfileModel> {
        return profileRepository.selectProfile(profileId)
    }
}
