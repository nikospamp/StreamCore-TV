package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcoretv.core.model.content.ContentModel

sealed interface HomeAction {
    data class Load(val profileId: String) : HomeAction
    data class ContentSelected(val content: ContentModel) : HomeAction
    data object Refresh : HomeAction
}