package com.pampoukidis.streamcoretv.client.tmdb.ui.avatar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import streamcoretv.client.tmdb.ui.generated.resources.Res
import streamcoretv.client.tmdb.ui.generated.resources.*

class TmdbProfileAvatarArtworkResolverTest {

    private val subject = TmdbProfileAvatarArtworkResolver()
    private val expectedArtworkById = linkedMapOf(
        "tmdb-avatar-01" to Res.drawable.tmdb_profile_avatar_01,
        "tmdb-avatar-02" to Res.drawable.tmdb_profile_avatar_02,
        "tmdb-avatar-03" to Res.drawable.tmdb_profile_avatar_03,
        "tmdb-avatar-04" to Res.drawable.tmdb_profile_avatar_04,
        "tmdb-avatar-05" to Res.drawable.tmdb_profile_avatar_05,
        "tmdb-avatar-06" to Res.drawable.tmdb_profile_avatar_06,
        "tmdb-avatar-07" to Res.drawable.tmdb_profile_avatar_07,
        "tmdb-avatar-08" to Res.drawable.tmdb_profile_avatar_08,
        "tmdb-avatar-09" to Res.drawable.tmdb_profile_avatar_09,
        "tmdb-avatar-10" to Res.drawable.tmdb_profile_avatar_10,
        "tmdb-avatar-11" to Res.drawable.tmdb_profile_avatar_11,
        "tmdb-avatar-12" to Res.drawable.tmdb_profile_avatar_12,
        "tmdb-avatar-13" to Res.drawable.tmdb_profile_avatar_13,
        "tmdb-avatar-14" to Res.drawable.tmdb_profile_avatar_14,
        "tmdb-avatar-15" to Res.drawable.tmdb_profile_avatar_15,
        "tmdb-avatar-16" to Res.drawable.tmdb_profile_avatar_16,
        "tmdb-avatar-17" to Res.drawable.tmdb_profile_avatar_17,
        "tmdb-avatar-18" to Res.drawable.tmdb_profile_avatar_18,
        "tmdb-avatar-19" to Res.drawable.tmdb_profile_avatar_19,
        "tmdb-avatar-20" to Res.drawable.tmdb_profile_avatar_20,
    )

    @Test
    fun `every configured avatar ID resolves to unique catalog artwork`() {
        assertEquals(20, expectedArtworkById.values.toSet().size)
        assertEquals(20, subject.supportedAvatarIds.size)
        assertEquals(expectedArtworkById.keys, subject.supportedAvatarIds)
        expectedArtworkById.forEach { (avatarId, expectedArtwork) ->
            assertEquals(expectedArtwork, subject.resolve(avatarId))
        }
    }

    @Test
    fun `unknown avatar ID does not resolve`() {
        assertNull(subject.resolve("tmdb-avatar-unknown"))
    }
}
