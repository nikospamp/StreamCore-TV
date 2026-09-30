package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError

sealed interface ProfilesEffect {
    data object EntryStarted : ProfilesEffect
    data class ProfileSelected(val profile: StreamCoreProfile) : ProfilesEffect
    data class ShowError(val error: StreamCoreError) : ProfilesEffect
}

