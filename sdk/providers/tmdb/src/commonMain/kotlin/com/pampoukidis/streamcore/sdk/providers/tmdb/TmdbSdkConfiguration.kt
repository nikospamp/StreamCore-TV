package com.pampoukidis.streamcore.sdk.providers.tmdb

import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbConnectionConfiguration
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration

data class TmdbSdkConfiguration(
    val common: StreamCoreConfiguration,
    val connection: TmdbConnectionConfiguration,
    val demoPlayback: Boolean = false,
    /** Preserve original application filenames during its explicit upgrade. */
    val legacyApplicationStorage: Boolean = false,
)

