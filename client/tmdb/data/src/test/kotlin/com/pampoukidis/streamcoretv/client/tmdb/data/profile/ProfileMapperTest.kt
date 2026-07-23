package com.pampoukidis.streamcoretv.client.tmdb.data.profile

import com.pampoukidis.streamcoretv.client.tmdb.data.model.ProfileAvatarDto
import com.pampoukidis.streamcoretv.client.tmdb.data.model.ProfileDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileMapperTest {

    @Test
    fun `network avatar URL is retained when avatar option is mapped`() {
        val dto = ProfileAvatarDto(
            id = "tmdb-avatar-01",
            imageUrl = "https://image.tmdb.org/avatar-01.png",
        )

        val model = dto.toModel()

        assertEquals(dto.id, model.id)
        assertEquals(dto.imageUrl, model.imageUrl)
    }

    @Test
    fun `network avatar URL is retained when profile is mapped`() {
        val dto = profileDto(avatarUrl = "https://image.tmdb.org/profile-avatar.png")

        val model = dto.toModel()

        assertEquals(dto.avatarId, model.avatar.id)
        assertEquals(dto.avatarUrl, model.avatar.imageUrl)
    }

    @Test
    fun `null and blank avatar URLs retain the local fallback ID`() {
        val nullUrlModel = ProfileAvatarDto(
            id = "tmdb-avatar-02",
            imageUrl = null,
        ).toModel()
        val blankUrlModel = ProfileAvatarDto(
            id = "tmdb-avatar-03",
            imageUrl = "   ",
        ).toModel()

        assertEquals("tmdb-avatar-02", nullUrlModel.id)
        assertNull(nullUrlModel.imageUrl)
        assertEquals("tmdb-avatar-03", blankUrlModel.id)
        assertEquals("   ", blankUrlModel.imageUrl)
    }

    private fun profileDto(avatarUrl: String?): ProfileDto {
        return ProfileDto(
            id = "profile-01",
            displayName = "Profile",
            avatarId = "tmdb-avatar-01",
            avatarUrl = avatarUrl,
            parentalLevelId = "all",
            parentalLevelLabel = "All",
            parentalLevelRank = 0,
            canDelete = true,
            isKidsProfile = false,
        )
    }
}
