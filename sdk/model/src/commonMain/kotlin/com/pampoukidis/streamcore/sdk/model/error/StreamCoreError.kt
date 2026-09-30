package com.pampoukidis.streamcore.sdk.model.error

/**
 * Typed operation failure. Branch on its type and structured details, not provider diagnostic messages.
 */
sealed interface StreamCoreError {
    val source: StreamCoreErrorSource?

    /** Operation unavailable for this provider/capability; distinct from rejected credentials. */
    data class Unsupported(val operation: String, override val source: StreamCoreErrorSource? = null) : StreamCoreError
    /** Typed input problems for form feedback. Runtime validation remains authoritative. */
    data class Validation(val issues: List<StreamCoreValidationIssue>, override val source: StreamCoreErrorSource? = null) : StreamCoreError
    /** Missing, mismatched or superseded authorization; inspect reason for recovery. */
    data class InvalidContext(val reason: StreamCoreContextFailureReason = StreamCoreContextFailureReason.NoActiveProfile, override val source: StreamCoreErrorSource? = null) : StreamCoreError
    /** Provider rejected a profile PIN without account logout. Null attempts means none supplied; zero is not a runtime-defined cooldown. */
    data class PinRejected(val remainingAttempts: Int? = null, override val source: StreamCoreErrorSource? = null) : StreamCoreError
    /** Persistent state read/write failure. Failed clear-selection cleanup never restores revoked access. */
    data class Storage(override val source: StreamCoreErrorSource? = null) : StreamCoreError
    /** The client was closed; a new owned instance is required for another session. */
    data class Closed(override val source: StreamCoreErrorSource? = null) : StreamCoreError

    data class Network(
        override val source: StreamCoreErrorSource? = null,
    ) : StreamCoreError

    data class Timeout(
        override val source: StreamCoreErrorSource? = null,
    ) : StreamCoreError

    data class Unauthorized(
        override val source: StreamCoreErrorSource? = null,
    ) : StreamCoreError

    /** Authoritative account-session rejection; runtime reconciles context and revokes profile access. */
    data class SessionExpired(
        override val source: StreamCoreErrorSource? = null,
    ) : StreamCoreError

    /** Account credential rejection; does not itself declare an existing session expired. */
    data class Authentication(
        val remainingAttempts: Int? = null,
        override val source: StreamCoreErrorSource? = null,
    ) : StreamCoreError

    data class Server(
        override val source: StreamCoreErrorSource? = null,
    ) : StreamCoreError

    data class Parsing(
        override val source: StreamCoreErrorSource? = null,
    ) : StreamCoreError

    data class Unknown(
        override val source: StreamCoreErrorSource? = null,
    ) : StreamCoreError
}
