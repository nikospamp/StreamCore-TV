package com.pampoukidis.streamcoretv.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

internal enum class MobileTopLevelDestination {
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

internal fun NavDestination.mobileTopLevelDestination(): MobileTopLevelDestination? {
    return when {
        hasRoute<AppRoute.Home>() -> MobileTopLevelDestination.Home
        hasRoute<AppRoute.Search>() -> MobileTopLevelDestination.Search
        hasRoute<AppRoute.Library>() -> MobileTopLevelDestination.Library
        else -> null
    }
}
