package com.pampoukidis.streamcoretv.navigation

import kotlinx.serialization.Serializable

internal sealed interface AppRoute {

    @Serializable
    data object Login : AppRoute

    @Serializable
    data object Profiles : AppRoute

    @Serializable
    data class Authenticated(
        val profileId: String,
    ) : AppRoute
}
