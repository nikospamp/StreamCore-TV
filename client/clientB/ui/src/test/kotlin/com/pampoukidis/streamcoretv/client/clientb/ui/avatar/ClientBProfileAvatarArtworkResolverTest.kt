package com.pampoukidis.streamcoretv.client.clientb.ui.avatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ClientBProfileAvatarArtworkResolverTest {

    private val subject = ClientBProfileAvatarArtworkResolver()
    private val configuredAvatarIds = (1..9).map { index ->
        "client-b-avatar-${index.toString().padStart(2, '0')}"
    }

    @Test
    fun `every configured avatar ID resolves to unique catalog artwork`() {
        assertEquals(9, configuredAvatarIds.toSet().size)
        assertEquals(9, subject.supportedAvatarIds.size)
        assertEquals(configuredAvatarIds.toSet(), subject.supportedAvatarIds)
        configuredAvatarIds.forEach { avatarId ->
            assertNotNull(subject.resolve(avatarId))
        }
    }

    @Test
    fun `unknown avatar ID does not resolve`() {
        assertNull(subject.resolve("client-b-avatar-unknown"))
    }
}
