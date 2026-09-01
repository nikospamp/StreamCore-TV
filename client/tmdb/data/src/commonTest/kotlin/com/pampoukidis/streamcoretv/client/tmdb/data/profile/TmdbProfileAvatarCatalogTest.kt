package com.pampoukidis.streamcoretv.client.tmdb.data.profile

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

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