package com.pampoukidis.streamcoretv.client.clientb.data.profile

import com.pampoukidis.streamcoretv.client.clientb.data.model.ProfileAvatarDto
import com.pampoukidis.streamcoretv.client.clientb.data.model.ProfileDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileMapperTest {

    @Test
    fun `network avatar URL is retained when avatar option is mapped`() {
        val dto = ProfileAvatarDto(
            id = "client-b-avatar-01",
            imageUrl = "https://client-b.example/avatar-01.png",
        )

        val model = dto.toModel()

        assertEquals(dto.id, model.id)
        assertEquals(dto.imageUrl, model.imageUrl)
    }

    @Test
    fun `network avatar URL is retained when profile is mapped`() {
        val dto = profileDto(avatarUrl = "https://client-b.example/profile-avatar.png")

        val model = dto.toModel()

        assertEquals(dto.avatarId, model.avatar.id)
        assertEquals(dto.avatarUrl, model.avatar.imageUrl)
    }

    @Test
    fun `null and blank avatar URLs retain the local fallback ID`() {
        val nullUrlModel = ProfileAvatarDto(
            id = "client-b-avatar-02",
            imageUrl = null,
        ).toModel()
        val blankUrlModel = ProfileAvatarDto(
            id = "client-b-avatar-03",
            imageUrl = "   ",
        ).toModel()

        assertEquals("client-b-avatar-02", nullUrlModel.id)
        assertNull(nullUrlModel.imageUrl)
        assertEquals("client-b-avatar-03", blankUrlModel.id)
        assertEquals("   ", blankUrlModel.imageUrl)
    }

    private fun profileDto(avatarUrl: String?): ProfileDto {
        return ProfileDto(
            id = "profile-01",
            displayName = "Profile",
            avatarId = "client-b-avatar-01",
            avatarUrl = avatarUrl,
            parentalLevelId = "all",
            parentalLevelLabel = "All",
            parentalLevelRank = 0,
            canDelete = true,
            isKidsProfile = false,
        )
    }
}
