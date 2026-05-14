package com.pampoukidis.streamcoretv.core.model.error

sealed interface AppError {
    val source: ErrorSource?

    data class Network(
        override val source: ErrorSource? = null,
    ) : AppError

    data class Timeout(
        override val source: ErrorSource? = null,
    ) : AppError

    data class Unauthorized(
        override val source: ErrorSource? = null,
    ) : AppError

    data class SessionExpired(
        override val source: ErrorSource? = null,
    ) : AppError

    data class Authentication(
        val remainingAttempts: Int? = null,
        override val source: ErrorSource? = null,
    ) : AppError

    data class Server(
        override val source: ErrorSource? = null,
    ) : AppError

    data class Parsing(
        override val source: ErrorSource? = null,
    ) : AppError

    data class Unknown(
        override val source: ErrorSource? = null,
    ) : AppError
}
