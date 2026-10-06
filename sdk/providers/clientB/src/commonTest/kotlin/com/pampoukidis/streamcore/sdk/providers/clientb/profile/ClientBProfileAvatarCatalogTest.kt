package com.pampoukidis.streamcore.sdk.providers.clientb.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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