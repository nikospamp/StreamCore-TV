package com.pampoukidis.streamcoretv.feature.profiles.domain

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult

class LoadProfilesUseCase constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(): AppResult<List<ProfileModel>> {
        return profileRepository.getProfiles()
    }
}
