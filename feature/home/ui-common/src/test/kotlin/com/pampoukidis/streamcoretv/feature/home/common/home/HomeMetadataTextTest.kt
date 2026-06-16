package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.homeMetadataText
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeMetadataTextTest {

    @Test
    fun `omits unrated fallback certification`() {
        val subject = contentModel(pgRatingName = "NR").homeMetadataText()

        assertEquals("8/10", subject)
    }

    @Test
    fun `omits blank certification`() {
        val subject = contentModel(pgRatingName = " ").homeMetadataText()

        assertEquals("8/10", subject)
    }

    @Test
    fun `keeps known certification`() {
        val subject = contentModel(pgRatingName = "PG-13").homeMetadataText()

        assertEquals("8/10 · PG-13", subject)
    }

    @Test
    fun `keeps all ages certification`() {
        val subject = contentModel(pgRatingName = "All").homeMetadataText()

        assertEquals("8/10 · All", subject)
    }

    private fun contentModel(pgRatingName: String): ContentModel {
        return ContentModel(
            id = "content-1",
            title = "Content",
            description = "Description",
            rating = 8,
            pgRatingName = pgRatingName,
            pgRatingLevel = 0,
            poster = "poster",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
        )
    }
}
