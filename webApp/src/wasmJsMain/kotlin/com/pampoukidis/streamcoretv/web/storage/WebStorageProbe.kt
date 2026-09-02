package com.pampoukidis.streamcoretv.web.storage

import androidx.datastore.core.CorruptionException
import kotlinx.browser.localStorage
import kotlinx.browser.sessionStorage
import kotlinx.coroutines.CancellationException
import org.w3c.dom.Storage

internal class WebStorageProbe(
    private val localProvider: () -> WebKeyValueStorage = {
        BrowserWebKeyValueStorage(localStorage)
    },
    private val sessionProvider: () -> WebKeyValueStorage = {
        BrowserWebKeyValueStorage(sessionStorage)
    },
) {
    fun select(): WebStorageSelection {
        return when (val localResult = probe(localProvider, LocalProbeKey)) {
            WebStorageProbeResult.Available -> WebStorageSelection.Persistent
            is WebStorageProbeResult.Unavailable -> {
                when (val sessionResult = probe(sessionProvider, SessionProbeKey)) {
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

    private fun probe(
        storageProvider: () -> WebKeyValueStorage,
        key: String,
    ): WebStorageProbeResult {
        var storage: WebKeyValueStorage? = null
        return try {
            storage = storageProvider()
            storage.setItem(key, ProbeValue)
            check(storage.getItem(key) == ProbeValue)
            storage.removeItem(key)
            WebStorageProbeResult.Available
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            storage?.let { acquiredStorage ->
                try {
                    acquiredStorage.removeItem(key)
                } catch (cleanupCancellation: CancellationException) {
                    throw cleanupCancellation
                } catch (_: Throwable) {
                    // The original storage failure owns the classification.
                }
            }
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
    Io("browser storage I/O failed"),
    Unknown("unknown storage failure"),
}

private sealed interface WebStorageProbeResult {
    data object Available : WebStorageProbeResult
    data class Unavailable(val failure: WebStorageFailure) : WebStorageProbeResult
}

internal fun Throwable.toWebStorageFailure(): WebStorageFailure {
    val normalized = buildString {
        var current: Throwable? = this@toWebStorageFailure
        repeat(MaxCauseDepth) {
            val throwable = current ?: return@repeat
            append(throwable::class.simpleName.orEmpty())
            append(' ')
            append(throwable.message.orEmpty())
            append(' ')
            current = throwable.cause
        }
    }.lowercase()
    val kind = when {
        this is CorruptionException || "corrupt" in normalized || "serial" in normalized ||
            "protobuf" in normalized || "invalid version" in normalized ||
            "unsupported version" in normalized || "version mismatch" in normalized ->
            WebStorageFailureKind.Corruption
        "security" in normalized || "denied" in normalized || "notallowed" in normalized ->
            WebStorageFailureKind.Security
        "quota" in normalized || "full" in normalized -> WebStorageFailureKind.Quota
        "ioexception" in normalized || "i/o" in normalized -> WebStorageFailureKind.Io
        else -> WebStorageFailureKind.Unknown
    }
    return WebStorageFailure(kind = kind, cause = this)
}

private const val MaxCauseDepth = 4
