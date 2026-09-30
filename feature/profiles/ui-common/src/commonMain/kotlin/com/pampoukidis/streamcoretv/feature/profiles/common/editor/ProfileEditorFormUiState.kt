package com.pampoukidis.streamcoretv.feature.profiles.common.editor

import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.common.editor.ProfileEditorMode
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileValidationResult

data class ProfileEditorFormUiState(
    val mode: ProfileEditorMode,
    val draft: ProfileDraftModel,
    val initialDraft: ProfileDraftModel = draft,
    val validation: StreamCoreProfileValidationResult = StreamCoreProfileValidationResult(),
) {
    val hasChanges: Boolean
        get() = draft != initialDraft
}
