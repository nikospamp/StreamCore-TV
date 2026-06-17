package com.pampoukidis.streamcoretv.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.pampoukidis.streamcoretv.core.model.content.ContentModel
import kotlinx.serialization.json.Json

object ContentModelNavType : NavType<ContentModel?>(
    isNullableAllowed = true,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun put(bundle: Bundle, key: String, value: ContentModel?) {
        bundle.putString(key, value?.let { content -> json.encodeToString(content) })
    }

    override fun get(bundle: Bundle, key: String): ContentModel? {
        return bundle.getString(key)?.let { value ->
            json.decodeFromString<ContentModel>(value)
        }
    }

    override fun parseValue(value: String): ContentModel {
        return json.decodeFromString<ContentModel>(Uri.decode(value))
    }

    override fun serializeAsValue(value: ContentModel?): String {
        return value?.let { content ->
            Uri.encode(json.encodeToString(content))
        } ?: "null"
    }
}