package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileValidationResult

data class ProfileEditorScreenUiState(
    val mode: ProfileEditorMode = ProfileEditorMode.Create,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val editorOptions: ProfileEditorOptionsModel? = null,
    val editor: ProfileEditorUiState? = null,
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


