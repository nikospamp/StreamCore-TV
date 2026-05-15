package com.pampoukidis.streamcoretv.feature.profiles.domain

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import javax.inject.Inject

class LoadProfileEditorOptionsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository,
) {

    suspend operator fun invoke(): AppResult<ProfileEditorOptionsModel> {
        return profileRepository.getProfileEditorOptions()
    }
}
