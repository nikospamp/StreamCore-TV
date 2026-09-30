package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinUiState

data class ProfilesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val pendingSelectionProfileId: String? = null,
    val profiles: List<StreamCoreProfile> = emptyList(),
    val pendingDeleteProfile: StreamCoreProfile? = null,
    val mode: ProfilesMode = ProfilesMode.Selection,
    val loadError: StreamCoreError? = null,
    val pin: ProfilePinUiState? = null,
    val restoreFocusProfileId: String? = null,
)

