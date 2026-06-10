package com.pampoukidis.streamcoretv.data.client.catalog

import com.pampoukidis.streamcoretv.core.model.content.RowStyle
import com.pampoukidis.streamcoretv.data.client.model.ClientACastDto
import com.pampoukidis.streamcoretv.data.client.model.ClientAContentDto
import com.pampoukidis.streamcoretv.data.client.model.ClientAGenreDto
import com.pampoukidis.streamcoretv.data.client.model.ClientAHomeRowDto
import com.pampoukidis.streamcoretv.data.client.model.ClientARowPresentationDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogMapperTest {

    @Test
    fun `maps client A content into backend agnostic content model`() {
        val dto = contentDto()

        val model = dto.toModel()

        assertEquals("content-1", model.id)
        assertEquals("Title", model.title)
        assertEquals("Lead Actor", model.cast.single().name)
        assertEquals("Drama", model.genres.single().name)
    }

    @Test
    fun `maps client A row presentation into backend agnostic row style`() {
        val dto = ClientAHomeRowDto(
            rowId = "row-1",
            heading = "Top 10",
            subheading = "Most watched",
            presentation = ClientARowPresentationDto.RankedShelf,
            items = listOf(contentDto()),
        )

        val model = dto.toModel()

        assertEquals("row-1", model.id)
        assertEquals(RowStyle.TopTen, model.style)
        assertEquals("content-1", model.content.single().id)
    }

    private fun contentDto(): ClientAContentDto {
        return ClientAContentDto(
            contentId = "content-1",
            name = "Title",
            synopsis = "Description",
            score = 9,
            maturityLabel = "PG-13",
            maturityRank = 13,
            posterUrl = "poster",
            heroImageUrl = "backdrop",
            publishedAtEpochMs = 123L,
            cast = listOf(
                ClientACastDto(
                    personId = "person-1",
                    fullName = "Lead Actor",
                    character = "Lead",
                    portraitUrl = "portrait",
                ),
            ),
            genres = listOf(
                ClientAGenreDto(genreId = "drama", label = "Drama"),
            ),
        )
    }
}