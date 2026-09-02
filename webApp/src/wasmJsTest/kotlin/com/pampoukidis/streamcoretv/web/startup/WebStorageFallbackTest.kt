package com.pampoukidis.streamcoretv.web.startup

import com.pampoukidis.streamcoretv.web.storage.WebStorageSelection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebStorageFallbackTest {
    @Test
    fun persistentGraphStartsWithoutSessionRetry(): TestResult {
        return runTest {
            val attempts = mutableListOf<Boolean>()

            val outcome = startWithStorageFallback(WebStorageSelection.Persistent) { useSession ->
                attempts += useSession
                "graph"
            }

            val started = assertIs<WebStorageStartupOutcome.Started<String>>(outcome)
            assertEquals(listOf(false), attempts)
            assertEquals("graph", started.value)
            assertEquals(false, started.useSessionStorage)
            assertNull(started.warning)
        }
    }

    @Test
    fun persistentQuotaFailureRetriesOfficialSessionStorage(): TestResult {
        return runTest {
            val attempts = mutableListOf<Boolean>()

            val outcome = startWithStorageFallback(WebStorageSelection.Persistent) { useSession ->
                attempts += useSession
                if (!useSession) {
                    error("QuotaExceededError")
                }
                "session-graph"
            }

            val started = assertIs<WebStorageStartupOutcome.Started<String>>(outcome)
            assertEquals(listOf(false, true), attempts)
            assertEquals("session-graph", started.value)
            assertTrue(started.useSessionStorage)
            assertTrue(started.warning.orEmpty().contains("quota"))
        }
    }

    @Test
    fun persistentCorruptProtobufFailureRetriesOfficialSessionStorage(): TestResult {
        return runTest {
            val attempts = mutableListOf<Boolean>()

            val outcome = startWithStorageFallback(WebStorageSelection.Persistent) { useSession ->
                attempts += useSession
                if (!useSession) {
                    error("CorruptionException: invalid protobuf version")
                }
                "session-graph"
            }

            val started = assertIs<WebStorageStartupOutcome.Started<String>>(outcome)
            assertEquals(listOf(false, true), attempts)
            assertTrue(started.useSessionStorage)
            assertTrue(started.warning.orEmpty().contains("corrupt"))
        }
    }

    @Test
    fun persistentSecurityFailureRetriesOfficialSessionStorage(): TestResult {
        return runTest {
            val attempts = mutableListOf<Boolean>()

            val outcome = startWithStorageFallback(WebStorageSelection.Persistent) { useSession ->
                attempts += useSession
                if (!useSession) {
                    error("SecurityError: access denied")
                }
                "session-graph"
            }

            val started = assertIs<WebStorageStartupOutcome.Started<String>>(outcome)
            assertEquals(listOf(false, true), attempts)
            assertTrue(started.useSessionStorage)
            assertTrue(started.warning.orEmpty().contains("policy denied"))
        }
    }

    @Test
    fun unknownGraphFailureDoesNotMaskDefectWithStorageRetry(): TestResult {
        return runTest {
            val attempts = mutableListOf<Boolean>()

            val outcome = startWithStorageFallback(WebStorageSelection.Persistent) { useSession ->
                attempts += useSession
                error("missing Koin definition")
            }

            val failed = assertIs<WebStorageStartupOutcome.Failed>(outcome)
            assertEquals(listOf(false), attempts)
            assertTrue(failed.guidance.contains("missing Koin definition"))
        }
    }

    @Test
    fun sessionFailureAfterPersistentFailureMapsToBlockingGuidance(): TestResult {
        return runTest {
            val attempts = mutableListOf<Boolean>()

            val outcome = startWithStorageFallback(WebStorageSelection.Persistent) { useSession ->
                attempts += useSession
                if (useSession) {
                    error("SecurityError")
                }
                error("QuotaExceededError")
            }

            val failed = assertIs<WebStorageStartupOutcome.Failed>(outcome)
            assertEquals(listOf(false, true), attempts)
            assertTrue(failed.guidance.contains("persistent (storage quota was exceeded)"))
            assertTrue(failed.guidance.contains("session (browser policy denied access)"))
        }
    }

    @Test
    fun cancellationNeverRetriesOrMapsToBlockingState(): TestResult {
        return runTest {
            val attempts = mutableListOf<Boolean>()
            var cancellation: CancellationException? = null

            try {
                startWithStorageFallback(WebStorageSelection.Persistent) { useSession ->
                    attempts += useSession
                    throw CancellationException("cancel startup")
                }
            } catch (throwable: CancellationException) {
                cancellation = throwable
            }

            assertEquals(listOf(false), attempts)
            assertEquals("cancel startup", cancellation?.message)
        }
    }
}
