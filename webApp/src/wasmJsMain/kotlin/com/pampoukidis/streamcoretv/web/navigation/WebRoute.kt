package com.pampoukidis.streamcoretv.web.navigation

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
        override val path: String = "/profiles/$profileId/edit"
    }

    data object AuthenticatedLanding : WebRoute {
        override val path: String = "/authenticated"
    }

    data class Details(val contentId: String) : WebRoute {
        override val path: String = "/details/$contentId"
    }

    data class Player(val contentId: String) : WebRoute {
        override val path: String = "/player/$contentId"
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
                segments.size == 2 && segments.first() == "details" && segments.last().isBrowserSafeId() -> {
                    Details(segments.last())
                }
                segments.size == 2 && segments.first() == "player" && segments.last().isBrowserSafeId() -> {
                    Player(segments.last())
                }
                else -> Root
            }
        }
    }
}

private fun String.isBrowserSafeId(): Boolean {
    return isNotBlank() && all { character ->
        character.isLetterOrDigit() || character == '-' || character == '_'
    }
}
