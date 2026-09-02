package com.pampoukidis.streamcoretv.web.startup

import com.pampoukidis.streamcoretv.web.config.WebRuntimeConfigLoadResult
import com.pampoukidis.streamcoretv.web.config.WebRuntimeConfigLoader
import com.pampoukidis.streamcoretv.web.graph.startWebGraph
import com.pampoukidis.streamcoretv.web.navigation.WebNavigationController
import com.pampoukidis.streamcoretv.web.storage.WebStorageProbe

internal class WebStartup(
    private val configLoader: WebRuntimeConfigLoader = WebRuntimeConfigLoader(),
    private val storageProbe: WebStorageProbe = WebStorageProbe(),
) {
    suspend fun start(): WebStartupState {
        val config = when (val loadResult = configLoader.load()) {
            is WebRuntimeConfigLoadResult.Success -> loadResult.config
            is WebRuntimeConfigLoadResult.Failure -> {
                return WebStartupState.BlockingError(loadResult.guidance)
            }
        }
        return when (
            val graphOutcome = startWithStorageFallback(
                selection = storageProbe.select(),
                starter = { useSessionStorage ->
                    startWebGraph(
                        config = config,
                        useSessionStorage = useSessionStorage,
                    )
                },
            )
        ) {
            is WebStorageStartupOutcome.Started -> WebStartupState.Ready(
                graph = graphOutcome.value,
                navigationController = WebNavigationController(),
                storageWarning = graphOutcome.warning,
                useSessionStorage = graphOutcome.useSessionStorage,
            )
            is WebStorageStartupOutcome.Failed -> WebStartupState.BlockingError(
                guidance = graphOutcome.guidance,
            )
        }
    }
}
