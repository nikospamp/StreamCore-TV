package com.pampoukidis.streamcoretv.web.startup

import com.pampoukidis.streamcoretv.web.storage.WebStorageFailureKind
import com.pampoukidis.streamcoretv.web.storage.WebStorageSelection
import com.pampoukidis.streamcoretv.web.storage.toWebStorageFailure
import kotlinx.coroutines.CancellationException

internal suspend fun <T> startWithStorageFallback(
    selection: WebStorageSelection,
    starter: suspend (useSessionStorage: Boolean) -> T,
): WebStorageStartupOutcome<T> {
    return when (selection) {
        is WebStorageSelection.Blocked -> {
            WebStorageStartupOutcome.Failed(selection.guidance)
        }
        is WebStorageSelection.SessionFallback -> {
            startSessionOnly(
                warning = selection.warning,
                starter = starter,
            )
        }
        WebStorageSelection.Persistent -> {
            startPersistentThenSession(starter)
        }
    }
}

private suspend fun <T> startSessionOnly(
    warning: String,
    starter: suspend (useSessionStorage: Boolean) -> T,
): WebStorageStartupOutcome<T> {
    return try {
        WebStorageStartupOutcome.Started(
            value = starter(true),
            useSessionStorage = true,
            warning = warning,
        )
    } catch (throwable: CancellationException) {
        throw throwable
    } catch (throwable: Throwable) {
        WebStorageStartupOutcome.Failed(
            guidance = graphFailureGuidance(throwable),
        )
    }
}

private suspend fun <T> startPersistentThenSession(
    starter: suspend (useSessionStorage: Boolean) -> T,
): WebStorageStartupOutcome<T> {
    return try {
        WebStorageStartupOutcome.Started(
            value = starter(false),
            useSessionStorage = false,
            warning = null,
        )
    } catch (throwable: CancellationException) {
        throw throwable
    } catch (persistentFailure: Throwable) {
        val storageFailure = persistentFailure.toWebStorageFailure()
        if (!storageFailure.kind.isRecoverableStorageFailure()) {
            return WebStorageStartupOutcome.Failed(
                guidance = graphFailureGuidance(persistentFailure),
            )
        }

        try {
            WebStorageStartupOutcome.Started(
                value = starter(true),
                useSessionStorage = true,
                warning = "Persistent DataStore failed (${storageFailure.kind.label}). " +
                    "This tab is using session storage; state will be lost when it closes.",
            )
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (sessionFailure: Throwable) {
            val sessionStorageFailure = sessionFailure.toWebStorageFailure()
            WebStorageStartupOutcome.Failed(
                guidance = "Browser DataStore startup failed in persistent " +
                    "(${storageFailure.kind.label}) and session " +
                    "(${sessionStorageFailure.kind.label}) modes. Enable site storage and reload.",
            )
        }
    }
}

private fun WebStorageFailureKind.isRecoverableStorageFailure(): Boolean {
    return this == WebStorageFailureKind.Security ||
        this == WebStorageFailureKind.Quota ||
        this == WebStorageFailureKind.Corruption ||
        this == WebStorageFailureKind.Io
}

private fun graphFailureGuidance(throwable: Throwable): String {
    return "The TMDB-only web graph could not start: ${throwable.message ?: "unknown graph error"}."
}

internal sealed interface WebStorageStartupOutcome<out T> {
    data class Started<T>(
        val value: T,
        val useSessionStorage: Boolean,
        val warning: String?,
    ) : WebStorageStartupOutcome<T>

    data class Failed(
        val guidance: String,
    ) : WebStorageStartupOutcome<Nothing>
}
