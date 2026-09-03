package com.pampoukidis.streamcoretv.web.navigation

import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination

sealed interface WebRoute {
    val path: String

    data object Diagnostic : WebRoute {
        override val path: String = "/diagnostic"
    }

    data object Root : WebRoute {
        override val path: String = "/"
    }

    data object Login : WebRoute {
        override val path: String = "/login"
    }

    data object Profiles : WebRoute {
        override val path: String = "/profiles"
    }

    data object CreateProfile : WebRoute {
        override val path: String = "/profiles/new"
    }

    data class EditProfile(val profileId: String) : WebRoute {
        init {
            require(profileId.isBrowserSafeId())
        }

        override val path: String = "/profiles/$profileId/edit"
    }

    data object AuthenticatedLanding : WebRoute {
        override val path: String = "/authenticated"
    }

    data object Home : WebRoute {
        override val path: String = "/home"
    }

    data object Search : WebRoute {
        override val path: String = "/search"
    }

    data object Library : WebRoute {
        override val path: String = "/library"
    }

    data class Details(val contentId: String) : WebRoute {
        init {
            require(contentId.isBrowserSafeId())
        }

        override val path: String = "/details/$contentId"
    }

    data class Player(val contentId: String) : WebRoute {
        init {
            require(contentId.isBrowserSafeId())
        }

        override val path: String = "/player/$contentId"
    }

    data class DiagnosticDetails(val contentId: String) : WebRoute {
        init {
            require(contentId.isBrowserSafeId())
        }

        override val path: String = "/diagnostic/details/$contentId"
    }

    data class DiagnosticPlayer(val contentId: String) : WebRoute {
        init {
            require(contentId.isBrowserSafeId())
        }

        override val path: String = "/diagnostic/player/$contentId"
    }

    companion object {
        fun parse(path: String): WebRoute {
            val segments = path.substringBefore('?').trim('/').split('/').filter(String::isNotBlank)
            return when {
                segments.isEmpty() -> Root
                segments.size == 1 && segments.first() == "diagnostic" -> Diagnostic
                segments.size == 1 && segments.first() == "login" -> Login
                segments.size == 1 && segments.first() == "profiles" -> Profiles
                segments.size == 2 && segments.first() == "profiles" && segments.last() == "new" -> CreateProfile
                segments.size == 3 && segments.first() == "profiles" &&
                    segments.last() == "edit" && segments[1].isBrowserSafeId() -> EditProfile(segments[1])
                segments.size == 1 && segments.first() == "authenticated" -> AuthenticatedLanding
                segments.size == 1 && segments.first() == "home" -> Home
                segments.size == 1 && segments.first() == "search" -> Search
                segments.size == 1 && segments.first() == "library" -> Library
                segments.size == 2 && segments.first() == "details" && segments.last().isBrowserSafeId() -> {
                    Details(segments.last())
                }
                segments.size == 2 && segments.first() == "player" && segments.last().isBrowserSafeId() -> {
                    Player(segments.last())
                }
                segments.size == 3 && segments[0] == "diagnostic" && segments[1] == "details" &&
                    segments[2].isBrowserSafeId() -> DiagnosticDetails(segments[2])
                segments.size == 3 && segments[0] == "diagnostic" && segments[1] == "player" &&
                    segments[2].isBrowserSafeId() -> DiagnosticPlayer(segments[2])
                else -> Root
            }
        }
    }
}

internal fun WebRoute.isDiagnosticRoute(): Boolean {
    return this is WebRoute.Diagnostic ||
        this is WebRoute.DiagnosticDetails ||
        this is WebRoute.DiagnosticPlayer
}

internal fun WebRoute.requiresSelectedProfile(): Boolean {
    return this is WebRoute.AuthenticatedLanding ||
        this is WebRoute.Home ||
        this is WebRoute.Search ||
        this is WebRoute.Library ||
        this is WebRoute.Details ||
        this is WebRoute.Player
}

internal fun WebRoute.browseDestinationOrNull(): WebBrowseDestination? {
    return when (this) {
        WebRoute.Home -> WebBrowseDestination.Home
        WebRoute.Search -> WebBrowseDestination.Search
        WebRoute.Library -> WebBrowseDestination.Library
        is WebRoute.Details -> WebBrowseDestination.Details
        else -> null
    }
}

internal fun String.isBrowserSafeId(): Boolean {
    return isNotBlank() && all { character ->
        character.isLetterOrDigit() || character == '-' || character == '_'
    }
}
