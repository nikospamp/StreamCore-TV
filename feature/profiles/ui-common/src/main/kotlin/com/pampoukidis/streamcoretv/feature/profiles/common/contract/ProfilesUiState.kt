package com.pampoukidis.streamcoretv.feature.profiles.common.contract

import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.core.model.auth.ProfileModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileValidationResult

data class ProfilesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val pendingSelectionProfileId: String? = null,
    val profiles: List<ProfileModel> = emptyList(),
    val editorOptions: ProfileEditorOptionsModel? = null,
    val editor: ProfileEditorUiState? = null,
    val pendingDeleteProfile: ProfileModel? = null,
)

data class ProfileEditorUiState(
    val mode: ProfileEditorMode,
    val draft: ProfileDraftModel,
    val validation: ProfileValidationResult = ProfileValidationResult(),
)

enum class ProfileEditorMode {
    Create,
    Edit,
}
