package com.pampoukidis.streamcoretv.data.tmdb.catalog

import com.pampoukidis.streamcoretv.core.model.content.RowStyle
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbCastDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbContentDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbGenreDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbHomeRowDto
import com.pampoukidis.streamcoretv.data.tmdb.model.TmdbRowPresentationDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogMapperTest {

    @Test
    fun `maps TMDB content into backend agnostic content model`() {
        val dto = contentDto()

        val model = dto.toModel()

        assertEquals("content-1", model.id)
        assertEquals("Title", model.title)
        assertEquals("Lead Actor", model.cast.single().name)
        assertEquals("Drama", model.genres.single().name)
    }

    @Test
    fun `maps TMDB row presentation into backend agnostic row style`() {
        val dto = TmdbHomeRowDto(
            rowId = "row-1",
            heading = "Top 10",
            subheading = "Most watched",
            presentation = TmdbRowPresentationDto.RankedShelf,
            items = listOf(contentDto()),
        )

        val model = dto.toModel()

        assertEquals("row-1", model.id)
        assertEquals(RowStyle.TopTen, model.style)
        assertEquals("content-1", model.content.single().id)
    }

    private fun contentDto(): TmdbContentDto {
        return TmdbContentDto(
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
                TmdbCastDto(
                    personId = "person-1",
                    fullName = "Lead Actor",
                    character = "Lead",
                    portraitUrl = "portrait",
                ),
            ),
            genres = listOf(
                TmdbGenreDto(genreId = "drama", label = "Drama"),
            ),
        )
    }
}
