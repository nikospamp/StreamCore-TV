# KMP-05 — Migrate TMDB and ClientB Provider Implementations

## Goal

Move eligible TMDB and ClientB provider code to KMP while preserving provider isolation, Android flavor behavior, and the Android Media3 engine.

## Context

Provider modules own DTOs, Ktor/API implementations, auth/session storage, mappers, repository implementations, capability behavior, UI artwork/error
mapping, and playback-source resolution. Only platform engines, Android configuration, and Android-specific playback factories should remain outside
common code.

## Dependencies and Parallelization

- **Depends on:** KMP-03 and KMP-04 merged.
- **Blocks:** KMP-06.
- **Parallelization:** One integration owner. Provider sub-work may be split between TMDB and ClientB only if the owner retains version catalog,
  shared runtime config, Ktor factory, and application flavor wiring.

## In-Scope Modules

- `:client:tmdb:data`
- `:client:clientB:data`
- `:client:tmdb:player`
- `:client:clientB:player`
- Android flavor configuration in `:app`.
- Media3 Koin registration required by the provider-neutral engine split.

Client UI/resource modules are deferred to KMP-06.

## Non-Goals

- No web target or Fetch engine.
- No ClientB backend implementation beyond existing behavior.
- No DTO/domain merging.
- No API endpoint or authentication-flow redesign.
- No DRM/entitlement additions.

## Implementation Tasks

1. Convert both provider data modules to plain KMP Android libraries and move portable DTOs, mappers, API contracts, repositories, and tests to common
   source sets. Enable `withHostTest` in every converted provider module containing `commonTest`; execute those tests with
   `testAndroidHostTest`.
2. Preserve all provider DTOs as `internal`; no core/domain/feature module may import them.
3. Introduce the common configuration value:

```kotlin
data class TmdbRuntimeConfig(
    val baseUrl: String,
    val readAccessToken: String,
    val accountId: String,
)
```

4. Remove direct provider-module `BuildConfig` access. Create `TmdbRuntimeConfig` in the TMDB app flavor from the existing Gradle/local properties and
   inject it through Koin.
5. Keep Ktor request policy common: content negotiation, `expectSuccess`, default base URL, authorization header, and timeout values.
6. Create the Android Ktor client/engine in `androidMain` with OkHttp. Common repositories depend on `HttpClient` or the provider API contract, never
   on OkHttp.
7. Replace JVM-specific error mapping:
    - Use Ktor common timeout/request exceptions.
    - Preserve HTTP/auth/server/parsing `AppError` mapping.
    - Rethrow cancellation.
8. Replace Java calendar/locale operations with the common date/time and invariant string APIs established in KMP-02.
9. Move TMDB auth DataStore repository logic to common code; create its qualified Android DataStore instance in `androidMain` with the exact existing
   filename and keys.
10. Convert both client player modules into KMP playback-source modules containing only provider source resolution and Koin bindings.
11. Move Media3 session-factory registration to `:playback:media3`. `:app` includes the selected provider source module plus the Android playback
    engine independently.
12. Keep the current TMDB public Sintel DASH source and ClientB dummy source unchanged.
13. Migrate provider repository/mapper/auth tests to common tests and keep HTTP behavior deterministic through fake engines/APIs.

## Public API or Type Changes

- Add `TmdbRuntimeConfig` as the provider's injected configuration contract.
- Provider Koin modules expose common repository/API/playback-source bindings plus Android engine/storage modules.
- `PlaybackSessionFactory` implementation ownership moves from client player modules to `:playback:media3`.
- DTOs remain internal and public repository interfaces remain provider-neutral.

## Verification Commands

```powershell
.\gradlew.bat :client:tmdb:data:compileCommonMainKotlinMetadata
.\gradlew.bat :client:clientB:data:compileCommonMainKotlinMetadata
.\gradlew.bat :client:tmdb:player:compileCommonMainKotlinMetadata
.\gradlew.bat :client:clientB:player:compileCommonMainKotlinMetadata
.\gradlew.bat :client:tmdb:data:testAndroidHostTest
.\gradlew.bat :client:clientB:data:testAndroidHostTest
.\gradlew.bat :playback:media3:compileDebugKotlin
.\gradlew.bat :app:assembleTmdbDebug
.\gradlew.bat :app:assembleClientBDebug
rg -n "BuildConfig|io\.ktor\.client\.engine\.okhttp|^import (android|java|androidx\.annotation|androidx\.core)\." client/tmdb/data/src/commonMain client/clientB/data/src/commonMain client/tmdb/player/src/commonMain client/clientB/player/src/commonMain -g "*.kt"
```

`BuildConfig` and OkHttp may exist only in Android/app source sets, not in provider common code.

## Test Scenarios

- TMDB request headers/base URL/timeouts match the Android baseline.
- Empty runtime credentials fail through the existing auth/error path rather than crashing graph creation.
- TMDB login/session/account persistence remains compatible across app restart.
- TMDB catalog, details, search, profiles, and auth repository tests retain their mappings.
- ClientB repositories retain deterministic existing behavior.
- TMDB and ClientB playback-source repositories resolve their current DASH sources.
- Selecting one app flavor does not register the other provider's definitions.

## Acceptance Criteria

- Both provider common metadata/test suites pass.
- Every provider data test file recorded in the accepted KMP-00 inventory is represented in executable host tests, and executed test-case counts
  match the baseline plus any tests added during migration; no common suite may disappear behind a zero-test task.
- Both Android flavor APKs assemble and complete the baseline provider flows.
- No provider token/account/base URL is hard-coded into common source.
- No provider DTO escapes its client module.
- Playback provider selection and Android playback-engine selection are independent.
- No browser target has been added.

## Handoff Checklist

- [ ] Provider source-set moves listed.
- [ ] Runtime configuration ownership documented.
- [ ] Ktor engine/error mapping documented.
- [ ] Auth DataStore filename/key compatibility confirmed.
- [ ] Playback module ownership change documented.
- [ ] Both flavor build/test results included.
- [ ] Executed provider host-test counts compared with KMP-00.
- [ ] Final working tree is clean after committing this ticket.
