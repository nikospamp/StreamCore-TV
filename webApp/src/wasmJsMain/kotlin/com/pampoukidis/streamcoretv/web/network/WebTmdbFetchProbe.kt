package com.pampoukidis.streamcoretv.web.network

import com.pampoukidis.streamcore.sdk.api.ProfileService
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.api.SearchService
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryPinRequired
import com.pampoukidis.streamcoretv.web.graph.WebGraphHandle

internal class WebTmdbFetchProbe {
    suspend fun run(graph: WebGraphHandle): WebTmdbFetchProbeResult {
        val profileRepository = graph.application.koin.get<ProfileService>()
        val profiles = when (val result = profileRepository.getProfiles()) {
            is StreamCoreResult.Success -> result.value
            is StreamCoreResult.Failure -> return WebTmdbFetchProbeResult.Failure(result.error.toProbeCode())
        }
        val profile = profiles.firstOrNull()
            ?: return WebTmdbFetchProbeResult.Failure("profile-not-found")
        val repository = graph.application.koin.get<SearchService>()
        when (val selection = profileRepository.selectProfile(profile.id)) {
            is StreamCoreResult.Failure -> return WebTmdbFetchProbeResult.Failure(selection.error.toProbeCode())
            is StreamCoreResult.Success -> {
                val outcome = selection.value
                if (outcome is StreamCoreProfileEntryPinRequired) {
                    profileRepository.cancelPin(outcome.challenge.challengeId)
                    return WebTmdbFetchProbeResult.Failure("profile-pin-required")
                }
            }
        }
        return try { when (
            val result = repository.search(
                profileId = profile.id,
                query = ProbeQuery,
            )
        ) {
            is StreamCoreResult.Success -> WebTmdbFetchProbeResult.Success
            is StreamCoreResult.Failure -> WebTmdbFetchProbeResult.Failure(result.error.toProbeCode())
        } } finally {
            profileRepository.clearSelection()
        }
    }

    private fun StreamCoreError.toProbeCode(): String {
        return when (this) {
            is StreamCoreError.PinRejected -> "profile-pin-rejected"
            is StreamCoreError.Unsupported -> "unsupported"
            is StreamCoreError.Validation -> "validation-error"
            is StreamCoreError.InvalidContext -> "invalid-context"
            is StreamCoreError.Storage -> "storage-error"
            is StreamCoreError.Closed -> "closed"
            is StreamCoreError.Authentication -> "authentication-error"
            is StreamCoreError.Network -> "network-error"
            is StreamCoreError.Parsing -> "parsing-error"
            is StreamCoreError.Server -> "server-error"
            is StreamCoreError.SessionExpired -> "session-expired"
            is StreamCoreError.Timeout -> "timeout"
            is StreamCoreError.Unauthorized -> "unauthorized"
            is StreamCoreError.Unknown -> "unknown-error"
        }
    }

    private companion object {
        const val ProbeQuery = "web-fetch-probe"
    }
}

internal sealed interface WebTmdbFetchProbeResult {
    data object Success : WebTmdbFetchProbeResult
    data class Failure(val code: String) : WebTmdbFetchProbeResult
}
