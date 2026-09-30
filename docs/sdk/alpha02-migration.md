# Migrating to 0.1.0-alpha02

This candidate intentionally changes the Kotlin API. Upgrade all StreamCore artifacts together and
recompile consumers. Maven artifact names remain unchanged. The architecture remains backend-agnostic.

## Imports and services

Public Kotlin declarations now use `com.pampoukidis.streamcore.sdk`. Models use domain packages under
`model`; services are in `api`; provider factories are in `providers.tmdb` and `providers.clientb`.
Optional presentation contracts and resources use `ui` and `providers.<provider>.ui`.
There are no forwarding aliases for the previous application-shaped packages.

Use `client.bootstrap()` to restore a session and `client.context` to observe validated account/profile
state. Replace the authentication repository with `AuthService`, and use `login`, `loginWithQr`,
`logout` and `recoverPassword`. Consumer services no longer expose a second bootstrap or auth stream.

Replace `savedLibrary` with `library`, source resolution with `playback.resolveSource`, and `getHomeRows`
with `getCollections`. `SearchService` combines query, interaction and history operations; there are no
separate `searchInteractions` or `searchHistory` client services. Pure form validation remains available
through the SDK validators.

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
public interfaces are removed without forwarding aliases. The reusable history fixture is now
`InMemorySearchService`; its unavailable catalogue operations explicitly return `Unsupported`.

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

`ReportingPreferencesStore` remains internal in `storage`. Legacy migration is shared through one internal `migrateLegacyPreferences` function in `storage.migration`. Small store contracts are colocated with their implementations; no module, generic repository layer or public migration framework is introduced. Application/feature production code must not import provider or storage internals.

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

Recompile provider integrations and use `build/sdk-provider-domains/maven` for fresh publication/consumer checks. This alignment introduces no modules, snapshots or behavior changes; previous verification records retain their own artifact scope.

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

Construction makes no network requests. `bootstrap()` is idempotent after successful restoration.
Cancellation propagates as coroutine cancellation. A transient logout failure retains the session for retry.
Successful logout clears credentials and active context while retaining account-owned saved data.
`close()` is idempotent and releases owned resources without logout or data erasure. Hosts own their Flow
collectors and must cancel their collection scopes; closing the SDK does not complete a StateFlow.

## Saved data and behavior

Keep the same backend/environment, storage namespace and legacy-storage setting during upgrades.
Application IDs, persisted filenames/keys, account/profile IDs, avatar IDs and serialized fields are
preserved. Kotlin package changes do not require clearing application or browser storage.

Search typing alone does not enter history. Explicit successful searches with results and opened search
results retain the established recording behavior. History failure does not turn successful search into
failure. Playback keeps its existing cadence, resume/completion thresholds and retention; failure to save
progress does not prevent leaving the player.

The supported distribution remains Android and Kotlin/Wasm. `createWeb` is a Kotlin/Wasm factory;
Swift and JavaScript/TypeScript packages are separate future milestones. This candidate is staged locally;
it does not establish remote registry publication or a stable 1.0 compatibility promise.
