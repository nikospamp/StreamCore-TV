package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.core.model.error.AppError

sealed interface ProfilesEffect {
    data class ProfileSelected(val profile: ProfileModel) : ProfilesEffect
    data class ShowError(val error: AppError) : ProfilesEffect
}

