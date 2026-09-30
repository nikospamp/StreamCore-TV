package com.example.streamcore.consumer

import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEvent
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryReady
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryChooseProfile
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryPinRequired
import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreProfileEntryNoProfiles
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackSupport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/** This source compiles with provider coordinates alone; it cannot see checkout implementation classes. */
suspend fun exerciseClient(
    client: StreamCoreClient,
    identifier: String = "external@example.test",
    password: String = "reference-password",
) {
    try {
        check(client.configuration.expectedAccountId == null)
        client.bootstrap().valueOrThrow()
        client.auth.login(identifier, password).valueOrThrow()
        val accountId = checkNotNull(client.context.value.account).id
        check(accountId.isNotBlank())
        val profile = activateEntry(client)
        val rows = client.home.getCollections(profile.id).valueOrThrow()
        val content = rows.flatMap { it.content }.first { it.title.trim().length >= 3 }
        client.details.getDetails(profile.id, content.id).valueOrThrow()
        client.search.search(profile.id, content.title).valueOrThrow()
        client.search.recordHistory(profile.id, "  external   search  ").valueOrThrow()
        check("external search" in client.search.observeHistory(profile.id).first().valueOrThrow())
        client.library.setInMyList(profile.id, content, true).valueOrThrow()
        check(client.library.observe(profile.id).first().valueOrThrow().myListContent.any { it.id == content.id })
        client.playback.updateProgress(
            StreamCorePlaybackProgressEntry(
                profileId = profile.id,
                contentId = content.id,
                contentSnapshot = content,
                positionMillis = 30_000L,
                durationMillis = 120_000L,
                updatedAtMillis = Clock.System.now().toEpochMilliseconds(),
            ),
        ).valueOrThrow()
        check(client.playback.getProgress(profile.id, content.id).valueOrThrow() != null)
        check(client.capabilities.playback == StreamCorePlaybackSupport.DemoMedia)
        val request = StreamCorePlaybackRequest(profile.id, content.id, content)
        client.playback.resolveSource(request).valueOrThrow()
        val recorder = client.playback.createProgressRecorder(request, initialPositionMillis = 30_000L)
        recorder.reportEvent(StreamCorePlaybackProgressEvent.Checkpoint, 45_000L, 120_000L).valueOrThrow()
        check(client.playback.getProgress(profile.id, content.id).valueOrThrow()?.positionMillis == 45_000L)
        recorder.reportEvent(StreamCorePlaybackProgressEvent.Completed, 120_000L, 120_000L).valueOrThrow()
        check(client.playback.getProgress(profile.id, content.id).valueOrThrow() == null)
        client.auth.logout().valueOrThrow()
        check(client.context.value.account == null)
        client.auth.login(identifier, password).valueOrThrow()
        check(client.context.value.account?.id == accountId)
        client.auth.logout().valueOrThrow()
    } finally {
        try {
            // Attempt to revoke only this journey's session, and report a failed cleanup safely.
            if (client.context.value.account != null) {
                withContext(NonCancellable) {
                    when (val cleanup = client.auth.logout()) {
                        is StreamCoreResult.Success -> Unit
                        is StreamCoreResult.Failure -> error("SDK session cleanup failed: ${cleanup.error::class.simpleName}")
                    }
                }
            }
        } finally {
            client.close()
            client.close()
        }
    }
}

private fun <T> StreamCoreResult<T>.valueOrThrow(): T {
    return when (this) {
        is StreamCoreResult.Success -> value
        is StreamCoreResult.Failure -> error("SDK operation failed: ${error::class.simpleName}")
    }
}

private suspend fun activateEntry(client: StreamCoreClient): StreamCoreProfile {
    return when (val entry = client.profiles.beginEntry().valueOrThrow()) {
        is StreamCoreProfileEntryReady -> entry.profile
        is StreamCoreProfileEntryChooseProfile -> {
            when (val selection = client.profiles.selectProfile(entry.profiles.first().id).valueOrThrow()) {
                is StreamCoreProfileEntryReady -> selection.profile
                is StreamCoreProfileEntryPinRequired -> error("Profile PIN input is required before content access")
            }
        }
        is StreamCoreProfileEntryPinRequired -> error("Profile PIN input is required before content access")
        StreamCoreProfileEntryNoProfiles -> error("The authenticated account has no profiles")
    }
}
