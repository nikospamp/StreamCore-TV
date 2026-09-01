package com.pampoukidis.streamcoretv.web.startup

import com.pampoukidis.streamcoretv.web.graph.WebGraphHandle
import com.pampoukidis.streamcoretv.web.navigation.WebNavigationController

sealed interface WebStartupState {
    data object Loading : WebStartupState

    data class BlockingError(
        val guidance: String,
    ) : WebStartupState

    data class Ready(
        val graph: WebGraphHandle,
        val navigationController: WebNavigationController,
        val storageWarning: String?,
    ) : WebStartupState
}
