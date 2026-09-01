package com.pampoukidis.streamcoretv.web.storage

import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import kotlinx.coroutines.CancellationException
import org.w3c.dom.Storage

internal class WebStorageProbe(
    private val local: WebKeyValueStorage = BrowserWebKeyValueStorage(localStorage),
    private val session: WebKeyValueStorage = BrowserWebKeyValueStorage(sessionStorage),
) {
    fun select(): WebStorageSelection {
        return when (val localResult = probe(local, LocalProbeKey)) {
            WebStorageProbeResult.Available -> WebStorageSelection.Persistent
            is WebStorageProbeResult.Unavailable -> {
                when (val sessionResult = probe(session, SessionProbeKey)) {
                    WebStorageProbeResult.Available -> WebStorageSelection.SessionFallback(
                        warning = "Persistent browser storage is unavailable (${localResult.failure.kind.label}). " +
                            "State will be lost when this tab closes.",
                    )
                    is WebStorageProbeResult.Unavailable -> WebStorageSelection.Blocked(
                        guidance = "Browser storage is unavailable. Enable site storage and reload. " +
                            "Persistent failure: ${localResult.failure.kind.label}; " +
                            "session failure: ${sessionResult.failure.kind.label}.",
                    )
                }
            }
        }
    }

    private fun probe(storage: WebKeyValueStorage, key: String): WebStorageProbeResult {
        return try {
            storage.setItem(key, ProbeValue)
            check(storage.getItem(key) == ProbeValue)
            storage.removeItem(key)
            WebStorageProbeResult.Available
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            runCatching { storage.removeItem(key) }
            WebStorageProbeResult.Unavailable(throwable.toWebStorageFailure())
        }
    }

    private companion object {
        const val LocalProbeKey = "streamcore.web.storage.probe.local"
        const val SessionProbeKey = "streamcore.web.storage.probe.session"
        const val ProbeValue = "available"
    }
}

internal interface WebKeyValueStorage {
    fun setItem(key: String, value: String)
    fun getItem(key: String): String?
    fun removeItem(key: String)
}

private class BrowserWebKeyValueStorage(
    private val delegate: Storage,
) : WebKeyValueStorage {
    override fun setItem(key: String, value: String) {
        delegate.setItem(key, value)
    }

    override fun getItem(key: String): String? {
        return delegate.getItem(key)
    }

    override fun removeItem(key: String) {
        delegate.removeItem(key)
    }
}

sealed interface WebStorageSelection {
    data object Persistent : WebStorageSelection
    data class SessionFallback(val warning: String) : WebStorageSelection
    data class Blocked(val guidance: String) : WebStorageSelection
}

data class WebStorageFailure(
    val kind: WebStorageFailureKind,
    val cause: Throwable,
)

enum class WebStorageFailureKind(val label: String) {
    Security("browser policy denied access"),
    Quota("storage quota was exceeded"),
    Corruption("stored data is corrupt"),
    Unknown("unknown storage failure"),
}

private sealed interface WebStorageProbeResult {
    data object Available : WebStorageProbeResult
    data class Unavailable(val failure: WebStorageFailure) : WebStorageProbeResult
}

private fun Throwable.toWebStorageFailure(): WebStorageFailure {
    val normalized = "${this::class.simpleName.orEmpty()} ${message.orEmpty()}".lowercase()
    val kind = when {
        "security" in normalized || "denied" in normalized -> WebStorageFailureKind.Security
        "quota" in normalized || "full" in normalized -> WebStorageFailureKind.Quota
        "corrupt" in normalized || "serial" in normalized -> WebStorageFailureKind.Corruption
        else -> WebStorageFailureKind.Unknown
    }
    return WebStorageFailure(kind = kind, cause = this)
}
