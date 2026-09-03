package com.pampoukidis.streamcoretv.client.tmdb.data.profile

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

internal const val TMDB_PROFILES_PREFERENCES_KEY = "profiles_json"
internal const val TMDB_PROFILES_SCHEMA_VERSION = 1

internal fun tmdbProfilesPreferencesKey(accountId: String): Preferences.Key<String> {
    if (accountId.isBlank()) {
        return stringPreferencesKey("$TMDB_PROFILES_PREFERENCES_KEY.account_unconfigured")
    }
    return stringPreferencesKey(
        "$TMDB_PROFILES_PREFERENCES_KEY.account_configured_${accountId.toStorageKeyComponent()}",
    )
}

private fun String.toStorageKeyComponent(): String {
    return buildString(capacity = length * HEX_CODE_UNIT_LENGTH) {
        this@toStorageKeyComponent.forEach { character ->
            append(character.code.toString(radix = HEX_RADIX).padStart(HEX_CODE_UNIT_LENGTH, '0'))
        }
    }
}

private const val HEX_RADIX = 16
private const val HEX_CODE_UNIT_LENGTH = 4
