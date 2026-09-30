package com.pampoukidis.streamcore.sdk.testing

import com.pampoukidis.streamcore.sdk.model.profile.StreamCoreCreateProfile
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackProgressEntry
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackRequest
import com.pampoukidis.streamcore.sdk.api.StreamCoreClient
import com.pampoukidis.streamcore.sdk.model.playback.StreamCorePlaybackSupport
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/** Shared factory contract; call from a provider's test source set using only its wrapped public client. */
object ProviderContract {
    suspend fun verifyReferenceJourney(
        client: StreamCoreClient,
        identifier: String,
        password: String,
        expectedAccountId: String,
    ) {
        try {
            check(client.configuration.expectedAccountId == null)
            check(!client.context.value.isBootstrapped)
            client.bootstrap().contractValue()
            check(client.context.value.account == null)
            check((client.auth.login(" ", " ") as? StreamCoreResult.Failure)?.error is StreamCoreError.Validation)
            check((client.auth.loginWithQr("qr") as? StreamCoreResult.Failure)?.error is StreamCoreError.Unsupported)
            check((client.auth.recoverPassword("user@example.test") as? StreamCoreResult.Failure)?.error is StreamCoreError.Unsupported)

            client.auth.login(identifier, password).contractValue()
            check(client.context.value.account?.id == expectedAccountId)
            check(expectedAccountId.isNotBlank())
            val profiles = client.profiles.getProfiles().contractValue()
            val profile = profiles.first { !it.isKidsProfile }
            client.profiles.selectProfile(profile.id).contractValue()
            check(client.context.value.profile?.id == profile.id)
            val options = client.profiles.getProfileEditorOptions().contractValue()
            val invalidProfile = StreamCoreCreateProfile(" ", options.avatars.first().id, options.parentalLevels.first().id)
            check((client.profiles.createProfile(invalidProfile) as? StreamCoreResult.Failure)?.error is StreamCoreError.Validation)

            val content = client.home.getCollections(profile.id).contractValue().flatMap { it.content }.first()
            check(client.details.getDetails(profile.id, content.id).contractValue().id == content.id)
            check(client.search.search(profile.id, "  ${content.title}  ").contractValue().any { it.id == content.id })
            client.details.getRecommendations(profile.id, content.id).contractValue()
            client.search.recordHistory(profile.id, "  example   query  ").contractValue()
            client.search.recordHistory(profile.id, "EXAMPLE query").contractValue()
            check(client.search.observeHistory(profile.id).first().contractValue() == listOf("EXAMPLE query"))

            client.library.setLiked(profile.id, content, true).contractValue()
            client.library.setInMyList(profile.id, content, true).contractValue()
            val membership = client.library.observeContentState(profile.id, content.id).first().contractValue()
            check(membership.isLiked && membership.isInMyList)
            val now = Clock.System.now().toEpochMilliseconds()
            val progress = StreamCorePlaybackProgressEntry(profile.id, content.id, content, 29_999L, 100_000L, now)
            client.playback.updateProgress(progress).contractValue()
            check(client.playback.getProgress(profile.id, content.id).contractValue() == null)
            client.playback.updateProgress(progress.copy(positionMillis = 30_000L)).contractValue()
            check(client.playback.getProgress(profile.id, content.id).contractValue()?.positionMillis == 30_000L)
            client.playback.updateProgress(progress.copy(positionMillis = 95_000L)).contractValue()
            check(client.playback.getProgress(profile.id, content.id).contractValue() == null)
            client.playback.updateProgress(progress.copy(positionMillis = 40_000L)).contractValue()

            check(client.capabilities.playback == StreamCorePlaybackSupport.DemoMedia)
            val source = client.playback.resolveSource(StreamCorePlaybackRequest(profile.id, content.id, content)).contractValue()
            check(source.assetId.startsWith("demo:"))
            check(!source.uri.isNullOrBlank())
            client.auth.logout().contractValue()
            check(client.context.value.account == null && client.context.value.profile == null)
            client.auth.login(identifier, password).contractValue()
            check(client.context.value.account?.id == expectedAccountId)
            client.profiles.selectProfile(profile.id).contractValue()
            check(client.search.observeHistory(profile.id).first().contractValue() == listOf("EXAMPLE query"))
            check(client.library.observeContentState(profile.id, content.id).first().contractValue().isInMyList)
            check(client.playback.getProgress(profile.id, content.id).contractValue()?.positionMillis == 40_000L)
            client.auth.logout().contractValue()
        } finally {
            client.close()
            client.close()
        }
        check(client.context.value.isClosed)
    }

    suspend fun verifyPlaybackRequiresExplicitOptIn(client: StreamCoreClient, identifier: String, password: String) {
        try {
            client.bootstrap().contractValue()
            client.auth.login(identifier, password).contractValue()
            val profile = client.profiles.getProfiles().contractValue().first { !it.isKidsProfile }
            client.profiles.selectProfile(profile.id).contractValue()
            val content = client.home.getCollections(profile.id).contractValue().flatMap { it.content }.first()
            check(client.capabilities.playback == StreamCorePlaybackSupport.Unsupported)
            val result = client.playback.resolveSource(StreamCorePlaybackRequest(profile.id, content.id, content))
            check((result as? StreamCoreResult.Failure)?.error is StreamCoreError.Unsupported)
        } finally {
            client.close()
        }
    }
}
