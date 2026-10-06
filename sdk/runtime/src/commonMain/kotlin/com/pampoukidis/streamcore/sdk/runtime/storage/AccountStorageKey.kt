package com.pampoukidis.streamcore.sdk.runtime.storage

import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration

/** Length framing is injective for arbitrary Unicode account/profile identifiers. */
internal fun accountStorageKey(configuration: StreamCoreConfiguration, accountId: String, profileId: String): String {
    return "sdk2:" + listOf(configuration.backend, configuration.storageNamespace, accountId, profileId).joinToString("") { "${it.length}:$it" }
}
