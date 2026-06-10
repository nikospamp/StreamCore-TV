package com.pampoukidis.streamcoretv.feature.home.common.home

sealed interface HomeAction {
    data class Load(val profileId: String) : HomeAction
    data object Refresh : HomeAction
}