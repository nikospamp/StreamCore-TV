package com.pampoukidis.streamcoretv.client.clientb.ui.avatar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import streamcoretv.client.clientb.ui.generated.resources.Res
import streamcoretv.client.clientb.ui.generated.resources.*

class ClientBProfileAvatarArtworkResolverTest {

    private val subject = ClientBProfileAvatarArtworkResolver()
    private val expectedArtworkById = linkedMapOf(
        "client-b-avatar-01" to Res.drawable.clientb_profile_avatar_01,
        "client-b-avatar-02" to Res.drawable.clientb_profile_avatar_02,
        "client-b-avatar-03" to Res.drawable.clientb_profile_avatar_03,
        "client-b-avatar-04" to Res.drawable.clientb_profile_avatar_04,
        "client-b-avatar-05" to Res.drawable.clientb_profile_avatar_05,
        "client-b-avatar-06" to Res.drawable.clientb_profile_avatar_06,
        "client-b-avatar-07" to Res.drawable.clientb_profile_avatar_07,
        "client-b-avatar-08" to Res.drawable.clientb_profile_avatar_08,
        "client-b-avatar-09" to Res.drawable.clientb_profile_avatar_09,
    )

    @Test
    fun `every configured avatar ID resolves to unique catalog artwork`() {
        assertEquals(9, expectedArtworkById.values.toSet().size)
        assertEquals(9, subject.supportedAvatarIds.size)
        assertEquals(expectedArtworkById.keys, subject.supportedAvatarIds)
        expectedArtworkById.forEach { (avatarId, expectedArtwork) ->
            assertEquals(expectedArtwork, subject.resolve(avatarId))
        }
    }

    @Test
    fun `unknown avatar ID does not resolve`() {
        assertNull(subject.resolve("client-b-avatar-unknown"))
    }
}
