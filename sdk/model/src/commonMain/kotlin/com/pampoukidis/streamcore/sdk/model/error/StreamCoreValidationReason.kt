package com.pampoukidis.streamcore.sdk.model.error

/**
 * Machine-readable reason; applications choose localized wording for the associated field.
 */
enum class StreamCoreValidationReason { Required, TooLong, UnknownSelection, NotAllowed, InvalidFormat, InvalidLength, OutOfRange, Mismatch }
