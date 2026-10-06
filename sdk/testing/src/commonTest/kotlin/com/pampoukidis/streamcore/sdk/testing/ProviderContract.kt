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

/**
 * Shared behavior checks used by `TmdbSdkContractTest` and `ClientBSdkContractTest`.
 * Each provider's `commonTest` creates a client through its SDK factory and passes that public
 * [StreamCoreClient] here, so both providers run the same checks against the backend-agnostic API.
 * Provider-specific fixture setup and `@Test` functions stay in the provider modules.
 *
 * Both providers add `sdk/testing/src/commonTest/kotlin` to their `commonTest` source directories.
 * This keeps one shared copy while compiling it as test code in each provider. `:sdk:testing`
 * applies only Gradle's base plugin to give these sources a project identity. It does not compile,
 * run tests or publish a library itself; these helpers never enter SDK artifacts.
 *
 * These journeys cover the current reference providers, not every possible provider. In
 * particular, [verifyReferenceJourney] expects demo media and unsupported QR login/password
 * recovery. Providers with different capabilities need tests matching their declared behavior.
 *
 * Use an isolated test client and storage for each call. The helpers perform authentication and
 * other SDK operations, throw when an expectation fails, and close the supplied client in
 * `finally`, including on failure or cancellation. Closing does not erase the saved test data.
 */
internal object ProviderContract {
    /**
     * Exercises authentication, profile selection/validation, catalogue access, normalized search
     * history, library membership, progress thresholds and demo source resolution. Logs out and
     * back in to check that the account's history, My List membership and progress are retained.
     * This checks retention within one client lifetime, not persistence across process restarts.
     *
     * Supply a fresh client with authentication not yet initialized, no stored session or saved
     * data, no expected-account restriction, and demo playback enabled. Its fixture must support
     * the supplied credentials, an adult profile selectable without a PIN, nonempty avatar and
     * parental-level options, and home content available through details and title search.
     * Recommendations must succeed, but may be empty. QR login and password recovery must return
     * an unsupported result.
     *
     * Writes history, library membership and progress; the final logout runs only if preceding
     * checks succeed. Always calls [StreamCoreClient.close] twice to exercise idempotent closing.
     *
     * @param client Dedicated factory-created client; do not reuse it after this call.
     * @param identifier Valid login identifier for the test account.
     * @param password Valid login password for the test account.
     * @param expectedAccountId Nonblank identity that authentication must discover. This is an
     * assertion value, not the SDK configuration's optional expected-account restriction.
     */
    suspend fun verifyReferenceJourney(
        client: StreamCoreClient,
        identifier: String,
        password: String,
        expectedAccountId: String,
    ) {
        try {
            check(client.configuration.expectedAccountId == null)
            check(!client.context.value.isAuthInitialized)
            client.auth.restoreSession().contractValue()
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

    /**
     * Verifies that disabling demo playback reports an unsupported playback capability and makes
     * source resolution return an unsupported result, even for accessible catalogue content.
     *
     * Supply a dedicated reference-provider client with demo playback disabled, valid test
     * credentials, an adult profile selectable without a PIN, and nonempty home content.
     * Restores authentication, logs in and selects that profile before checking playback.
     * Always closes the client; it does not log out or erase credentials and saved data.
     *
     * @param client Dedicated factory-created client with demo playback disabled; closed on return.
     * @param identifier Valid login identifier for the test account.
     * @param password Valid login password for the test account.
     */
    suspend fun verifyPlaybackRequiresExplicitOptIn(client: StreamCoreClient, identifier: String, password: String) {
        try {
            client.auth.restoreSession().contractValue()
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
