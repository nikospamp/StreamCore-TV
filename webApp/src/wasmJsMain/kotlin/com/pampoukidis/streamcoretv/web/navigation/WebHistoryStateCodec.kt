package com.pampoukidis.streamcoretv.web.navigation

import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseDestination
import com.pampoukidis.streamcoretv.core.ui.web.WebBrowseFocusKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal object WebHistoryStateCodec {
    private val json = Json

    fun encode(key: WebBrowseFocusKey?): String? {
        if (key == null) {
            return null
        }
        return json.encodeToString(
            WebHistoryStateDto(
                version = CurrentVersion,
                destination = key.destination.historyKey,
                sectionKey = key.sectionKey,
                itemKey = key.itemKey,
            ),
        )
    }

    fun decode(value: String?): WebBrowseFocusKey? {
        if (value.isNullOrBlank()) {
            return null
        }
        return try {
            val state = json.decodeFromString<WebHistoryStateDto>(value)
            if (state.version != CurrentVersion) {
                return null
            }
            val destination = WebBrowseDestination.entries.firstOrNull { candidate ->
                candidate.historyKey == state.destination
            } ?: return null
            WebBrowseFocusKey(
                destination = destination,
                sectionKey = state.sectionKey,
                itemKey = state.itemKey,
            )
        } catch (_: Throwable) {
            null
        }
    }

    private const val CurrentVersion = 1
}

@Serializable
private data class WebHistoryStateDto(
    val version: Int,
    val destination: String,
    val sectionKey: String,
    val itemKey: String,
)
