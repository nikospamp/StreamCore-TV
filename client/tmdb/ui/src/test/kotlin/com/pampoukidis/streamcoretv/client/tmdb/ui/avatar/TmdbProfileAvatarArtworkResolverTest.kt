package com.pampoukidis.streamcoretv.client.tmdb.ui.avatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbProfileAvatarArtworkResolverTest {

    private val subject = TmdbProfileAvatarArtworkResolver()
    private val configuredAvatarIds = (1..20).map { index ->
        "tmdb-avatar-${index.toString().padStart(2, '0')}"
    }

    @Test
    fun `every configured avatar ID resolves to unique catalog artwork`() {
        assertEquals(20, configuredAvatarIds.toSet().size)
        assertEquals(20, subject.supportedAvatarIds.size)
        assertEquals(configuredAvatarIds.toSet(), subject.supportedAvatarIds)
        configuredAvatarIds.forEach { avatarId ->
            assertNotNull(subject.resolve(avatarId))
        }
    }

    @Test
    fun `unknown avatar ID does not resolve`() {
        assertNull(subject.resolve("tmdb-avatar-unknown"))
    }
}
