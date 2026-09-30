# Maintaining the StreamCore SDK

This guide explains the current `0.1.0-alpha02` SDK from source to published artifact. Its target architecture is backend-agnostic: applications consume common operations and models; providers translate a particular backend into those contracts. The delivered targets are Android and Kotlin/Wasm. There is no Swift framework or JavaScript/TypeScript package. ClientB is simulated, and both reference providers' optional playback resolves demonstration media.

Use the [quickstart](quickstart.md) for a working consumer, [integration reference](integration.md) for contractual behavior, and [verification record](verification.md) for checks actually executed. This companion explains why the implementation is divided as it is and where to work when behavior changes.

## The nine modules

These are nine independently published modules, including the two nested provider UI modules. A folder beneath a provider is not automatically part of its headless artifact.

| Module | What it owns; concrete example |
| --- | --- |
| [`:sdk:model`](../../sdk/model/build.gradle.kts) | Provider-neutral values: `StreamCoreContent`, profile inputs/outcomes, configuration, capabilities, `StreamCoreResult` and typed errors. Some content/progress values are serializable because they appear in saved snapshots. |
| [`:sdk:api`](../../sdk/api/build.gradle.kts) | The consumer facade and service contracts: `StreamCoreClient`, `ProfileService`, `SearchService`, plus pure validators. It says what callers can do without exposing transport or storage. |
| [`:sdk:runtime`](../../sdk/runtime/build.gradle.kts) | Shared enforcement and implementation: authentication coordination, profile authorization, stale-work rejection, saved-state partitioning, search history and playback-progress policy. Its separate integration contracts serve provider factories. |
| [`:sdk:testing`](../../sdk/testing/build.gradle.kts) | Reusable consumer/provider-test code such as `ProviderContract` and `InMemorySearchService`. Consumers add it only to test dependencies. |
| [`:sdk:providers:tmdb`](../../sdk/providers/tmdb/build.gradle.kts) | TMDB protocol, DTOs, mapping, authentication, catalogue policy, local reference profiles, connection configuration and platform factories. |
| [`:sdk:providers:clientB`](../../sdk/providers/clientB/build.gradle.kts) | A second, simulated backend showing the same contracts work without TMDB assumptions. Includes opt-in single/protected-profile scenarios; its default remains unchanged. |
| [`:sdk:ui`](../../sdk/ui/build.gradle.kts) | Optional resource-backed presentation contracts: avatar resolution, `ErrorUiModel`, default error mapping and shared strings. It does not render screens. |
| [`:sdk:providers:tmdb:ui`](../../sdk/providers/tmdb/ui/build.gradle.kts) | TMDB avatar artwork and provider-specific error wording, using the shared presentation contracts. |
| [`:sdk:providers:clientB:ui`](../../sdk/providers/clientB/ui/build.gradle.kts) | ClientB avatar artwork and presentation adapters, independently consumable from its headless provider. |

The public package root is `com.pampoukidis.streamcore.sdk`. Packages describe public API organization; Gradle paths describe modules; Maven coordinates describe distribution. The publishing convention maps these deliberately. Do not derive a rename in one system automatically from a change in another. Some internal provider files retain older `streamcoretv.client` packages; that does not make their DTOs or adapters public consumer contracts.

## Reading a module directory

`src/` contains maintained Kotlin/resources/tests. `build.gradle.kts` declares the module's conventions, dependencies, target options and publication participation. `build/` contains generated classes, resources, archives, reports and metadata; editing it does not fix the source that regenerates it.

There are two different uses of “API” worth separating:

- `:sdk:api` is a real Gradle module containing executable Kotlin declarations under `sdk/api/src/`.
- Gradle’s `api(...)` dependency declaration exports a dependency to consumers. It is a dependency scope, not a source directory.

The repository does not maintain generated API/ABI snapshots. Review public declarations, defaults and behavior deliberately when changing contracts, and validate those changes through direct tests and independent consumers.

Every top-level type in `:sdk:model` uses `StreamCore` plus its domain name, without a trailing `Model` suffix. This includes inputs, events, enums, results and errors; nested members keep their existing names. Provider DTOs retain `Dto`, and application models, API services and optional UI contracts follow their own naming rules. The source-boundary verifier checks SDK model naming without generating an API snapshot.

Portable code belongs in `commonMain`; Android storage/transport factories belong in `androidMain`, browser implementations in `wasmJsMain`. `commonTest` supplies shared tests, `androidHostTest` adds JVM-executed platform tests, and `wasmJsTest` can exercise browser-specific behavior. Device tests are distinct from host tests.

The [KMP convention](../../build-logic/src/main/kotlin/StreamCoreKmpLibraryPlugin.kt) registers the official Android-KMP target, with compile SDK 37, minimum SDK 24 and JVM target 11. Modules opt into host tests and Wasm. Android-KMP libraries are single-variant; application flavors do not create provider-library flavors. Resource libraries use the [resource convention](../../build-logic/src/main/kotlin/StreamCoreResourcesKmpLibraryPlugin.kt), avoiding application preview-tooling dependencies. Do not assume every common test executes on every target: inspect each module's enabled compilations. The convention currently removes Compose-enabled library Wasm test compilations; browser resource checks run through the independent `uiWasm` consumer instead.

## Dependencies and runtime calls point in different directions

The principal compile-dependency chain is:

`provider → runtime → api → model`

Providers also directly export `api`; optional presentation follows `provider UI → sdk UI → model`. Runtime does not depend on a concrete provider. Otherwise adding a provider would require changing the shared runtime and would undermine substitution.

At execution time, the application calls the facade, the runtime enforces rules, and the runtime calls an injected provider adapter. For example:

`client.home.getCollections(profileId) → runtime checks → HomeProvider.getCollections → TMDB repository/network → mapped models → runtime filtering → result`

That reverse invocation is dependency inversion, not a circular Gradle dependency. [`HomeProvider`](../../sdk/runtime/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/runtime/integration/provider/HomeProvider.kt) is an interface owned by runtime; the factory supplies its implementation. Consumer `HomeService` and provider `HomeProvider` serve the same domain, with runtime enforcement between them. Provider repositories implement these narrow ports directly, without a forwarding catalogue wrapper.

| Consumer service / operation | Backend port | SDK-local persistence |
| --- | --- | --- |
| `AuthService.login / loginWithQr / logout / recoverPassword` | `AuthProvider`, with matching operation names | Authentication persistence remains provider-owned |
| `ProfileService` profile management/selection | `ProfileProvider` | Provider selection persistence; runtime owns entry/PIN authorization |
| `HomeService.getCollections` | `HomeProvider.getCollections` | No home store |
| `DetailsService.getDetails / getRecommendations` | `DetailsProvider`, with matching operation names | No details store |
| `SearchService.search / loadTrending` and history operations | `SearchProvider` for catalogue queries | `SearchHistoryStore` for SDK-owned history |
| `PlaybackService.resolveSource` and progress operations | `PlaybackProvider.resolveSource` for media | `PlaybackProgressStore` for SDK-owned progress |
| `LibraryService` | No library provider | `LibraryStore`; combined collections also read saved progress |

`ContentPolicyProvider.isContentAllowed` supplies backend classification/denial rules across these domains; it does not need a consumer service or store. Runtime applies it alongside existing authentication, profile and stale-work checks. Service/provider/store names describe responsibilities, not a requirement to create three layers for every domain.

Dependency scope also affects distribution. TMDB declares `api(projects.sdk.api)` but `implementation(projects.sdk.runtime)`. External applications therefore compile against exported facade/model types without needing runtime integration imports. **Implementation does not mean omitted from publication.** The provider needs runtime at execution, so runtime is published separately and included in the resolved runtime dependency closure. The staged Android POM lists `sdk-api-android` with compile scope and `sdk-runtime-android` with runtime scope; Gradle metadata supplies the richer platform variants. Runtime itself exports types needed by its provider SPI, including DataStore contracts. Those are deliberate provider-integration surfaces, not feature dependencies.

Applications normally declare a provider coordinate; they do not manually reconstruct its transitive dependency graph. The independent sample proves this by compiling with Maven coordinates and no hidden SDK project dependencies.

## Follow one client through its lifetime

Start at [`TmdbSdk.createAndroid`](../../sdk/providers/tmdb/src/androidMain/kotlin/com/pampoukidis/streamcore/sdk/providers/tmdb/TmdbSdk.android.kt). It creates owned platform storage and transport, then calls the shared [TMDB assembly function](../../sdk/providers/tmdb/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/providers/tmdb/TmdbSdk.kt). Construction performs no network requests. It returns `StreamCoreClient`, although the concrete object is `RuntimeStreamCoreClient`.

The factory injects an `AuthProvider`, `ProviderSessionFactory`, local stores and a resource-close callback. After identity is known, the session factory creates account-bound `ProviderSessionServices` holding `profiles`, `home`, `details`, `search`, `playback` and `contentPolicy`; construction must not perform network requests. This is plain constructor composition. Koin registration belongs to the consuming application. The [Android service bindings](../../app/src/main/java/com/pampoukidis/streamcoretv/di/SdkServicesModule.kt) register interfaces from the single flavor-selected client; features do not choose providers.

[`RuntimeStreamCoreClient`](../../sdk/runtime/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/runtime/RuntimeStreamCoreClient.kt) implements the typed service properties. One atomic runtime state holds session, entry attempt, challenge, verification and activation. [`ContextStateFlow`](../../sdk/runtime/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/runtime/ContextStateFlow.kt) projects that state synchronously into the consumer context; account identity is withheld while required installation persistence is incomplete.

`bootstrap()` restores an account, not profile authorization. If there is no restored account, `auth.login` discovers one. TMDB's optional `expectedAccountId` constrains the discovered identity; it is not an identity source. Its connection configuration only contains URL/token settings.

Next, `profiles.beginEntry()` starts a fresh decision:

1. No profiles produces `StreamCoreProfileEntryNoProfiles`.
2. One unprotected profile produces `StreamCoreProfileEntryReady` and is already active.
3. One protected profile produces `StreamCoreProfileEntryPinRequired`.
4. Multiple profiles produce `StreamCoreProfileEntryChooseProfile`; the user chooses before `selectProfile`.

The [profile contract](../../sdk/api/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/api/ProfileService.kt) makes PIN verification an explicit continuation. Providers declare numeric input metadata and verify the PIN; runtime binds the opaque challenge to this instance/account/entry attempt. `confirmPin` publishes authorization only after successful verification and required selection persistence. Wrong PIN is `PinRejected`, not account-session expiry. ClientB's protected fixtures use `1234`; TMDB retains unprotected adult/kids reference profiles.

Content and saved-state calls still take profile IDs, but those IDs must match the active authorized profile. Listing/editing/deleting profiles is account-scoped management. Internal session/activation identity checks stop obsolete work from being accepted into a newer context. `profileActivationId` lets application state follow the same boundary, including re-entry into the same profile. It is an in-memory scope identity, not a backend token. Observers and progress recorders capture an activation: create new ones after activation changes; recollecting an old flow does not grant it new access.

`clearSelection()` revokes access before legacy-selection cleanup, even if cleanup fails. Successful logout clears credentials/context while retaining account-owned saved data. Transient logout failure can retain the session for retry. Cancellation propagates; provider-committed state still matters. `close()` cancels owned SDK work and releases resources without remote logout or data erasure. The host must cancel its own Flow collectors separately.

Error classification is part of the lifecycle contract. Shared session invalidation responds to `StreamCoreError.SessionExpired`; an authentication attempt rejection, unauthorized content, unsupported operation or transient failure must not automatically become a logout. Preserve that distinction when mapping backend errors.

## Persistence is an SDK concern

[`SdkLocalRepositories`](../../sdk/runtime/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/runtime/storage/SdkLocalRepositories.kt) groups stores and context/migration callbacks. `SdkPlatformStorage`, `PreferencesSdkStorage` and the internal `ReportingPreferencesStore` share the `runtime.storage` package. Small `LibraryStore`, `SearchHistoryStore` and `PlaybackProgressStore` contracts sit beside their internal preference implementations in `storage.library`, `storage.search` and `storage.playback`. [`AccountStorageKey`](../../sdk/runtime/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/runtime/AccountStorageKey.kt) length-frames backend/environment, storage namespace, account ID and profile ID, avoiding collisions from concatenated delimiters. Identical profile IDs in two accounts do not share library/history/progress.

Platform factories reserve storage ownership; independently owned clients need distinct persistent namespaces. [`PreferencesSdkStorage`](../../sdk/runtime/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/runtime/storage/PreferencesSdkStorage.kt) also preserves legacy reading/migration through one internal `migrateLegacyPreferences` helper in `runtime.storage.migration`. Trusted persisted identity or an explicit administrator mapping establishes ownership; the next login cannot claim unattributed data. Unowned bytes are quarantined, and each store commits its migration marker with transformed state. This is per-store atomicity, not one transaction spanning every store; interrupted stores can resume independently.

Provider contracts stay together in `runtime.integration.provider`: auth, profile, home, details, search, playback, content policy, session factory/services and provider-operation failures. Persistence does not need another integration or repository-contract layer. This organization groups code by responsibility without adding modules or changing which callers can use the consumer API. The source-boundary verifier rejects runtime imports from feature and Android/browser application production code.

A stored selected-profile ID is compatibility metadata, never an unlock grant. Kotlin package changes likewise do not justify clearing storage. Preserve field names/defaults, keys, IDs and migration markers when changing serialized models; test old payloads alongside new behavior.

## Search and playback divide policy from interaction

[SearchService](../../sdk/api/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/api/SearchService.kt) at `client.search` combines query, interaction and history management operations. Runtime normalizes input and records successful nonempty submitted/recent-selected searches. Typing alone does not enter history. `displayedResults` handles reuse of loaded results; `resultSelected` records opening one. Use `observeHistory`, `recordHistory`, `removeHistoryQuery` and `clearHistory` for explicit history management. A history-storage failure does not discard successful search results. UI still owns debounce, request presentation, focus and navigation.

[PlaybackService](../../sdk/api/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/api/PlaybackService.kt) at `client.playback` groups source resolution, saved progress and recorder creation. It replaces separate source/progress consumer interfaces. The internal runtime can still use a narrow captured-progress helper; that implementation split does not require separate services for consumers.

[RuntimePlaybackProgressRecorder](../../sdk/runtime/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/runtime/RuntimePlaybackProgressRecorder.kt), returned by `playback.createProgressRecorder`, consumes `Periodic`, `Checkpoint` and `Completed` events through `reportEvent`. It owns ten-second buckets and timestamps; resume policy requires at least 30 seconds and less than 95% completion with known positive duration. Unknown duration preserves previous progress in the recorder; completion removes it. Direct `playback.updateProgress(entry)` applies a caller-timestamped snapshot without cadence and removes non-resumable entries, including unknown duration. The [event guide](playback-progress.md) explains save/remove/skip outcomes. The SDK returns a typed checkpoint failure; the current application ignores that result and continues player exit rather than surfacing the failure. Engines, tracks, fullscreen and rendering remain application playback concerns.

[RuntimeLibraryService](../../sdk/runtime/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/runtime/RuntimeLibraryService.kt) combines saved membership with progress. Do not mistake this for a unified home feed: `home.getCollections` retrieves/filters provider collections, while the current application Home ViewModel separately observes progress and builds its continue-watching presentation.

To trace a library mutation, start at the facade's captured activation, then follow `libraryForActivation` into `RuntimeLibraryService` and its wrapped `LibraryStore`. The service takes the membership timestamp from its injectable clock before the wrapper applies validation, profile authorization and content policy, then persists under the captured account/profile key. The adapter keeps those responsibilities explicit without moving the timestamp or refreshing the capture. Collection construction preserves progress order, sorts the two membership rows independently and gives library failures precedence over progress failures.

For a playback checkpoint, the recorder uses activation-bound `PlaybackProgressOperations` to apply the timestamped snapshot's resume policy. `lastAttemptedPeriodicBucket` tracks attempted periodic writes: it advances before persistence even when that attempt fails. Explicit checkpoints bypass that gate without changing the bucket; unknown-duration samples return before it, while completion removes progress first. The recorder's mutex keeps this sequence serialized.

## Optional UI stops before rendering

[`ErrorUiModel`](../../sdk/ui/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/ui/error/ErrorUiModel.kt) carries Compose string resources; avatar resolvers return drawable resources. Provider UI specializes artwork and wording, while SDK UI supplies shared defaults. Resource assets live in `commonMain/composeResources` and Android resource processing packages them for consumers.

These artifacts intentionally bring the Compose resource dependency closure. Headless artifacts do not. Neither optional presentation nor headless SDK owns screens, ViewModels, navigation, D-pad behavior or Koin. A provider-specific authentication message belongs in provider UI; a TV focus ring belongs in application design-system/UI code. The same distinction keeps provider DTOs out of shared models.

## Where a change belongs

| Desired change | Starting point and companion work |
| --- | --- |
| New shared operation or typed failure | Model/API contract, runtime enforcement, provider responsibility and direct contract tests. |
| TMDB endpoint or payload mapping | Internal TMDB adapter/DTO/mapper; retain provider-neutral output. |
| Profile/PIN access rule | Runtime plus provider verification contract; test direct service calls without a ViewModel. |
| Search recording or resume policy | Runtime event/store behavior and shared tests; keep UI timing separate. |
| Saved-state schema | Runtime serializers/migration and restart fixtures; preserve ownership and legacy bytes. |
| Avatar or localized provider wording | Corresponding optional UI module and resource-only consumer checks. |
| Chooser layout, focus, animation or player controls | Application feature/design-system/playback modules. |
| New backend | Follow the [provider template](provider-template.md); implement SPI and factory rather than forking runtime. |

## Tests, publication and compatibility

Reusable fixtures belong in `sdk/testing/src/commonMain` because they are the artifact's published library code. They become test-only dependencies in other modules' `commonTest`. Its [ProviderContract](../../sdk/testing/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/testing/ProviderContract.kt) exercises a wrapped public client; its fake history service helps consumer tests. `commonMain` here does not make it an application production dependency. API/testing deliberately do not add filler common tests or host-test compilations.

Use focused runtime/provider host tests for rules; Node for headless Wasm; browser tests for actual browser storage; device/resource tests where packaging or input behavior matters. A Node case cannot prove `localStorage` restart. Ordinary source changes do not require starting devices. [Runtime tests](../../sdk/runtime/src/commonTest/kotlin/com/pampoukidis/streamcore/sdk/runtime/RuntimeStreamCoreClientTest.kt) and the [published consumer](../../samples/sdk-consumer) cover different risks: behavioral enforcement versus packaging/integration.

The [publishing convention](../../build-logic/src/main/kotlin/StreamCoreSdkPublishingPlugin.kt) fixes current coordinates/version and registers dependency checks. [`verify_sdk.py`](../../tools/sdk/verify_sdk.py) checks module/import boundaries and staged metadata. Intentional public contract changes require reviewing the maintained declarations, their consumer impact and behavioral tests; document migration requirements when callers must change.

The [publication script](../../tools/sdk/verify-publication.ps1) stages all nine artifacts and tests standalone Maven consumers, including optional UI resources. For this candidate, use `-RepositoryPath build/sdk-provider-domains/maven`; the resolved path reaches publisher and consumers through `-PsdkRepository`. Source jars and the runtime dependency closure accompany the artifacts. Local staging is not remote registry release, and alpha02 deliberately breaks alpha01 Kotlin APIs; read the [migration guide](alpha02-migration.md). Passing an app build alone does not replace these gates.

## A productive reading order

Read the [client facade](../../sdk/api/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/api/StreamCoreClient.kt), then [ProfileService](../../sdk/api/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/api/ProfileService.kt) and [context model](../../sdk/model/src/commonMain/kotlin/com/pampoukidis/streamcore/sdk/model/StreamCoreContext.kt). Follow the TMDB factory into runtime, trace one `getCollections` call through `HomeProvider`, then inspect storage partitioning. Finish with the runtime contract tests and published sample. That order connects each abstraction to a caller, an enforcing implementation, and evidence that the boundary works.
