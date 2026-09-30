package com.example.streamcore.uiresources

import com.pampoukidis.streamcore.sdk.providers.clientb.ui.avatar.ClientBProfileAvatarArtworkResolver
import com.pampoukidis.streamcore.sdk.providers.clientb.ui.error.ClientBErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.avatar.TmdbProfileAvatarArtworkResolver
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.error.TmdbErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.ui.avatar.ProfileAvatarArtworkResolver
import com.pampoukidis.streamcore.sdk.ui.error.DefaultErrorPresentationMapper
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment

/** Loads actual published payloads; a non-null generated resource handle alone is insufficient. */
suspend fun verifyPublishedUiResources() {
    val tmdbAvatars: ProfileAvatarArtworkResolver = TmdbProfileAvatarArtworkResolver()
    val clientBAvatars: ProfileAvatarArtworkResolver = ClientBProfileAvatarArtworkResolver()
    val environment = getSystemResourceEnvironment()
    for (index in 1..20) {
        val id = "tmdb-avatar-" + index.toString().padStart(2, '0')
        val resource = checkNotNull(tmdbAvatars.resolve(id))
        val xml = getDrawableResourceBytes(environment, resource).decodeToString()
        check("<vector" in xml && "</vector>" in xml) { "Missing TMDB avatar payload: $id" }
    }
    for (index in 1..9) {
        val id = "client-b-avatar-" + index.toString().padStart(2, '0')
        val resource = checkNotNull(clientBAvatars.resolve(id))
        val xml = getDrawableResourceBytes(environment, resource).decodeToString()
        check("<vector" in xml && "</vector>" in xml) { "Missing ClientB avatar payload: $id" }
    }
    check(tmdbAvatars.resolve("unknown") == null)
    check(clientBAvatars.resolve("unknown") == null)

    val defaultMapper = DefaultErrorPresentationMapper()
    val tmdbError = TmdbErrorPresentationMapper(defaultMapper).map(StreamCoreError.Authentication())
    check(getString(tmdbError.title) == "Sign-in failed")
    check(getString(tmdbError.message) == "Check your TMDB username and password, then try again.")
    check(getString(tmdbError.confirmAction) == "OK")
    val clientBError = ClientBErrorPresentationMapper(defaultMapper).map(StreamCoreError.Network())
    check(getString(clientBError.title) == "Connection issue")
    check(getString(clientBError.message) == "Check your connection and try again.")
    check(getString(clientBError.confirmAction) == "OK")
    val expired = defaultMapper.map(StreamCoreError.SessionExpired())
    check(!expired.dismissible)
    check(getString(expired.title) == "Session expired")
}

