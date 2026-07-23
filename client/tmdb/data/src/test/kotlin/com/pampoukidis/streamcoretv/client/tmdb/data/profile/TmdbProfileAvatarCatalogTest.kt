package com.pampoukidis.streamcoretv.client.tmdb.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbProfileAvatarCatalogTest {

    @Test
    fun `provides all bundled avatar slots`() {
        val avatars = TmdbProfileAvatarCatalog.avatars

        assertEquals(20, avatars.size)
        assertEquals(20, avatars.map { it.id }.toSet().size)
        assertTrue(avatars.all { it.id.startsWith("tmdb-avatar-") })
        avatars.forEach { avatar -> assertNull(avatar.imageUrl) }
    }
}