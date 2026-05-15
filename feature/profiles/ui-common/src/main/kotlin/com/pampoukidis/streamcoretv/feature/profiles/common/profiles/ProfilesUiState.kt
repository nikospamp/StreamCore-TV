package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel

data class ProfilesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val pendingSelectionProfileId: String? = null,
    val profiles: List<ProfileModel> = emptyList(),
    val pendingDeleteProfile: ProfileModel? = null,
)

