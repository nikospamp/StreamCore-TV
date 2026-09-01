package com.pampoukidis.streamcoretv.web.storage

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebStorageProbeTest {
    @Test
    fun selectsPersistentStorageAfterWriteReadRemoveProbe() {
        val local = FakeStorage()

        val selection = WebStorageProbe(local = local, session = FakeStorage()).select()

        assertIs<WebStorageSelection.Persistent>(selection)
        assertTrue(local.values.isEmpty())
    }

    @Test
    fun securityDenialFallsBackToSessionStorageWithWarning() {
        val selection = WebStorageProbe(
            local = FailingStorage("SecurityError: access denied"),
            session = FakeStorage(),
        ).select()

        assertIs<WebStorageSelection.SessionFallback>(selection)
        assertTrue(selection.warning.contains("lost when this tab closes"))
    }

    @Test
    fun quotaFailureIsClassifiedAndFallsBackWithoutLeavingProbeData() {
        val session = FakeStorage()

        val selection = WebStorageProbe(
            local = FailingStorage("QuotaExceededError"),
            session = session,
        ).select()

        assertIs<WebStorageSelection.SessionFallback>(selection)
        assertTrue(selection.warning.contains("quota"))
        assertTrue(session.values.isEmpty())
    }

    @Test
    fun bothStorageFailuresBlockGraphStartup() {
        val selection = WebStorageProbe(
            local = FailingStorage("Corrupt storage"),
            session = FailingStorage("SecurityError"),
        ).select()

        assertIs<WebStorageSelection.Blocked>(selection)
        assertTrue(selection.guidance.contains("corrupt"))
        assertTrue(selection.guidance.contains("policy denied"))
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
