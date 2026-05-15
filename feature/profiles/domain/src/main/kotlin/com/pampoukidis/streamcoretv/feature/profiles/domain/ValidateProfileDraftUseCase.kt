package com.pampoukidis.streamcoretv.feature.profiles.domain

import com.pampoukidis.streamcoretv.core.model.auth.ProfileEditorOptionsModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileDraftModel
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileFieldError
import com.pampoukidis.streamcoretv.feature.profiles.data.ProfileValidationResult
import javax.inject.Inject

class ValidateProfileDraftUseCase @Inject constructor() {

    operator fun invoke(
        draft: ProfileDraftModel,
        options: ProfileEditorOptionsModel?,
    ): ProfileValidationResult {
        val displayName = draft.displayName.trim()
        val avatarIds = options?.avatars.orEmpty().map { it.id }.toSet()
        val parentalLevelIds = options?.parentalLevels.orEmpty().map { it.id }.toSet()

        return ProfileValidationResult(
            displayNameError = when {
                displayName.isBlank() -> ProfileFieldError.Blank
                displayName.length > MaxDisplayNameLength -> ProfileFieldError.TooLong
                else -> null
            },
            avatarError = when {
                draft.avatarId.isBlank() -> ProfileFieldError.MissingSelection
                options != null && draft.avatarId !in avatarIds -> ProfileFieldError.UnknownSelection
                else -> null
            },
            parentalLevelError = when {
                draft.parentalLevelId.isBlank() -> ProfileFieldError.MissingSelection
                options != null && draft.parentalLevelId !in parentalLevelIds -> ProfileFieldError.UnknownSelection
                else -> null
            },
        )
    }

    private companion object {
        const val MaxDisplayNameLength = 32
    }
}
