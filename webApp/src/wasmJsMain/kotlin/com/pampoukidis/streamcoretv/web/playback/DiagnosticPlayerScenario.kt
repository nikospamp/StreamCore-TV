package com.pampoukidis.streamcoretv.web.playback

internal enum class DiagnosticPlayerScenario(
    val queryValue: String,
) {
    Success("success"),
    AutoplayBlocked("autoplay-blocked"),
    RecoverableError("recoverable-error"),
    Tracks("tracks"),
    Resume("resume"),
    NoFilmstrip("no-filmstrip"),
    ;

    companion object {
        fun fromQueryValue(value: String?): DiagnosticPlayerScenario {
            return entries.firstOrNull { scenario -> scenario.queryValue == value } ?: Success
        }
    }
}
