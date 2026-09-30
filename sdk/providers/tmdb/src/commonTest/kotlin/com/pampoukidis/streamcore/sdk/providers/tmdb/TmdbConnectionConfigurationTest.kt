package com.pampoukidis.streamcore.sdk.providers.tmdb

import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TmdbConnectionConfigurationTest {
    @Test
    fun diagnosticsRedactTheTokenInConnectionAndSdkConfiguration() {
        val token = "private-fixture-token"
        val connection = TmdbConnectionConfiguration("https://api.tmdb.test", token)
        val config = TmdbSdkConfiguration(StreamCoreConfiguration("tmdb-test", "redaction"), connection)

        assertEquals(token, connection.readAccessToken)
        assertTrue(connection.toString().contains("[REDACTED]"))
        assertFalse(connection.toString().contains(token))
        assertFalse(config.toString().contains(token))
    }
}
