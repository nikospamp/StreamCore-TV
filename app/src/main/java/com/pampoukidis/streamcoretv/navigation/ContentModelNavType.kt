package com.pampoukidis.streamcoretv.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.pampoukidis.streamcore.sdk.model.catalog.StreamCoreContent
import kotlinx.serialization.json.Json

object ContentModelNavType : NavType<StreamCoreContent?>(
    isNullableAllowed = true,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun put(bundle: Bundle, key: String, value: StreamCoreContent?) {
        bundle.putString(key, value?.let { content -> json.encodeToString(content) })
    }

    override fun get(bundle: Bundle, key: String): StreamCoreContent? {
        return bundle.getString(key)?.let { value ->
            json.decodeFromString<StreamCoreContent>(value)
        }
    }

    override fun parseValue(value: String): StreamCoreContent {
        return json.decodeFromString<StreamCoreContent>(Uri.decode(value))
    }

    override fun serializeAsValue(value: StreamCoreContent?): String {
        return value?.let { content ->
            Uri.encode(json.encodeToString(content))
        } ?: "null"
    }
}
