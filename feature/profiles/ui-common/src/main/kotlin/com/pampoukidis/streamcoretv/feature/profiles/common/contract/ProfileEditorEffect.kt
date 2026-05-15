package com.pampoukidis.streamcoretv.feature.profiles.common.contract

import com.pampoukidis.streamcoretv.core.model.error.AppError

sealed interface ProfileEditorEffect {
    data object ProfileSaved : ProfileEditorEffect
    data object Close : ProfileEditorEffect
    data class ShowError(val error: AppError) : ProfileEditorEffect
}
