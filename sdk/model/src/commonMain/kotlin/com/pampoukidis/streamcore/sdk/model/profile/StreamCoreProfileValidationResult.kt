package com.pampoukidis.streamcore.sdk.model.profile

data class StreamCoreProfileValidationResult(
    val displayNameError: StreamCoreProfileFieldError? = null,
    val avatarError: StreamCoreProfileFieldError? = null,
    val parentalLevelError: StreamCoreProfileFieldError? = null,
) {
    val isValid: Boolean =
        displayNameError == null &&
            avatarError == null &&
            parentalLevelError == null
}
