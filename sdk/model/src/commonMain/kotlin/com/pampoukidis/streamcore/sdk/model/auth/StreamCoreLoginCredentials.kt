package com.pampoukidis.streamcore.sdk.model.auth

data class StreamCoreLoginCredentials(
    val identifier: String,
    val password: String,
) {
    override fun toString(): String {
        return "StreamCoreLoginCredentials(identifier=[redacted], password=[redacted])"
    }
}
