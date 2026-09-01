package com.pampoukidis.streamcoretv.core.model.content

import com.pampoukidis.streamcoretv.core.model.general.Genre
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentModelTimeTest {
    @Test
    fun positiveEpochIncludesUtcReleaseYear() {
        val content = content(releaseDate = 946_684_800_000L)

        assertEquals("2000  ·  Drama  ·  8/10 · PG-13", content.heroMetadata())
    }

    @Test
    fun zeroEpochOmitsReleaseYear() {
        val content = content(releaseDate = 0L)

        assertEquals("Drama  ·  8/10 · PG-13", content.heroMetadata())
    }

    @Test
    fun negativeEpochOmitsReleaseYear() {
        val content = content(releaseDate = -1L)

        assertEquals("Drama  ·  8/10 · PG-13", content.heroMetadata())
    }

    @Test
    fun instantBeforeUtcYearBoundaryUsesPreviousYear() {
        val content = content(releaseDate = 1_704_067_199_999L)

        assertEquals("2023  ·  Drama  ·  8/10 · PG-13", content.heroMetadata())
    }

    @Test
    fun instantAtUtcYearBoundaryUsesNewYear() {
        val content = content(releaseDate = 1_704_067_200_000L)

        assertEquals("2024  ·  Drama  ·  8/10 · PG-13", content.heroMetadata())
    }

    private fun content(releaseDate: Long): ContentModel {
        return ContentModel(
            id = "content-1",
            title = "Title",
            description = "Description",
            rating = 8,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "/poster.jpg",
            backdrop = "/backdrop.jpg",
            cast = emptyList(),
            releaseDate = releaseDate,
            genres = listOf(Genre(id = "18", name = "Drama")),
        )
    }
}
