package com.pampoukidis.streamcore.sdk.providers.tmdb.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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