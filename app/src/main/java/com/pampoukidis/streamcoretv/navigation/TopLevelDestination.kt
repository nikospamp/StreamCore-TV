package com.pampoukidis.streamcoretv.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

internal enum class TopLevelDestination {
    Home,
    Search,
    Library;

    fun route(profileId: String): AppRoute {
        return when (this) {
            Home -> AppRoute.Home(profileId = profileId)
            Search -> AppRoute.Search(profileId = profileId)
            Library -> AppRoute.Library(profileId = profileId)
        }
    }
}

internal fun NavDestination.topLevelDestination(): TopLevelDestination? {
    return when {
        hasRoute<AppRoute.Home>() -> TopLevelDestination.Home
        hasRoute<AppRoute.Search>() -> TopLevelDestination.Search
        hasRoute<AppRoute.Library>() -> TopLevelDestination.Library
        else -> null
    }
}
