package com.pampoukidis.streamcoretv.feature.profiles.data

data class ProfileValidationResult(
    val displayNameError: ProfileFieldError? = null,
    val avatarError: ProfileFieldError? = null,
    val parentalLevelError: ProfileFieldError? = null,
) {
    val isValid: Boolean =
        displayNameError == null &&
            avatarError == null &&
            parentalLevelError == null
}
