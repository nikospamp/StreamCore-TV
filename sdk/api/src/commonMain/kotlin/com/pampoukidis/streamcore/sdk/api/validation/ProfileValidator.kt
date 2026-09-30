package com.pampoukidis.streamcore.sdk.api.validation

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEditorOptions
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileFieldError
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileValidationResult

/** Backend-free feedback for immutable create/update inputs; runtime operations validate against current provider options. */
object ProfileValidator {
    /** Null options skip catalogue-membership checks, not required-field/name-length validation. */
    fun validate(profile: StreamCoreCreateProfile, options: StreamCoreProfileEditorOptions?): StreamCoreProfileValidationResult {
        return StreamCoreProfileValidationResult(
            displayNameError = when {
                profile.displayName.trim().isBlank() -> StreamCoreProfileFieldError.Blank
                profile.displayName.trim().length > 32 -> StreamCoreProfileFieldError.TooLong
                else -> null
            },
            avatarError = when {
                profile.avatarId.isBlank() -> StreamCoreProfileFieldError.MissingSelection
                options != null && options.avatars.none { it.id == profile.avatarId } -> StreamCoreProfileFieldError.UnknownSelection
                else -> null
            },
            parentalLevelError = when {
                profile.parentalLevelId.isBlank() -> StreamCoreProfileFieldError.MissingSelection
                options != null && options.parentalLevels.none { it.id == profile.parentalLevelId } -> StreamCoreProfileFieldError.UnknownSelection
                else -> null
            },
        )
    }
}
