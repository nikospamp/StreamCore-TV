package com.pampoukidis.streamcore.sdk.runtime.storage.migration

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.runtime.accountStorageKey
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

/** Migrates one legacy payload, preserving its bytes and committing its completion marker transactionally. */
internal suspend fun migrateLegacyPreferences(
    store: DataStore<Preferences>, json: Json, keyName: String, mapName: String?,
    configuration: StreamCoreConfiguration, trustedOwner: String?,
) {
    val dataKey = stringPreferencesKey(keyName)
    val backupKey = stringPreferencesKey("sdk_v1_backup_$keyName")
    val ownerKey = stringPreferencesKey("sdk_v2_migration_owner_$keyName")
    val quarantineKey = stringPreferencesKey("sdk_v2_unowned_$keyName")
    store.edit { values ->
        if (values[ownerKey] != null) return@edit
        val encoded = values[dataKey]
        if (encoded == null) {
            values[ownerKey] = "empty"
            return@edit
        }
        if (values[backupKey] == null) values[backupKey] = encoded
        if (trustedOwner == null) {
            // Preserve bytes without allowing the next signed-in account to claim them.
            values[quarantineKey] = encoded
            values[dataKey] = if (mapName != null) {
                JsonObject(mapOf(mapName to JsonObject(emptyMap()), "version" to JsonPrimitive(2))).toString()
            } else {
                "[]"
            }
            values[ownerKey] = "unowned"
            return@edit
        }
        val parsed = json.parseToJsonElement(encoded)
        val migrated = if (mapName != null) {
            val document = parsed.jsonObject
            val byProfile = document[mapName]?.jsonObject ?: JsonObject(emptyMap())
            val destination = mutableMapOf<String, JsonElement>()
            byProfile.forEach { (profileId, entries) ->
                val partition = accountStorageKey(configuration, trustedOwner, profileId)
                destination[partition] = entries
            }
            JsonObject(document + (mapName to JsonObject(destination)) + ("version" to JsonPrimitive(2)))
        } else {
            JsonArray(parsed.jsonArray.map { element ->
                val entry = element.jsonObject
                val profileId = entry["profileId"]?.jsonPrimitive?.content ?: throw SerializationException("Missing legacy profile")
                JsonObject(entry + ("profileId" to JsonPrimitive(accountStorageKey(configuration, trustedOwner, profileId))))
            })
        }
        // Backup, transformed bytes, and completion marker commit in the same store transaction.
        // Separate stores can retry independently after interruption.
        values[dataKey] = migrated.toString()
        values[ownerKey] = trustedOwner
    }
}
