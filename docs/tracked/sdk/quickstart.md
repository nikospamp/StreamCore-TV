# First StreamCore SDK journey

Use the simulated ClientB provider to exercise the backend-agnostic SDK without credentials or a backend server. This guide targets `0.1.0-alpha02`; existing consumers should read the [compatibility notes](integration.md#compatibility) and upgrade all SDK artifacts together.

## Get the artifacts

Use the current candidate staged locally through the [maintainer publication workflow](integration.md#maintainer-publication-and-verification), with a fresh repository such as `build/sdk-candidate/maven`; this is not a remote registry release. Obtain the staged repository from the maintainer and check the [verification record](verification.md) for the exact candidate and checks performed. Consumer applications do not run the SDK's internal build tasks.

The complete runnable project is [samples/sdk-consumer](../../../samples/sdk-consumer). Its [settings](../../../samples/sdk-consumer/settings.gradle.kts) accepts `-PsdkRepository=<absolute staged repository path>`. It resolves SDK coordinates only; there is no checkout-source or composite-build substitution. The [headless module](../../../samples/sdk-consumer/headless/build.gradle.kts) runs as Kotlin/Wasm on Node (verified with Node 24), and the [Android module](../../../samples/sdk-consumer/androidApp/build.gradle.kts) demonstrates the platform factory and the same public operations.

In an already configured Kotlin Multiplatform consumer, declare:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.pampoukidis.streamcore:provider-clientb:0.1.0-alpha02")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}
```

API/model/coroutine signature dependencies arrive through the provider. This example does not require Koin, Compose, a player engine, or a direct `sdk-runtime` dependency in the consuming project. The sample pins Kotlin 2.3.21 and AGP 9.1.1; those are candidate verification versions, not a measured minimum-version range. Android SDK libraries declare API 24 minimum; samples compile against 37 and target 36. Optional Compose UI has its own dependency closure. Check [recorded verification](verification.md) for executed build/platform evidence.

## Copy the complete reference journey

The two listings below come from the coordinate-only sample. The journey is reproduced in full; the runner contains its first test. Their canonical sources are [SdkConsumerJourney.kt](../../../samples/sdk-consumer/headless/src/commonMain/kotlin/com/example/streamcore/consumer/SdkConsumerJourney.kt) and [PublishedClientTest.kt](../../../samples/sdk-consumer/headless/src/commonTest/kotlin/com/example/streamcore/consumer/PublishedClientTest.kt). Use those source files when following later API changes. The consumer verification build compiles the source files, rather than treating documentation snippets as build evidence.

Put this first file in the consumer's `commonMain` source set. It handles every operation result, branches on profile-entry/selection outcomes, uses the activated ID for content, and closes the client in `finally`. The sample deliberately fails the journey on an operation error; an application instead maps the same typed error to its form, retry action or navigation state.

```kotlin
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
import kotlin.time.Clock

/** This source compiles with provider coordinates alone; it cannot see checkout implementation classes. */
suspend fun exerciseClient(client: StreamCoreClient) {
    try {
        check(client.configuration.expectedAccountId == null)
        client.auth.restoreSession().valueOrThrow()
        client.auth.login("external@example.test", "reference-password").valueOrThrow()
        val accountId = checkNotNull(client.context.value.account).id
        check(accountId.isNotBlank())
        val profile = activateEntry(client)
        val rows = client.home.getCollections(profile.id).valueOrThrow()
        val content = rows.flatMap { it.content }.first()
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
        client.auth.login("external@example.test", "reference-password").valueOrThrow()
        check(client.context.value.account?.id == accountId)
        client.auth.logout().valueOrThrow()
    } finally {
        client.close()
        client.close()
    }
}

private fun <T> StreamCoreResult<T>.valueOrThrow(): T {
    return when (this) {
        is StreamCoreResult.Success -> value
        is StreamCoreResult.Failure -> error("SDK operation failed: $error")
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
```

Put this runner in `commonTest`. ClientB simulates the supplied login values; they are fixture input, not a real account. Demo playback is explicitly enabled because the journey resolves a sample source.

```kotlin
package com.example.streamcore.consumer

import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBReferenceProfileScenario
import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBSdk
import com.pampoukidis.streamcore.sdk.providers.clientb.ClientBSdkConfiguration
import com.pampoukidis.streamcore.sdk.model.StreamCorePersistenceMode
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test

class PublishedClientTest {
    @Test
    fun headlessWasmExercisesPublishedProvider(): TestResult {
        return runTest {
            val client = ClientBSdk.createInMemory(
                ClientBSdkConfiguration(
                    common = StreamCoreConfiguration("clientb-reference", "wasm-test", persistence = StreamCorePersistenceMode.InMemory),
                    demoPlayback = true,
                ),
            )
            exerciseClient(client)
        }
    }
}
```

Run the sample's headless test target (`:headless:wasmJsNodeTest`) through its independent Gradle build, using the configured Node executable. The sample uses a temporary in-memory client. For an Android application, use `ClientBSdk.createAndroid` with the application's context; for Kotlin/Wasm browser storage, use `createWeb`. Keep one owner per persistent backend/storage namespace and close it when that application/service scope ends.

## Add profile selection and PIN UI

The reference journey automatically chooses the first profile solely to keep a headless test deterministic. Product UI must display `StreamCoreProfileEntryChooseProfile.profiles` and pass the user's chosen ID to `selectProfile`. `StreamCoreProfileEntryReady` means activation already succeeded: navigate once, without selecting again. `StreamCoreProfileEntryPinRequired` means no content access yet; collect PIN input and call `confirmPin(challengeId, pin)`. Handle `PinRejected` without logging out the account; call `cancelPin(challengeId)` if the user leaves the challenge.

The [complete published PIN journey](../../../samples/sdk-consumer/headless/src/commonMain/kotlin/com/example/streamcore/consumer/ReferencePinJourney.kt) and [its tests](../../../samples/sdk-consumer/headless/src/commonTest/kotlin/com/example/streamcore/consumer/PublishedClientTest.kt) exercise wrong/right PIN, profile switching, logout and close. Set `ClientBSdkConfiguration.referenceProfileScenario` to `SingleProtected` or `HouseholdProtected` from `ClientBReferenceProfileScenario`; the opt-in demo PIN is `1234`. `Standard` preserves the existing two unprotected reference profiles; `Single` demonstrates direct entry without a chooser. These are reference fixtures, not production backend authentication.

Call `auth.restoreSession()` explicitly to restore persisted account identity; it returns `StreamCoreResult<StreamCoreContext>` and never restores profile authorization. Direct login is also valid without first restoring a session. `context.isAuthInitialized` distinguishes pending authentication initialization from a completed logged-out state. After login or `auth.restoreSession()`, call `beginEntry`: one unprotected profile activates, one protected profile requires PIN, and multiple profiles require a choice. All ordinary content/state IDs must match the authorized profile. Observe `client.context`; use its `profileActivationId` to reset cached profile presentation across activations. Before explicit switching, `clearSelection` revokes current access. The host also owns and cancels its Flow collection scopes when ending use of the client.

## Connect TMDB

Add `com.pampoukidis.streamcore:provider-tmdb:0.1.0-alpha02`. This function supplies only connection configuration; account identity is discovered during authentication. External consumers normally leave `expectedAccountId` absent.

```kotlin
import com.pampoukidis.streamcore.sdk.model.StreamCoreConfiguration
import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbConnectionConfiguration
import com.pampoukidis.streamcore.sdk.providers.tmdb.TmdbSdkConfiguration

fun tmdbConfiguration(readAccessToken: String): TmdbSdkConfiguration {
    return TmdbSdkConfiguration(
        common = StreamCoreConfiguration(
            backend = "tmdb-production",
            storageNamespace = "my-application",
        ),
        connection = TmdbConnectionConfiguration(
            baseUrl = "https://api.themoviedb.org/3/",
            readAccessToken = readAccessToken,
        ),
        demoPlayback = false,
    )
}
```

Pass the result to `TmdbSdk.createAndroid` or `createWeb`, importing the extension factory from `com.pampoukidis.streamcore.sdk.providers.tmdb`. The [Android consumer](../../../samples/sdk-consumer/androidApp/src/main/kotlin/com/example/streamcore/consumer/ConsumerActivity.kt) includes construction with full imports and optional live-test input. TMDB retains its local adult/kids reference profiles; its optional playback is demonstration media, not a catalogue entitlement. The provider's connection `toString()` redacts its token.

See [integration](integration.md) for error recovery, capability checks, state ownership and optional presentation. `close()` releases resources; call `auth.logout()` when remote logout is intended. Cancellation remains coroutine cancellation. This candidate provides Android/Kotlin-Wasm APIs, not JavaScript/TypeScript or Swift distribution.
