package com.pampoukidis.streamcoretv.web.storage

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebStorageProbeTest {
    @Test
    fun selectsPersistentStorageAfterWriteReadRemoveProbe() {
        val local = FakeStorage()

        val selection = WebStorageProbe(
            localProvider = { local },
            sessionProvider = { FakeStorage() },
        ).select()

        assertIs<WebStorageSelection.Persistent>(selection)
        assertTrue(local.values.isEmpty())
    }

    @Test
    fun securityDenialFallsBackToSessionStorageWithWarning() {
        val selection = WebStorageProbe(
            localProvider = { FailingStorage("SecurityError: access denied") },
            sessionProvider = { FakeStorage() },
        ).select()

        assertIs<WebStorageSelection.SessionFallback>(selection)
        assertTrue(selection.warning.contains("lost when this tab closes"))
    }

    @Test
    fun quotaFailureIsClassifiedAndFallsBackWithoutLeavingProbeData() {
        val session = FakeStorage()

        val selection = WebStorageProbe(
            localProvider = { FailingStorage("QuotaExceededError") },
            sessionProvider = { session },
        ).select()

        assertIs<WebStorageSelection.SessionFallback>(selection)
        assertTrue(selection.warning.contains("quota"))
        assertTrue(session.values.isEmpty())
    }

    @Test
    fun bothStorageFailuresBlockGraphStartup() {
        val selection = WebStorageProbe(
            localProvider = { FailingStorage("Corrupt storage") },
            sessionProvider = { FailingStorage("SecurityError") },
        ).select()

        assertIs<WebStorageSelection.Blocked>(selection)
        assertTrue(selection.guidance.contains("corrupt"))
        assertTrue(selection.guidance.contains("policy denied"))
    }

    @Test
    fun localStorageGetterFailureIsGuardedAndFallsBackToSessionStorage() {
        val selection = WebStorageProbe(
            localProvider = { error("SecurityError: localStorage getter denied") },
            sessionProvider = { FakeStorage() },
        ).select()

        assertIs<WebStorageSelection.SessionFallback>(selection)
        assertTrue(selection.warning.contains("policy denied"))
    }

    @Test
    fun bothStorageGetterFailuresReturnBlockedSelection() {
        val selection = WebStorageProbe(
            localProvider = { error("SecurityError: localStorage getter denied") },
            sessionProvider = { error("SecurityError: sessionStorage getter denied") },
        ).select()

        assertIs<WebStorageSelection.Blocked>(selection)
        assertTrue(selection.guidance.contains("Persistent failure: browser policy denied access"))
        assertTrue(selection.guidance.contains("session failure: browser policy denied access"))
    }
}

private class FakeStorage : WebKeyValueStorage {
    val values = mutableMapOf<String, String>()

    override fun setItem(key: String, value: String) {
        values[key] = value
    }

    override fun getItem(key: String): String? {
        return values[key]
    }

    override fun removeItem(key: String) {
        values.remove(key)
    }
}

private class FailingStorage(
    private val message: String,
) : WebKeyValueStorage {
    override fun setItem(key: String, value: String) {
        error(message)
    }

    override fun getItem(key: String): String? {
        error(message)
    }

    override fun removeItem(key: String) {
        return
    }
}
