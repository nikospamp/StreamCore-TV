package com.pampoukidis.streamcore.sdk.model.catalog

import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LegacyModelSerializationTest {
    private val persistenceJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun legacyProgressAndRequestPayloadsRetainTheirExactJsonShape() {
        // These fixed property names predate the public Kotlin type rename. The stored JSON
        // has no model-class discriminator, including inside cast, genre and trailer snapshots.
        val legacyEntries = """
            [{
              "profileId":"προφίλ-日本",
              "contentId":"movie-1",
              "contentSnapshot":$legacyContent,
              "positionMillis":45000,
              "durationMillis":120000,
              "updatedAtMillis":1704067200123
            }]
        """.trimIndent()
        val entries = persistenceJson.decodeFromString<List<StreamCorePlaybackProgressEntry>>(legacyEntries)
        val entry = entries.single()

        assertEquals("προφίλ-日本", entry.profileId)
        assertEquals(1_704_067_200_123L, entry.updatedAtMillis)
        assertEquals("Ηθοποιός 日本語", entry.contentSnapshot.cast.single().name)
        assertEquals("Δράμα", entry.contentSnapshot.genres.single().name)
        assertEquals(45_000L, entry.contentSnapshot.playbackProgress?.positionMillis)
        assertEquals("Trailer", entry.contentSnapshot.trailers.single().title)
        assertEquals(
            Json.parseToJsonElement(legacyEntries),
            Json.parseToJsonElement(persistenceJson.encodeToString(entries)),
        )

        val legacyRequest = """
            {"profileId":"προφίλ-日本","contentId":"movie-1","contentSnapshot":$legacyContent}
        """.trimIndent()
        val request = persistenceJson.decodeFromString<StreamCorePlaybackRequest>(legacyRequest)
        assertEquals(entry.contentSnapshot, request.contentSnapshot)
        assertEquals(
            Json.parseToJsonElement(legacyRequest),
            Json.parseToJsonElement(persistenceJson.encodeToString(request)),
        )
    }

    @Test
    fun legacyContentWithoutOptionalFieldsKeepsDefaults() {
        val legacy = """
            {
              "id":"movie-1","title":"Legacy","description":"","rating":0,
              "pgRatingName":"","pgRatingLevel":0,"poster":"","backdrop":null,
              "cast":[],"releaseDate":0,"genres":[]
            }
        """.trimIndent()
        val content = Json.decodeFromString<StreamCoreContent>(legacy)

        assertNull(content.row)
        assertNull(content.playbackProgress)
        assertEquals(emptyList(), content.trailers)
        assertEquals(Json.parseToJsonElement(legacy), Json.parseToJsonElement(Json.encodeToString(content)))
    }

    private companion object {
        val legacyContent = """
            {
              "id":"movie-1","title":"Ταινία 日本語","description":"Legacy snapshot","rating":8,
              "pgRatingName":"PG-13","pgRatingLevel":13,"poster":"/poster.jpg","backdrop":null,
              "cast":[{"id":"cast-1","name":"Ηθοποιός 日本語","characterName":null,"image":null}],
              "releaseDate":1704067200000,"genres":[{"id":"18","name":"Δράμα"}],
              "row":"featured","playbackProgress":{"positionMillis":45000,"durationMillis":120000},
              "trailers":[{"id":"trailer-1","title":"Trailer","url":"https://example.test/trailer"}]
            }
        """.trimIndent()
    }
}
