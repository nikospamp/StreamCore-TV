package com.pampoukidis.streamcoretv.data.client.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientBProfileAvatarCatalogTest {

    @Test
    fun `provides nine bundled avatar slots`() {
        val avatars = ClientBProfileAvatarCatalog.avatars

        assertEquals(9, avatars.size)
        assertEquals(9, avatars.map { it.id }.toSet().size)
        assertTrue(avatars.all { it.id.startsWith("client-b-avatar-") })
        avatars.forEach { avatar -> assertNull(avatar.imageUrl) }
    }
}