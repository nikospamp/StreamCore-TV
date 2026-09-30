# Provider implementation template

A provider adapts a backend to StreamCore's backend-agnostic contracts. The public consumer API lives under `com.pampoukidis.streamcore.sdk.api`; backend adapters implement the independent SPI in `com.pampoukidis.streamcore.sdk.runtime.integration.provider`. Persistence assembly belongs to `com.pampoukidis.streamcore.sdk.runtime.storage`. Do not implement consumer services directly to bypass runtime enforcement.

## Modules and public surface

1. Add `:sdk:providers:<provider>` at `sdk/providers/<provider>` using `streamcore.kmp.library` and `streamcore.sdk.publishing`; apply serialization only when required. Add Android through the convention and Wasm only when transport/storage is ready. Export `api(projects.sdk.api)`; keep runtime, protocol and storage dependencies as implementation details. Register a unique artifact ID. Maven coordinates and Kotlin package names are deliberate surfaces, not inferred from directories.
2. Place public factories/configuration under `com.pampoukidis.streamcore.sdk.providers.<provider>`. Keep `createAndroid` / `createWeb`; factory construction must perform no network requests. Accept explicit backend/environment and storage namespace. Discover account identity during authentication; an optional common `expectedAccountId` restricts identity after discovery. Do not add a second account-ID source in connection configuration. Redact secrets from diagnostics/stringification.
3. Keep DTOs, mappers, transport/auth stores and adapters internal. Provide typed unsupported behavior and distinguish sample media from actual provider media. Return only the wrapped `StreamCoreClient` to consumers.
4. If shared artwork/wording is useful, add `:sdk:providers:<provider>:ui` with the resource-only convention and `api(projects.sdk.ui)`. Use `providers.<provider>.ui.*` and matching generated resource packages. Keep avatar IDs stable. This optional artifact must not depend on headless-provider networking, application `core:ui`, feature UI, Koin or player rendering.

Shared top-level types in `:sdk:model` use a `StreamCore` prefix without a trailing `Model` suffix, including requests, results, errors and enums. Map internal provider `Dto` values into those shared types. Consumer service names, provider factories and optional UI contracts retain their existing naming conventions.

## Authentication, profiles and content policy

Implement `AuthProvider`, `ProfileProvider`, `HomeProvider`, `DetailsProvider`, `SearchProvider`, `PlaybackProvider` and `ContentPolicyProvider`. These narrow backend contracts do not inherit consumer services. Provider repositories implement the ports directly; do not recreate a forwarding catalogue wrapper. `ProviderSessionFactory` supplies account-bound `ProviderSessionServices` with `profiles`, `home`, `details`, `search`, `playback` and `contentPolicy` implementations.

Align equivalent operations with consumer services: authentication uses `login`, `loginWithQr`, `logout` and `recoverPassword`; home uses `getCollections`; details uses `getDetails` / `getRecommendations`; search uses `search` / `loadTrending`. Other authentication SPI operations remain unchanged. `PlaybackProvider.resolveSource` returns `StreamCorePlaybackMedia` and retains its existing throwing error contract. Runtime translates and enforces these backend operations for consumers; search history, playback progress and library persistence remain SDK-local.

Authentication discovers a stable non-null opaque account ID. Session factories create account-bound adapters without network work. Preserve cancellation and transient failures. Only authoritative account-session rejection is `SessionExpired`; an incorrect password or profile PIN must not invalidate an otherwise valid account. Runtime coordinates bootstrap, context publication, stale-result rejection and resource close.

`ProfileProvider` supplies the profile catalogue, editor options, CRUD and backend selection. A provider with protected profiles declares `StreamCoreProfile.pinPolicy = StreamCoreProfilePinPolicy(digitCount)` and `StreamCoreCapabilities.profilePinVerification = true`, and implements `verifyProfilePin(profileId, pin): StreamCoreResult<Unit>`. Runtime owns `beginEntry`, one-profile auto-entry, challenges and active authorization; the provider owns the authoritative verification and any real attempt restrictions. Return `PinRejected(remainingAttempts = ...)` for a wrong PIN. Do not reuse `Authentication` or `SessionExpired` for it. Without verification capability, protected activation is unsupported, not silently unprotected.

Do not persist a PIN or application-facing authorization grant. PIN input stays in transient UI state; runtime challenges are bound to instance/account/entry attempt. Every fresh client requires entry, and switching away revokes profile access. Ordinary catalogue and local-state calls require the active authorized profile even though their IDs remain explicit. Account-scoped profile management keeps backend permissions. PIN creation/reset/recovery is not part of this contract.

TMDB keeps its existing adult/kids reference profiles unprotected. ClientB's `Standard`, `Single`, `SingleProtected` and `HouseholdProtected` scenarios are opt-in fixtures, with demo PIN `1234` for protected profiles; they are not a model of a production PIN backend.

Implement `ContentPolicyProvider.isContentAllowed` using the backend's real classification/denial rules. Do not derive universal maturity policy from numeric ratings. Runtime invokes the same policy for collections, direct details/source requests and saved content. Keep source-resolution failures typed; the SPI's `ProviderOperationException` can carry `StreamCoreError` through throwing backend operations. It is not consumer API.

## Storage and resource ownership

Construct platform-owned storage/transport in the factory, reserve the persistent namespace, and pass adapters plus `SdkLocalRepositories` to `RuntimeStreamCoreClient`. `SdkPlatformStorage`, its Android/Web factory functions, `SdkLocalRepositories` and `PreferencesSdkStorage` live in `runtime.storage`. `LibraryStore`, `SearchHistoryStore` and `PlaybackProgressStore` live beside their implementations in `runtime.storage.library`, `.search` and `.playback`. These are provider/runtime integration contracts, not application escape hatches. Keep storage DTOs/serializers and `ReportingPreferencesStore` internal. Owned resources close with the client; host-supplied resources retain explicit host ownership.

Keep shared legacy preference migration in the single internal `migrateLegacyPreferences` helper in `runtime.storage.migration`. Group code by responsibility and keep small contracts near their implementations; add a separate layer only when it solves a concrete problem.

Keep persistent backend/namespace/account/profile keys and existing file/JSON formats stable. Establish legacy ownership from trusted persisted identity or explicit administrator mapping before authentication changes it. Never give unattributed legacy bytes to the next login or infer ownership from `expectedAccountId`. Preserve unowned bytes in quarantine and commit transformed state/migration markers atomically. Successful logout removes credentials/active context but retains account-owned library/history/progress. Close releases resources without logout or erasure.

## Required direct contracts and application integration

Use `sdk-testing` only in test source sets. `ProviderContract` exercises the wrapped public client; add provider-specific tests for backend protocol and new capabilities. Required acceptance includes:

- Network-free construction; account discovery without expected-account restriction; bootstrap, logout failure/cancellation, idempotent close and resource ownership.
- Zero/one/multiple profile entry; wrong/right/cancelled PIN; unsupported verification; switching and fresh-instance re-entry; direct content/state calls rejected before activation or for a different profile ID.
- Account A → logout → B → A with overlapping profile IDs: no shared authorization/data, while A's saved state survives.
- Existing validation, search typing/submission/result-opening rules, library membership, progress cadence/resume/completion, provider maturity policy and explicit demo playback.
- Retired session/activation work cannot publish into a newer context; migration interruption and unowned/corrupt legacy fixtures preserve attributable data.

Applications register the client and public services at their composition root, optionally adding plain provider artwork/error constructors. Features depend on `sdk-api`, not `sdk-runtime` or provider adapters. Use `client.context` as the account/authorized-profile source; scope disposable profile content by `profileActivationId` so re-entering the same profile does not reuse stale content. Navigation owns the entry/PIN screens and uses a successful activation once. Keep editable drafts, focus, D-pad/pointer handling and engines outside SDK artifacts.

Stage the full artifact closure and exercise the [coordinate-only consumers](../../samples/sdk-consumer). Review intentional public contract changes and their consumer impact; run behavioral tests, dependency/import boundaries, metadata and resource packaging gates. Browser storage and Android persistence require their actual platform checks. Record executed evidence in [verification](verification.md), never infer production-backend acceptance from a simulated provider. See [integration](integration.md#maintainer-publication-and-verification) for maintainer commands.

For each new feature, define shared models/operations, runtime-enforced rules, provider capability/unsupported behavior, account/profile/persistence ownership, direct SDK tests and application effects. Add contracts for demonstrated domain needs. JS/TS and Swift distribution, DRM, subscriptions or live TV require separate concrete delivery scope.
