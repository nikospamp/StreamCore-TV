package com.pampoukidis.streamcoretv.feature.profiles.data

sealed interface ProfileFieldError {
    data object Blank : ProfileFieldError
    data object TooLong : ProfileFieldError
    data object MissingSelection : ProfileFieldError
    data object UnknownSelection : ProfileFieldError
}
