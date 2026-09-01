package com.pampoukidis.streamcoretv.web.startup

import com.pampoukidis.streamcoretv.web.config.WebRuntimeConfigLoadResult
import com.pampoukidis.streamcoretv.web.config.WebRuntimeConfigLoader
import com.pampoukidis.streamcoretv.web.graph.startWebGraph
import com.pampoukidis.streamcoretv.web.navigation.WebNavigationController
import com.pampoukidis.streamcoretv.web.storage.WebStorageProbe
import com.pampoukidis.streamcoretv.web.storage.WebStorageSelection
import kotlinx.coroutines.CancellationException

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
        val storageSelection = storageProbe.select()
        if (storageSelection is WebStorageSelection.Blocked) {
            return WebStartupState.BlockingError(storageSelection.guidance)
        }

        return try {
            val useSessionStorage = storageSelection is WebStorageSelection.SessionFallback
            WebStartupState.Ready(
                graph = startWebGraph(
                    config = config,
                    useSessionStorage = useSessionStorage,
                ),
                navigationController = WebNavigationController(),
                storageWarning = (storageSelection as? WebStorageSelection.SessionFallback)?.warning,
            )
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            WebStartupState.BlockingError(
                "The TMDB-only web graph could not start: ${throwable.message ?: "unknown graph error"}.",
            )
        }
    }
}
