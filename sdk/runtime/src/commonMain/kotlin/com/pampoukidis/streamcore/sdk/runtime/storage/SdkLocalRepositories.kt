package com.pampoukidis.streamcore.sdk.runtime.storage

import com.pampoukidis.streamcore.sdk.runtime.storage.library.LibraryStore
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.runtime.storage.search.SearchHistoryStore
import com.pampoukidis.streamcore.sdk.runtime.storage.playback.PlaybackProgressStore
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration

/** Runtime infrastructure only; identifiers passed to these repositories are encoded account partitions. */
data class SdkLocalRepositories(
    val library: LibraryStore,
    val history: SearchHistoryStore,
    val progress: PlaybackProgressStore,
    val loadSelectedProfile: suspend (StreamCoreConfiguration, String) -> String? = { _, _ -> null },
    val saveSelectedProfile: suspend (StreamCoreConfiguration, String, String?) -> Unit = { _, _, _ -> },
    val migrateLegacy: suspend (StreamCoreConfiguration, String?) -> StreamCoreResult<Unit> = { _, _ -> StreamCoreResult.Success(Unit) },
)
