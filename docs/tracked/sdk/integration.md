# StreamCore SDK integration

StreamCore's target architecture is backend-agnostic. Version `0.1.0-alpha02` exposes one runtime-enforced client for Android and Kotlin/Wasm. Screens, navigation, player engines, focus, and dependency injection remain application-owned. This candidate is staged locally; it does not deliver a JavaScript/TypeScript package or Swift framework.

Start with the [credential-free quickstart](quickstart.md). [Verification](verification.md) records checks actually completed; the instructions here do not establish acceptance by themselves.

## Compatibility

This candidate intentionally breaks earlier Kotlin source/ABI, including earlier alpha02 development snapshots. Upgrade all StreamCore artifacts together and recompile consumers, provider integrations, fakes and tests. There are no forwarding aliases or stable 1.0 compatibility guarantees. Use the current packages and services below; restoration is `client.auth.restoreSession()` and readiness is `client.context.value.isAuthInitialized`.

Remove any dependency on the retired `com.pampoukidis.streamcore:sdk-testing` artifact; external consumers maintain their own test fixtures. The supported publication contains eight artifacts. Current namespaced saved data retains its account/profile partitioning; old development files and browser keys are neither imported nor deleted. There is no legacy-storage configuration or shipped-data migration requirement. See [saved-data ownership](#saved-data-ownership-and-compatibility) before choosing a storage namespace.

## Artifacts and repository structure

All coordinates use group `com.pampoukidis.streamcore` and version `0.1.0-alpha02`.

| Coordinate suffix | Module / purpose |
| --- | --- |
| `sdk-model` | `:sdk:model` — common values, capabilities, inputs and typed errors |
| `sdk-api` | `:sdk:api` — consumer services and pure form validators |
| `sdk-runtime` | `:sdk:runtime` — enforcement, persistence and provider SPI; implementation dependency |
| `provider-tmdb` | `:sdk:providers:tmdb` — TMDB authentication/catalogue and local reference profiles |
| `provider-clientb` | `:sdk:providers:clientB` — simulated reference provider |
| `sdk-ui` | `:sdk:ui` — optional presentation contracts/default wording |
| `provider-tmdb-ui` | `:sdk:providers:tmdb:ui` — optional TMDB artwork and wording |
| `provider-clientb-ui` | `:sdk:providers:clientB:ui` — optional ClientB artwork and wording |

Module directories mirror these paths under `sdk/`. Headless consumers declare the provider artifact: API/model signature dependencies are exported transitively. Add the provider UI artifact separately if needed. Provider implementation depends on runtime → API → model; headless modules never depend on optional SDK UI. Optional UI depends on SDK UI/model/resource APIs, not provider networking or application rendering.

`:sdk:testing` is a ninth registered SDK project, alongside the eight published modules above. It applies only Gradle's `base` plugin and holds `sdk/testing/src/commonTest/kotlin`, which the providers include in their own `commonTest` compilations. It has no Kotlin/Android compilation, standalone test execution or publication. External applications declare only the supported artifacts above and maintain their own consumer tests.

Provider source uses the canonical `com.pampoukidis.streamcore.sdk.providers.<provider>` namespace throughout. Runtime composes internal domain services around one session owner; application consumers continue using the same `StreamCoreClient` services. The [maintainer guide](maintainer-guide.md) identifies implementation entry points without adding those internals to the consumer API.

## Public packages and services

The public package root is `com.pampoukidis.streamcore.sdk`. Factories/configuration live in `providers.tmdb` and `providers.clientb`; services in `api`; validators in `validation`; models in `model` and its `auth`, `profile`, `catalog`, `search`, `library`, `playback`, and `error` subpackages. Optional presentation uses `ui.error`, `ui.avatar`, and `providers.<provider>.ui.*`.

Every top-level type in `sdk:model` uses the `StreamCore` prefix without a trailing `Model` suffix, including inputs, results, enums and sealed types. For example, use `StreamCoreContent`, `StreamCoreProfile`, `StreamCoreResult` and `StreamCorePlaybackProgressEvent`. Nested members keep their names (`StreamCoreResult.Success`, `StreamCoreError.Validation`). API services and optional UI contracts keep their existing names.

| Client member | Service / ordinary use |
| --- | --- |
| `auth` | `AuthService`: `restoreSession`, `login`, `loginWithQr`, `logout`, `recoverPassword` |
| `profiles` | `ProfileService`: entry, selection/PIN activation, account-owned profile management |
| `home` | `HomeService.getCollections(profileId)` |
| `details` | `DetailsService.getDetails` / `getRecommendations` |
| `search` | `SearchService`: query/trending/interactions plus `observeHistory`, `recordHistory`, `removeHistoryQuery`, `clearHistory` |
| `library` | `LibraryService`: saved membership, content state and combined collections |
| `playback` | `PlaybackService`: `resolveSource`, `getProgress`, `observeProgress`, `updateProgress`, `removeProgress`, `createProgressRecorder` |

Use these typed services rather than provider contracts colocated in the runtime domain packages or persistence contracts in `runtime.storage`. Backend ports align with the auth, profile, home, details, search and playback service domains; runtime retains validation, authorization and shared policy enforcement. Public provider-integration declarations sit beside internal implementations without making those implementations consumer API. Library, search history and playback progress remain SDK-local. There is one consumer restoration operation (`client.auth.restoreSession()`) and one state source (`client.context`). `AuthService.restoreSession()` returns `StreamCoreResult<StreamCoreContext>` and is implemented directly by `RuntimeAuthService`; the client has no restoration wrapper. `AuthProvider.restoreSession()` remains a backend SPI operation returning `StreamCoreResult<StreamCoreAuthState>`, without exposing provider state as a second application API.

## Construction, identity and lifetime

`ClientBSdk` and `TmdbSdk` provide `createAndroid(context, config)` and Kotlin/Wasm `createWeb(config, useSessionStorage = false)`. ClientB additionally provides `createInMemory(config)` for credential-free reference journeys. Import platform extension factories from the same provider package as the factory object. Construction performs no network requests. Restoration is explicit: call `client.auth.restoreSession()` to read and validate persisted authentication. Once authentication is initialized, it returns the current context without repeating restoration, including after direct login. Login does not require a prior restore call. The auth-storage availability check remains internal to the authentication workflow so browser startup can select its storage fallback before login.

Give `StreamCoreConfiguration` an explicit backend/environment ID and storage namespace. Keep one persistent owner per backend/namespace; use a distinct namespace for an independent instance. `locale` defaults to `en-US`, `region` to null, and persistence to `Persistent`. Neither the backend/namespace nor the optional expected-account restriction supplies authenticated identity. TMDB discovers identity from the authenticated `/account` response. External consumers normally omit `expectedAccountId`; connection configuration has no account-ID field.

Observe `client.context` for `isAuthInitialized`, `account`, authorized `profile`, `isClosed`, and `profileActivationId`. Do not interpret an initial null account before authentication initialization as a completed logged-out decision. `StreamCoreContextFailureReason.AuthNotInitialized` identifies access attempted before authentication initialization. `profileActivationId` changes each time access is activated: it is an in-memory scope identity, not a backend token or persistence key. Applications can use it to discard profile content/ViewModels from an earlier activation, even when the profile ID is unchanged.

Successful logout clears credentials and active context while retaining account-owned saved data. A transient remote logout failure retains the session for an explicit retry; authoritative rejection is classified by the SDK. `close()` releases owned resources without logout or data erasure and is idempotent. Put close in the owning application's/service's cleanup. Hosts own their collection scopes and must cancel them: closing the client does not complete `context` or release a host's Flow collector. In-flight SDK work can be cancelled by close. Coroutine cancellation propagates; do not convert it to success or a normal failure result.

## Profile entry and PIN activation

After login or account restoration into a fresh client, call `profiles.beginEntry()`:

| Successful outcome | Application action |
| --- | --- |
| `StreamCoreProfileEntryReady(profile)` | Access is already active; navigate to Home |
| `StreamCoreProfileEntryChooseProfile(profiles)` | Display the chooser; pass the chosen ID to `selectProfile` |
| `StreamCoreProfileEntryPinRequired(challenge)` | Display a PIN input for the challenge |
| `StreamCoreProfileEntryNoProfiles` | Present the application's empty/profile-creation state |

Exactly one unprotected profile activates directly. Exactly one protected profile requests a PIN. Multiple profiles require a choice, including TV startup. Fresh session restoration restores account identity, never profile authorization; a remembered profile ID does not skip entry. Background/resume of the same active SDK instance does not itself require another PIN.

`selectProfile(profileId)` returns `StreamCoreProfileEntryReady` or `StreamCoreProfileEntryPinRequired`. `confirmPin(challengeId, pin)` returns the activated `StreamCoreProfile` on success. Do not call selection again from the subsequent navigation callback. The opaque challenge is bound to the SDK instance, account and entry attempt. `cancelPin(challengeId)` invalidates it immediately, including pending verification; keep the PIN only in the UI's temporary input state and clear that state after cancellation/success.

Providers declare `StreamCoreProfile.pinPolicy` and its numeric digit count. `capabilities.profilePinVerification` says whether the provider supports verification. Runtime validates input and the provider verifies it. Wrong PIN yields `StreamCoreError.PinRejected`, optionally with provider-supplied remaining attempts; it does not log the account out. PIN creation, changing, recovery and a generic lockout/cooldown framework are outside this candidate.

Ordinary content and saved-state calls retain explicit profile IDs, which must match the current authorized profile. This applies to home, details, search/history, library, playback sources and progress. Profile listing/editor options/create/update/delete are account-scoped management operations and retain provider permissions. A PIN gate does not reinterpret maturity ratings or prohibit all account-level management.

Before an explicit profile switch, call `profiles.clearSelection()`. Access is revoked in memory before saved-selection cleanup, even if that cleanup returns a storage error. Switching back to a protected profile requires a new PIN. A switch screen can use `getProfiles()` and `selectProfile()`; do not invoke automatic single-profile entry on every editor refresh. Pending old activation results cannot authorize the new profile/session.

TMDB retains its existing local adult/kids demo profiles, without PIN protection. ClientB's opt-in `referenceProfileScenario` is `Standard` by default (unchanged Primary/Family profiles). `Single`, `SingleProtected`, and `HouseholdProtected` exercise one-profile and protected-household entry. Protected reference profiles use demo PIN `1234`; these scenarios are simulated behavior, not production backend coverage. See the [published PIN journey](../../../samples/sdk-consumer/headless/src/commonMain/kotlin/com/example/streamcore/consumer/ReferencePinJourney.kt).

## Results, validation and optional operations

Consumer suspend operations return `StreamCoreResult`; synchronous PIN cancellation also returns `StreamCoreResult`. Branch on `Success.value` / `Failure.error`. Flow-based services emit `StreamCoreResult` values. Construction can reject invalid configuration and cancellation remains an exception; a result type does not mean either is swallowed. Lifecycle `close()` returns `Unit`.

`StreamCoreError.Validation.issues` contains typed `StreamCoreValidationField` / `StreamCoreValidationReason` values. `InvalidContext.reason` distinguishes uninitialized/unauthenticated state, missing/mismatched/unavailable profile, stale session/activation and expired PIN challenge. Use those typed reasons for recovery rather than parsing messages. `StreamCoreErrorSource` contains optional diagnostic metadata. Pure `LoginValidator`, `ProfileValidator` and `SearchQueryNormalizer` support immediate form feedback; runtime remains authoritative when operations are called directly.

Read capabilities before offering optional actions and handle `Unsupported` from the operation as well. Demo playback is false by default. When explicitly enabled, `StreamCorePlaybackSupport.DemoMedia` identifies a sample asset, never the requested catalogue title's media: TMDB resolves `demo:sintel`, ClientB `demo:trickplay`. Without opt-in, source resolution is unsupported.

## Search, library and playback

`search.search(profileId, query, interaction)` defaults to `StreamCoreSearchInteraction.Typing`: typing alone does not record history. Pass `Submitted` or `RecentSelected` for explicit actions; successful nonempty results enter history. Call `displayedResults` when an explicit submission reuses already-loaded results, and `resultSelected` when opening a result. The SDK normalizes queries; debouncing, request deduplication, focus and navigation stay in the application. Combined search keeps successful results if its history side effect fails. Explicit history operations return their own result for the application to handle.

`library` owns membership timestamps and combines liked/my-list/continue-watching content. Home returns ordered `StreamCoreCollection` values with semantic purpose and optional presentation hints. Applications choose their layout; StreamCore's `RowModel`/`RowType` stay application-owned.

Resolve media through `playback.resolveSource`, then use `playback.createProgressRecorder(request, initialPositionMillis)` for that playback. Send player events with `recorder.reportEvent`: `Periodic` uses ten-second position buckets, `Checkpoint` bypasses cadence for pause/seek/exit, and `Completed` removes saved progress. The recorder supplies timestamps and applies resume eligibility (at least 30 seconds, below 95% completion). It skips unknown-duration Periodic/Checkpoint samples, preserving prior progress. A successful result can mean saved, removed or ignored; returned storage failures must not block player exit. Advanced direct `playback.updateProgress(entry)` applies a caller-timestamped snapshot immediately and removes non-resumable entries, including unknown duration. Do not call both paths for one player event. See [the playback guide](playback-progress.md) for the exact event table and examples. Rendering engines and playback sessions remain outside the SDK.

## Optional presentation

Add `com.pampoukidis.streamcore:provider-tmdb-ui:0.1.0-alpha02` (or `provider-clientb-ui`) alongside the headless provider. Its public signatures use Compose `DrawableResource` / `StringResource`; no Koin, application UI, Material controls or player engine are required.

```kotlin
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.avatar.TmdbProfileAvatarArtworkResolver
import com.pampoukidis.streamcore.sdk.providers.tmdb.ui.error.TmdbErrorPresentationMapper
import com.pampoukidis.streamcore.sdk.ui.error.DefaultErrorPresentationMapper

val artwork = TmdbProfileAvatarArtworkResolver()
val errors = TmdbErrorPresentationMapper(DefaultErrorPresentationMapper())
```

Artwork resolution returns null for unknown avatar IDs. Error mapping returns resource-backed `ErrorUiModel`; the application renders it and owns navigation/interaction. SDK configuration `locale` controls backend requests; Compose resource locale controls UI wording. Generated resource packages now follow `com.pampoukidis.streamcore.sdk.ui.generated.resources` and `providers.<provider>.ui.generated.resources`; resource filenames and saved avatar IDs are preserved. [Published UI-only consumers](../../../samples/sdk-consumer/uiWasm/src/commonMain/kotlin/com/example/streamcore/uiresources/PublishedUiResources.kt) verify resource payloads without a headless-provider dependency.

## Saved-data ownership and compatibility

Saved state is partitioned by backend, storage namespace, authenticated account and profile. Providers and applications use the same current namespaced storage defaults. Keep those partition values stable when reopening a current SDK store. Account-owned library, history and progress survive logout; persisted selected-profile context never grants authorization in a fresh client.

There are no shipped-data compatibility requirements. The SDK does not convert old application payloads, infer their account owner, create migration backups/quarantine or select historical application filenames. Old unprefixed development application files and browser keys remain untouched and are not imported. Storage failures still propagate, and the authentication workflow checks auth/context storage availability before proceeding.

Providers interpret their own content classifications. TMDB's kids rule excludes the provider `adult` flag and does not treat numeric age/certification fields as universal maturity policy. Direct details/source requests and saved content use the same provider restriction.

## Maintainer publication and verification

Consumer setup is in the [quickstart](quickstart.md); this section is for building the candidate repository.

- Run `powershell -File tools/sdk/verify-publication.ps1 -RepositoryPath build/sdk-candidate/maven` from the repository root, choosing an unused repository path for each candidate to avoid retaining older metadata or the retired `sdk-testing` artifact. The script stages the complete eight-artifact closure, verifies the headless and optional UI groups separately, and passes the resolved repository through `-PsdkRepository` to both publishing and the standalone consumers under `samples/sdk-consumer`. The optional parameter defaults to `build/sdk-repository`; no remote registry is configured. Use `-SkipPublish` only for that exact already-staged candidate and retain the same `-RepositoryPath`.
- Supply `-NodeExecutable <path>` for Node 22+ and `-AndroidSdkRoot <path>` when needed. The script passes `-PstreamcoreNodeExecutable=<path>` to producer and consumer builds. Set `CHROME_BIN` for browser suites; `-AndroidSerial <device>` executes the UI-resource instrumentation test, otherwise its APK is built without claiming device execution.
- The consumer build has independent settings/plugins and uses Maven coordinates only: no SDK project dependency, `includeBuild`, source import or substitution. Copy it with `-PsdkRepository=<absolute staged repository path>` and ordinary Android SDK configuration. Its Android and Kotlin/Wasm journeys cover reference browsing/state/playback and protected-profile activation; UI-only consumers read packaged resource payloads.
- After building its APK, `node tools/sdk/run-android-consumer.mjs --serial <device>` runs the ClientB device journey. `STREAMCORE_ADB` selects ADB. Optional `--live-tmdb` transfers the existing ignored review configuration/credentials only to an explicitly authorized device's app-private one-use input file and removes the file. It tests live identity discovery/logout; a simulated journey does not establish live acceptance. Interrupted processes/network failures can prevent session cleanup; `close()` is not logout.
- After building the production browser distribution, run the application regression suite with `node webApp/e2e/node_modules/@playwright/test/cli.js test --config webApp/e2e/playwright.sdk.config.ts`. It uses one worker and 1280-/1920-pixel Chromium viewports. Set `CHROME_BIN` for Kotlin browser tests. On memory-constrained hosts, the application test executable can be linked with `--max-workers=1 --no-parallel -Pkotlin.daemon.jvmargs=-Xmx4096m`; avoid simultaneous builds and emulator reviews.
- Review public declarations, defaults and behavior for intentional API changes, including consumer impact and migration requirements. Run `python tools/sdk/verify_sdk.py` for module/import boundaries; `--repository build/sdk-candidate/maven` also checks emitted metadata/resource packaging for the isolated candidate. Generated API/ABI snapshot gates are not part of this workflow.
- `verifySdkHeadlessDependencies` excludes Compose/Skiko/Koin/rendering from headless closure. `verifySdkUiDependencies` permits the resource closure but excludes application UI/DI, transport/storage and player implementations. Direct lifecycle dependencies remain prohibited in optional UI; transitive resource requirements are allowed.

The pinned candidate verification configuration is Kotlin 2.3.21, AGP 9.1.1 and, for optional UI, Compose Multiplatform 1.12.0. These pins are tested configurations when recorded in [verification](verification.md), not proven minimum compatible tool versions. Android libraries declare minimum API 24; the repository and samples compile against API 37 and keep application target API 36. Headless Wasm tests require Node 22+. No minimum browser-version matrix has been established by those pins.

Acceptance requires public contract review, boundary and publication checks, executable consumers, both Android flavors, TMDB browser and applicable live-account checks, browser/Android persistence, and representative mobile/tablet/TV/web UI review. Node tests alone cannot establish browser `localStorage` restart behavior. Use [the provider template](provider-template.md) for new integrations.
