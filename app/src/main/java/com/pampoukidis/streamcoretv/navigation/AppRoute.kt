package com.pampoukidis.streamcoretv.navigation

import kotlinx.serialization.Serializable

internal sealed interface AppRoute {

    @Serializable
    data object Login : AppRoute

    @Serializable
    data object Profiles : AppRoute

    @Serializable
    data class CreateProfile(
        val fromLogin: Boolean,
    ) : AppRoute

    @Serializable
    data class EditProfile(
        val profileId: String,
    ) : AppRoute

    @Serializable
    data class Home(
        val profileId: String,
    ) : AppRoute

    @Serializable
    data class AssetDetails(
        val profileId: String,
        val contentId: String,
    ) : AppRoute
}
