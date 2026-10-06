package com.pampoukidis.streamcore.sdk.model

/** Identifies both the backend and its environment; never place secrets in these identifiers. */
data class StreamCoreConfiguration(
    val backend: String,
    val storageNamespace: String,
    val locale: String = "en-US",
    val region: String? = null,
    val expectedAccountId: String? = null,
    val persistence: StreamCorePersistenceMode = StreamCorePersistenceMode.Persistent,
) {
    init {
        require(backend.isNotBlank())
        require(storageNamespace.isNotBlank())
        require(expectedAccountId == null || expectedAccountId.isNotBlank())
    }
}
