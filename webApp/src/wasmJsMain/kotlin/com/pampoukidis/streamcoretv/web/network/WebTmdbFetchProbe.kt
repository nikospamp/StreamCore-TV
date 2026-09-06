package com.pampoukidis.streamcoretv.web.network

import com.pampoukidis.streamcoretv.core.domain.ProfileRepository
import com.pampoukidis.streamcoretv.core.model.error.AppError
import com.pampoukidis.streamcoretv.core.model.error.AppResult
import com.pampoukidis.streamcoretv.feature.search.domain.SearchRepository
import com.pampoukidis.streamcoretv.web.graph.WebGraphHandle

internal class WebTmdbFetchProbe {
    suspend fun run(graph: WebGraphHandle): WebTmdbFetchProbeResult {
        val profileRepository = graph.application.koin.get<ProfileRepository>()
        val profiles = when (val result = profileRepository.getProfiles()) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return WebTmdbFetchProbeResult.Failure(result.error.toProbeCode())
        }
        val profile = profiles.firstOrNull()
            ?: return WebTmdbFetchProbeResult.Failure("profile-not-found")
        val repository = graph.application.koin.get<SearchRepository>()
        return when (
            val result = repository.search(
                profileId = profile.id,
                query = ProbeQuery,
            )
        ) {
            is AppResult.Success -> WebTmdbFetchProbeResult.Success
            is AppResult.Failure -> WebTmdbFetchProbeResult.Failure(result.error.toProbeCode())
        }
    }

    private fun AppError.toProbeCode(): String {
        return when (this) {
            is AppError.Authentication -> "authentication-error"
            is AppError.Network -> "network-error"
            is AppError.Parsing -> "parsing-error"
            is AppError.Server -> "server-error"
            is AppError.SessionExpired -> "session-expired"
            is AppError.Timeout -> "timeout"
            is AppError.Unauthorized -> "unauthorized"
            is AppError.Unknown -> "unknown-error"
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
