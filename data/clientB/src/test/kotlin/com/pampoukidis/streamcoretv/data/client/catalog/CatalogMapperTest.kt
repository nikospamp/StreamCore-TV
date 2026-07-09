package com.pampoukidis.streamcoretv.data.client.catalog

import com.pampoukidis.streamcoretv.core.model.content.RowType
import com.pampoukidis.streamcoretv.data.client.model.ClientBCategoryDto
import com.pampoukidis.streamcoretv.data.client.model.ClientBContentDto
import com.pampoukidis.streamcoretv.data.client.model.ClientBContributorDto
import com.pampoukidis.streamcoretv.data.client.model.ClientBHomeLaneDto
import com.pampoukidis.streamcoretv.data.client.model.ClientBLaneTemplateDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogMapperTest {

    @Test
    fun `maps client B content into backend agnostic content model`() {
        val dto = contentDto()

        val model = dto.toModel()

        assertEquals("content-1", model.id)
        assertEquals("Title", model.title)
        assertEquals("Lead Actor", model.cast.single().name)
        assertEquals("Drama", model.genres.single().name)
    }

    @Test
    fun `maps client B lane template into backend agnostic row type`() {
        val dto = ClientBHomeLaneDto(
            laneId = "lane-1",
            title = "Spotlight",
            caption = "Featured",
            template = ClientBLaneTemplateDto.Spotlight,
            assets = listOf(contentDto()),
        )

        val model = dto.toModel()

        assertEquals("lane-1", model.id)
        assertEquals(RowType.Featured, model.type)
        assertEquals("content-1", model.content.single().id)
    }

    private fun contentDto(): ClientBContentDto {
        return ClientBContentDto(
            assetId = "content-1",
            displayTitle = "Title",
            description = "Description",
            ratingOutOfTen = 9,
            parentalRating = "13+",
            parentalLevel = 13,
            portraitImage = "poster",
            landscapeImage = "backdrop",
            releaseEpochMillis = 123L,
            contributors = listOf(
                ClientBContributorDto(
                    id = "person-1",
                    displayName = "Lead Actor",
                    roleName = "Lead",
                    imageUrl = "portrait",
                ),
            ),
            categories = listOf(
                ClientBCategoryDto(code = "drama", displayName = "Drama"),
            ),
        )
    }
}
