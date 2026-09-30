package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent

sealed interface HomeAction {
    data class Load(val profileId: String) : HomeAction
    data class ContentSelected(
        val content: StreamCoreContent,
        val sourceArtworkUrl: String? = null,
    ) : HomeAction
    data object Refresh : HomeAction
}
