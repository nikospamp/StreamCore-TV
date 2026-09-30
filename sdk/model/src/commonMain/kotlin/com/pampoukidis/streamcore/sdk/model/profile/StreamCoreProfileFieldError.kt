package com.pampoukidis.streamcore.sdk.model.profile

sealed interface StreamCoreProfileFieldError {
    data object Blank : StreamCoreProfileFieldError
    data object TooLong : StreamCoreProfileFieldError
    data object MissingSelection : StreamCoreProfileFieldError
    data object UnknownSelection : StreamCoreProfileFieldError
}
