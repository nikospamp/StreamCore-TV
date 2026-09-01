package com.pampoukidis.streamcoretv.core.model.content

import com.pampoukidis.streamcoretv.core.model.general.Cast
import com.pampoukidis.streamcoretv.core.model.general.Genre
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreModelSerializationTest {
    @Test
    fun serializedContentRetainsPublicFieldNames() {
        val encoded = Json.encodeToString(content())
        val fields = Json.parseToJsonElement(encoded).jsonObject.keys

        assertTrue(
            fields.containsAll(
                setOf(
                    "id",
                    "title",
                    "description",
                    "rating",
                    "pgRatingName",
                    "pgRatingLevel",
                    "poster",
                    "backdrop",
                    "cast",
                    "releaseDate",
                    "genres",
                ),
            ),
        )
    }

    @Test
    fun contentRoundTripRetainsEpochMilliseconds() {
        val original = content()

        val decoded = Json.decodeFromString<ContentModel>(Json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals(1_704_067_200_000L, decoded.releaseDate)
    }

    private fun content(): ContentModel {
        return ContentModel(
            id = "content-1",
            title = "Title",
            description = "Description",
            rating = 8,
            pgRatingName = "PG-13",
            pgRatingLevel = 13,
            poster = "/poster.jpg",
            backdrop = "/backdrop.jpg",
            cast = listOf(Cast("cast-1", "Actor", "Role", null)),
            releaseDate = 1_704_067_200_000L,
            genres = listOf(Genre("18", "Drama")),
            row = "featured",
            playbackProgress = PlaybackProgressModel(1_000L, 10_000L),
            trailers = listOf(TrailerModel("trailer-1", "Trailer", "https://example.test")),
        )
    }
}
