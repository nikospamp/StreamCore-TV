package com.pampoukidis.streamcoretv.feature.home.common.home

import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import com.pampoukidis.streamcoretv.core.model.content.PlaybackProgressModel
import com.pampoukidis.streamcoretv.core.model.content.RowModel
import com.pampoukidis.streamcoretv.core.model.content.RowType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeRowsMapperTest {

    @Test
    fun `explicit featured row wins and remaining shelf order is preserved`() {
        val rows = listOf(
            row("poster", RowType.Poster),
            row("featured", RowType.Featured),
            row("ranking", RowType.TopTen),
        )

        val result = rows.toHomeContentModel()

        assertEquals("featured-content", result.featured.single().id)
        assertEquals(listOf("poster", "ranking"), result.shelves.map { row -> row.id })
    }

    @Test
    fun `first populated row is featured fallback`() {
        val rows = listOf(
            row("poster", RowType.Poster),
            row("ranking", RowType.TopTen),
        )

        val result = rows.toHomeContentModel()

        assertEquals("poster-content", result.featured.single().id)
        assertEquals(listOf("ranking"), result.shelves.map { row -> row.id })
    }

    @Test
    fun `continue watching excludes content without playback progress`() {
        val continueRow = RowModel(
            id = "continue",
            title = "Bookmarks",
            subtitle = "",
            content = listOf(
                content(
                    id = "with-progress",
                    progress = PlaybackProgressModel(
                        positionMillis = 25L,
                        durationMillis = 100L,
                    ),
                ),
                content(id = "without-progress"),
            ),
            type = RowType.ContinueWatching,
        )

        val result = listOf(
            row("featured", RowType.Featured),
            continueRow,
        ).toHomeContentModel()

        assertEquals("with-progress", result.continueWatching?.content?.single()?.id)
        assertEquals(0.25f, result.continueWatching?.content?.single()?.playbackProgress?.fraction)
    }

    @Test
    fun `continue watching is absent when no item has progress`() {
        val result = listOf(
            row("featured", RowType.Featured),
            row("continue", RowType.ContinueWatching),
        ).toHomeContentModel()

        assertNull(result.continueWatching)
        assertEquals(emptyList<RowModel>(), result.shelves)
    }

    @Test
    fun `playback fraction clamps invalid positions`() {
        assertEquals(
            0f,
            PlaybackProgressModel(
                positionMillis = -10L,
                durationMillis = 100L,
            ).fraction,
        )
        assertEquals(
            1f,
            PlaybackProgressModel(
                positionMillis = 120L,
                durationMillis = 100L,
            ).fraction,
        )
        assertEquals(
            0f,
            PlaybackProgressModel(
                positionMillis = 10L,
                durationMillis = 0L,
            ).fraction,
        )
    }

    private fun row(
        id: String,
        type: RowType,
    ): RowModel {
        return RowModel(
            id = id,
            title = id,
            subtitle = "",
            content = listOf(content(id = "$id-content")),
            type = type,
        )
    }

    private fun content(
        id: String,
        progress: PlaybackProgressModel? = null,
    ): ContentModel {
        return ContentModel(
            id = id,
            title = id,
            description = "",
            rating = 8,
            pgRatingName = "13+",
            pgRatingLevel = 13,
            poster = "",
            backdrop = null,
            cast = emptyList(),
            releaseDate = 0L,
            genres = emptyList(),
            playbackProgress = progress,
        )
    }
}
