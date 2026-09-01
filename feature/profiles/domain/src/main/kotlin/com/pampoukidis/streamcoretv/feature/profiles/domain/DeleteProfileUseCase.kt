package com.pampoukidis.streamcoretv.feature.profiles.domain

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.error.AppResult

class DeleteProfileUseCase constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(profileId: String): AppResult<Unit> {
        return profileRepository.deleteProfile(profileId)
    }
}
