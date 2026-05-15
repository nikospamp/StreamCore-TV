package com.pampoukidis.streamcoretv.feature.profiles.domain

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import javax.inject.Inject

class LoadProfileEditorOptionsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke() = profileRepository.getProfileEditorOptions()
}
