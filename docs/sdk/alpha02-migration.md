# Migrating to 0.1.0-alpha02

This candidate intentionally changes the Kotlin API. Upgrade all StreamCore artifacts together and
recompile consumers. Supported production artifact names remain unchanged; the testing artifact is retired
as described below. The architecture remains backend-agnostic.

## Development storage compatibility removed — 2026-10-02

There are no shipped-data compatibility requirements. Remove `StreamCoreConfiguration.legacyAccountId`,
provider configuration `legacyApplicationStorage`, platform `legacyNames`, `AuthProvider.legacyAccountId()`
and `SdkLocalRepositories.migrateLegacy` from integrations. The internal conversion helper and its
backup, quarantine and migration-owner markers are removed. No replacement owner mapping is required.

Providers and applications now use the same existing namespaced storage defaults. Old development files
and browser keys are left untouched and are not imported or deleted. Current saved-state formats,
backend/namespace/account/profile isolation, provider authentication persistence, selected-profile cleanup
and saved-data retention across logout remain in place.

Runtime keeps the `SdkLocalRepositories.checkContextStorage` availability callback. Before restoration,
login or logout, `RuntimeAuthService.ensureContextStorageAvailable()` writes through the auth/context
store and caches success. The existing `sdk_context_schema = "2"` value identifies the current context
format and probes storage; it is not a migration completion marker. Browser startup can still switch to
session storage when persistent storage is unavailable. Saved-data decoding/error handling happens in
ordinary library/history/progress operations, without startup conversion.

This section supersedes the legacy-data compatibility assumptions in earlier development checkpoints
below and in the [historical ownership baseline](ownership-and-baseline.md). Historical verification
records continue to describe the exact code and checks from their own runs.

## Shared provider tests are no longer published — 2026-10-02

The `com.pampoukidis.streamcore:sdk-testing` publication is retired. `:sdk:testing` remains registered as a
support project applying only Gradle's `base` plugin, with no Kotlin/Android plugin, compilation or standalone test execution.
`ProviderContract` and `ContractResult` now live in `sdk/testing/src/commonTest/kotlin`, included directly
by each provider's `commonTest` source set. Existing provider tests still run the same shared journeys;
production SDK behavior and data formats are unchanged. The unused `InMemorySearchService` is removed.

Remove the old test-artifact dependency from external builds and maintain consumer-specific test fixtures
locally. In-repository providers should use the source-set declaration in the [provider template](provider-template.md#required-direct-contracts-and-application-integration).
Sync Gradle so the IDE recognizes the helpers as test sources, then exclude tests from usage searches
when tracing production code. An all-usages search can still show them. Stage this candidate into a fresh
Maven directory: previous staging repositories may still contain the retired artifact.

## Imports and services

Public Kotlin declarations now use `com.pampoukidis.streamcore.sdk`. Models use domain packages under
`model`; services are in `api`; provider factories are in `providers.tmdb` and `providers.clientb`.
Optional presentation contracts and resources use `ui` and `providers.<provider>.ui`.
There are no forwarding aliases for the previous application-shaped packages.

Use `client.auth.restoreSession()` to restore a session and `client.context` to observe validated account/profile
state. Replace the authentication repository with `AuthService`, and use `restoreSession`, `login`, `loginWithQr`,
`logout` and `recoverPassword`. The client has no restoration method or separate consumer auth stream.

Replace `savedLibrary` with `library`, source resolution with `playback.resolveSource`, and `getHomeRows`
with `getCollections`. `SearchService` combines query, interaction and history operations; there are no
separate `searchInteractions` or `searchHistory` client services. Pure form validation remains available
through `com.pampoukidis.streamcore.sdk.validation` (`LoginValidator`, `ProfileValidator` and `SearchQueryNormalizer`). Update earlier `sdk.api.validation` imports to this canonical package.

## SDK model prefix — 2026-09-29

Every top-level type in `:sdk:model` now starts with `StreamCore`; a former trailing `Model` suffix is removed. Existing `StreamCoreConfiguration`, `StreamCoreCapabilities` and `StreamCoreContext` names stay unchanged. Update imports and type references, upgrade the full artifact set together, and recompile. No compatibility aliases or generated API/ABI snapshots are introduced.

| Previous name | Current name |
| --- | --- |
| `ContentModel`, `ProfileModel` | `StreamCoreContent`, `StreamCoreProfile` |
| `AppResult`, `AppError`, `ErrorSource` | `StreamCoreResult`, `StreamCoreError`, `StreamCoreErrorSource` |
| `Cast`, `Genre` | `StreamCoreCastMember`, `StreamCoreGenre` |
| `LoginCredentials`, `DetailsRequest` | `StreamCoreLoginCredentials`, `StreamCoreDetailsRequest` |
| `ProfileEntryReady`, `ProfileEntryPinRequired` | `StreamCoreProfileEntryReady`, `StreamCoreProfileEntryPinRequired` |
| `PlaybackProgressEntryModel`, `PlaybackProgressEvent` | `StreamCorePlaybackProgressEntry`, `StreamCorePlaybackProgressEvent` |
| `SearchInteraction`, `PersistenceMode` | `StreamCoreSearchInteraction`, `StreamCorePersistenceMode` |

The same rule covers all other SDK model enums, inputs, events, validation types and sealed outcomes. Nested members retain their names: `AppResult.Success` becomes `StreamCoreResult.Success`, and `AppError.Validation` becomes `StreamCoreError.Validation`. Package organization, API service names, provider factories/configuration and optional UI contracts are unchanged by this rename.

This is a Kotlin source/ABI change within the alpha02 candidate. It does not change serialized field names, storage formats, account/profile IDs, resource IDs or behavior; keep existing data. The model-prefix verification used `build/sdk-model-prefix/maven`; follow the current [maintainer publication workflow](integration.md#maintainer-publication-and-verification) for subsequent candidates. Earlier alpha02 verification records describe their original artifacts, not this renamed set.

## Playback and search domain grouping — 2026-09-29

The current alpha02 development surface has one `PlaybackService` at `client.playback` and one
`SearchService` at `client.search`. Consumers of the earlier alpha02 split services must migrate:

| Previous member/method | Current member/method |
| --- | --- |
| `playbackSources.resolve` | `playback.resolveSource` |
| `playbackProgress.get / observe / remove` | `playback.getProgress / observeProgress / removeProgress` |
| `playbackProgress.record(entry)` | `playback.updateProgress(entry)` |
| `playbackProgress.openSession` | `playback.createProgressRecorder` |
| `PlaybackProgressRecorder.record` | `PlaybackProgressRecorder.reportEvent` |
| `searchHistory.observe / record / remove / clear` | `search.observeHistory / recordHistory / removeHistoryQuery / clearHistory` |

Replace injected `PlaybackSourceService` and `PlaybackProgressService` with one `PlaybackService`.
Replace injected `SearchHistoryService` with the same `SearchService` used for queries. The former three
public interfaces are removed without forwarding aliases. This step originally renamed the reusable
history fixture to `InMemorySearchService`; that unused fixture has since been removed with the testing artifact.

`reportEvent` processes player events, supplies timestamps and applies cadence; `updateProgress` applies
a complete caller-timestamped snapshot immediately. Both can remove an entry under the resume policy.
Read [the playback event guide](playback-progress.md) before choosing a path; do not call both for one event.
Persisted field names, profile/account partitioning, timestamps, thresholds and cancellation behavior remain unchanged.

Provider ports belong to `runtime.integration.provider` and persistence contracts to `runtime.storage`;
applications must use consumer services. Player engines, navigation and dependency-injection registration remain application-owned.

## Runtime packages by responsibility — 2026-09-29

Provider authors must update runtime imports; application consumer services and SDK model names are unchanged by this reorganization. All packages below are beneath `com.pampoukidis.streamcore.sdk.runtime`.

| Declarations formerly in `integration` | Current package |
| --- | --- |
| `AuthenticationProvider`, `CatalogueProvider`, `ProfilesProvider`, `ProviderSessionFactory`, `ProviderSessionServices`, `ProviderOperationException` | `integration.provider` |
| `SdkPlatformStorage`, its Android/Web factory functions, `SdkLocalRepositories`, `PreferencesSdkStorage` | `storage` |
| `LibraryStore` | `storage.library` |
| `SearchHistoryStore` | `storage.search` |
| `PlaybackProgressStore` | `storage.playback` |

`ReportingPreferencesStore` remains internal in `storage`. Small store contracts are colocated with their implementations; no module or generic repository layer is introduced. The migration helper present at this checkpoint has since been removed; see the development-storage section above. Application/feature production code must not import provider or storage internals.

Recompile provider integrations against the reorganized runtime. Maven coordinates, consumer operations, storage keys/formats and ownership behavior remain unchanged. This package-relocation step used `build/sdk-runtime-packages/maven`; follow the current [maintainer publication workflow](integration.md#maintainer-publication-and-verification) for subsequent candidates. The declaration names in the relocation table above precede the domain alignment below.

## Provider contracts aligned with services — 2026-09-29

Provider integrations now use narrow domain contracts in `runtime.integration.provider`. Consumer services, SDK-local storage packages and runtime enforcement are unchanged.

| Previous SPI | Current SPI |
| --- | --- |
| `AuthenticationProvider` | `AuthProvider` |
| `loginUser / loginUserWithQR / logoutUser / forgotPassword` | `login / loginWithQr / logout / recoverPassword`; other auth SPI operations retain their names |
| `ProfilesProvider` | `ProfileProvider` |
| `CatalogueProvider.getHomeRows` | `HomeProvider.getCollections` |
| `CatalogueProvider.getDetails / getRecommendations` | `DetailsProvider.getDetails / getRecommendations` |
| `CatalogueProvider.search / loadTrending` | `SearchProvider.search / loadTrending` |
| `CatalogueProvider.resolve` | `PlaybackProvider.resolveSource` |
| `CatalogueProvider.isContentAllowed` | `ContentPolicyProvider.isContentAllowed` |

`CatalogueProvider` is removed. Provider repositories implement the replacement ports directly. Replace the forwarding catalogue wrapper with account-bound `ProviderSessionServices(profiles, home, details, search, playback, contentPolicy)`; `ProviderSessionFactory` retains its network-free construction role. `PlaybackProvider.resolveSource` still returns `StreamCorePlaybackMedia` and retains the existing throwing failure contract. Content policy still applies through runtime to direct requests and saved content. Library/history/progress remain SDK-local, without new backend providers.

The provider-domain candidate used `build/sdk-provider-domains/maven` for publication/consumer checks. This alignment introduced no modules, snapshots or behavior changes; follow the current maintainer workflow for later candidates.

## SDK implementation organization — 2026-09-30

The consumer `sdk-api` and `sdk-model` contracts are unchanged. Provider implementation source now uses only `com.pampoukidis.streamcore.sdk.providers.tmdb` / `.clientb`, grouped by domain, network and shared catalogue responsibility. Public factory/configuration packages and optional UI separation are unchanged. Implementation classes remain internal; callers must not import the former application-shaped provider packages.

`RuntimeStreamCoreClient` now composes concrete internal auth, profile, home, details, search, library and playback services. One internal `runtime.session.RuntimeSession` owns atomic state, lifecycle, authorization and SDK-owned work, with `ContextStateFlow` beside it. Domain workflows use shared guards directly; the client is no longer their implementation hub. Playback helpers live in `runtime.playback` and retain captured activation; library operations reach the real store without a guarded-store round trip through the client.

Provider/runtime assembly code must stop passing the unused `SdkLocalRepositories.loadSelectedProfile` callback. Selected-profile writes/cleanup remain; saved selection still cannot authorize a fresh client. `AccountStorageKey` moves into `runtime.storage`, and private serialization types used by one repository are colocated in that file. This checkpoint preserved persistent keys, payload fields/defaults, call/error/cancellation behavior and resource ownership. Its legacy-conversion path has since been removed as described above.

The implementation-organization candidate used `build/sdk-organization/maven` for publication and independent consumers. No modules or generated API/ABI snapshots were added; previous verification entries describe their own artifacts. See the [maintainer guide](maintainer-guide.md#where-a-change-belongs) for current implementation entry points.

## Provider contracts beside runtime features — 2026-09-30

Provider authors must update imports from `com.pampoukidis.streamcore.sdk.runtime.integration.provider`. The consumer `sdk-api` and `sdk-model` surface is unchanged. All replacement packages below are beneath `com.pampoukidis.streamcore.sdk.runtime`.

| Declaration formerly in `integration.provider` | Current package |
| --- | --- |
| `AuthProvider` | `auth` |
| `ProfileProvider` | `profile` |
| `HomeProvider` | `home` |
| `DetailsProvider` | `details` |
| `SearchProvider` | `search` |
| `PlaybackProvider` | `playback` |
| `ContentPolicyProvider` | `content` |
| `ProviderSessionFactory`, `ProviderSessionServices` | `session` |
| `ProviderOperationException` | `error` |

These deliberate public provider-integration declarations now sit beside the features that use them; runtime implementation types remain internal. The entire `integration/provider` folder is retired. Recompile provider integrations against the new packages. This move changes SPI imports only: signatures, runtime behavior, consumer services, storage packages/formats and ownership remain unchanged, with no modules or snapshots added.

The feature-contract candidate used `build/sdk-feature-contracts/maven` for publication and independent consumers. Earlier migration sections and verification records retain the names and artifacts used at those checkpoints.

## Consolidated playback implementation — 2026-09-30

Consumer API/model types and provider contracts are unchanged; applications and providers need no source migration for this consolidation. The internal playback package now contains `PlaybackProvider.kt` and `RuntimePlaybackService.kt` only. Progress logic is merged into the service, and its private inner recorder owns the request, captured `AuthorizedProfile`, mutex and cadence bucket.

The internal `PlaybackProgressOperations`, `RuntimePlaybackProgress` and separate `RuntimePlaybackProgressRecorder` are removed. `RuntimeLibraryService` receives the actual playback service and calls its internal `observeProgress(profileId, capturedAuthorization)` overload, using the same grant for both combined flows. The progress-factory callback is removed. Tests follow this real design instead of requiring extra production layers for partial reuse or isolation.

Operation order, authorization, cancellation, progress cadence/thresholds, persistence and resource ownership are preserved. Provider, platform and storage boundaries remain. This consolidation used `build/sdk-playback-consolidation/maven` for publication/consumer checks; follow the current maintainer workflow for subsequent candidates. Older verification entries retain their original artifact scope.

## Authentication restoration on AuthService — 2026-10-02

This is an intentional Kotlin source/ABI break within alpha02. Upgrade the full artifact set together and recompile consumers, provider implementations, fakes and tests. No forwarding client method or compatibility alias remains.

| Previous surface | Current surface |
| --- | --- |
| `StreamCoreClient.bootstrap()` / `sdk.bootstrap()` | `AuthService.restoreSession()` / `sdk.auth.restoreSession()` |
| `AuthProvider.bootstrapAuth()` | `AuthProvider.restoreSession()` |
| `StreamCoreContext.isBootstrapped` | `StreamCoreContext.isAuthInitialized` |
| `StreamCoreContextFailureReason.NotBootstrapped` | `StreamCoreContextFailureReason.AuthNotInitialized` |

The consumer return type remains `StreamCoreResult<StreamCoreContext>`. The provider SPI still returns `StreamCoreResult<StreamCoreAuthState>` and remains a backend integration contract. `RuntimeAuthService` implements restoration directly. `RuntimeStreamCoreClient` composes services and delegates close; it does not forward authentication restoration. The auth/context storage availability check stays internal to the authentication workflow; the legacy conversion performed at this checkpoint has since been removed.

Behavior is unchanged: construction performs no network work; restoration is explicit; direct login is valid without a preceding restore; and once authentication is initialized, restoration returns the current context without repeating backend restoration. A fresh restored account has no profile authorization and must go through profile entry. Failed installation, cancellation, transient failures, authorization checks and resource ownership retain their existing behavior.

At this restoration checkpoint, persisted data and storage behavior were unchanged by the source rename. The subsequent development-storage simplification above removes legacy conversion and alternate filenames while retaining current namespaced persistence and account/profile isolation. Established provider diagnostics, including literal `bootstrapAuth` operation metadata, are intentionally retained; this source rename does not imply that all historical or diagnostic uses of the old spelling disappear.

This restoration change was staged in `build/sdk-restore-session/maven`. SDK/application checks and the independent Android/Wasm consumers passed; [verification](verification.md#authentication-session-restoration--2026-10-02) records that run's evidence. Use the current [maintainer publication workflow](integration.md#maintainer-publication-and-verification) for subsequent candidates.

## Profile entry and authorization

After login or restoration into a fresh SDK instance, call `profiles.beginEntry()` and handle its typed
outcome. One unprotected profile activates directly; one protected profile requests its PIN; multiple
profiles require a choice. A stored profile ID does not constitute authorization or skip the chooser.

Use `selectProfile` for a choice, `confirmPin` for a challenge, and `cancelPin` when leaving the challenge.
Only a completed activation permits navigation to profile content. Do not select the profile a second
time in a navigation callback. `clearSelection` ends profile access before switching.

Profile IDs remain explicit on content and saved-state operations. They must match the current authorized
profile; arbitrary owned profile IDs no longer grant access. Manage-profile operations remain account-scoped
and retain provider permissions. PIN protection does not reinterpret content maturity ratings.

PIN authorization is held in memory for the current activation. Fresh instances and switching away and
back require verification again; background/resume of the same active SDK does not. Providers verify PINs
and determine any attempt restrictions. PIN creation, changing and recovery are not part of this candidate.

TMDB retains its local demonstration profiles. ClientB's protected-profile scenarios are explicitly
simulated reference behavior, not evidence of production backend security or integration.

## Configuration and lifecycle

Use `TmdbConnectionConfiguration` for the connection. Specify an optional expected-account restriction
only in `StreamCoreConfiguration.expectedAccountId`; authentication still discovers the actual identity.
Configuration stringification redacts the read-access token.

Construction makes no network requests. `auth.restoreSession()` is explicit and idempotent once `isAuthInitialized` is true, including after direct login. Login does not require prior restoration.
Cancellation propagates as coroutine cancellation. A transient logout failure retains the session for retry.
Successful logout clears credentials and active context while retaining account-owned saved data.
`close()` is idempotent and releases owned resources without logout or data erasure. Hosts own their Flow
collectors and must cancel their collection scopes; closing the SDK does not complete a StateFlow.

## Saved data and behavior

Use a stable backend/environment and storage namespace when reopening current SDK data. Providers and
applications use the existing namespaced defaults; there is no legacy-storage setting. Account/profile
partitioning, avatar IDs and current serialized fields remain unchanged. Old development files and browser
keys are left untouched and are not imported; no clearing step is required.

Search typing alone does not enter history. Explicit successful searches with results and opened search
results retain the established recording behavior. History failure does not turn successful search into
failure. Playback keeps its existing cadence, resume/completion thresholds and retention; failure to save
progress does not prevent leaving the player.

The supported distribution remains Android and Kotlin/Wasm. `createWeb` is a Kotlin/Wasm factory;
Swift and JavaScript/TypeScript packages are separate future milestones. This candidate is staged locally;
it does not establish remote registry publication or a stable 1.0 compatibility promise.
