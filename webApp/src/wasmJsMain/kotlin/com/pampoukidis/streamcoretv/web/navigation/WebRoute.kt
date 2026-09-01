package com.pampoukidis.streamcoretv.web.navigation

sealed interface WebRoute {
    val path: String

    data object Diagnostic : WebRoute {
        override val path: String = "/"
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
                segments.isEmpty() -> Diagnostic
                segments.size == 2 && segments.first() == "details" && segments.last().isBrowserSafeId() -> {
                    Details(segments.last())
                }
                segments.size == 2 && segments.first() == "player" && segments.last().isBrowserSafeId() -> {
                    Player(segments.last())
                }
                else -> Diagnostic
            }
        }
    }
}

private fun String.isBrowserSafeId(): Boolean {
    return isNotBlank() && all { character ->
        character.isLetterOrDigit() || character == '-' || character == '_'
    }
}
