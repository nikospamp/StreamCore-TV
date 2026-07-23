package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileEditorMode
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileValidationResult

data class ProfileEditorFormUiState(
    val mode: ProfileEditorMode,
    val draft: ProfileDraftModel,
    val initialDraft: ProfileDraftModel = draft,
    val validation: ProfileValidationResult = ProfileValidationResult(),
) {
    val hasChanges: Boolean
        get() = draft != initialDraft
}