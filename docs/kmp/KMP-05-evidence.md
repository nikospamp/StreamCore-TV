# KMP-05 verification evidence

## Identity and scope

- Branch: `codex/kmp-05-provider-migration`.
- Prerequisite/integration start: `8235e7dbd1ae643d3f75e21ee54a011f9d233ace`.
- Verification date: 2026-09-01 (Europe/Athens).
- Scope: TMDB and ClientB data implementations, both provider playback-source modules, TMDB app-flavor runtime configuration, and provider-neutral
  Media3 Koin ownership.
- Target architecture: backend-agnostic Android-only KMP Phase 1. Compile SDK remains 37 and application/benchmark/profile target SDK remains 36.
  No Wasm/browser target, DRM, web storage, provider UI/resource migration, or KMP-06 work was added.

The root gate ran on the exact production/build/test tree later committed by the KMP-05 agent. The final handoff identifies that commit; the evidence
file cannot contain its own commit hash without a documentation-only follow-up commit.

## Source-set moves by module

| Module | Portable production | Platform production | Executable tests |
|---|---|---|---|
| `:client:tmdb:data` | DTOs, API/endpoints, common Ktor request policy, error/cancellation mapping, reference data, mappers, repositories, auth store logic, Koin repository bindings, `TmdbRuntimeConfig` | OkHttp engine/client construction and exact qualified DataStore creation in `androidMain` | 11 files in `commonTest`; `testAndroidHostTest` |
| `:client:clientB:data` | DTOs, deterministic catalog source, mappers, repositories, auth store logic, and Koin repository bindings | Exact qualified DataStore creation in `androidMain` | Seven files in `commonTest`; `testAndroidHostTest` |
| `:client:tmdb:player` | Sintel DASH source resolver and its Koin binding | None | One source-resolution/Koin test in `commonTest` |
| `:client:clientB:player` | Existing dummy DASH source resolver and its Koin binding | None | One source-resolution/Koin test in `commonTest` |

All four modules now apply `streamcore.kmp.library` and explicitly call `streamCoreKmp { withHostTest() }`. Production and pre-existing provider
tests moved from `main`/`test` into `commonMain`/`commonTest`. All JUnit assertions/annotations in migrated tests were replaced with `kotlin.test`.

TMDB's Android manifest moved to `androidMain`. Client UI/artwork/error resources remain Android libraries unchanged for KMP-06.

## Public API and provider isolation

KMP-05 adds the public, provider-owned configuration contract:

```kotlin
data class TmdbRuntimeConfig(
    val baseUrl: String,
    val readAccessToken: String,
    val accountId: String,
)
```

The old public primitive qualifier constants for base URL/token/account ID are replaced by this typed contract. Public backend-agnostic repository
interfaces, model packages, serialized field names, endpoint paths, request parameters, and `AppResult`/`AppError` shapes are unchanged.

Every provider DTO remains `internal`. Static scans found no provider DTO import in app/core/feature/playback or provider UI, and common sources contain
no Android/JVM imports. The backend-agnostic core and feature boundaries were not edited.

## Runtime configuration and security ownership

TMDB `BuildConfig` fields moved from `:client:tmdb:data` to the TMDB application product flavor. The app build reads the existing Gradle/local
properties `tmdbReadAccessToken` and `tmdbAccountId`, creates `TmdbRuntimeConfig` in `app/src/tmdb`, and injects the typed value through the flavor's
Koin composition root. The base URL default is also app-owned.

Provider common code has no `BuildConfig`, local-property access, token/account/base-URL literal, logging configuration, credential print, or secret
fallback. Empty credentials create the client/graph safely, add no Authorization header, and fail only when an operation requires valid remote auth.
The two app flavors compile from mutually exclusive source sets and include exactly one provider aggregate module.

## Ktor engine split and error policy

`createTmdbHttpClient` in `commonMain` owns the frozen request policy:

- `expectSuccess = true`;
- JSON content negotiation with unknown-key tolerance;
- configured default base URL;
- JSON Accept header;
- conditional Bearer authorization;
- connect timeout 10,000 ms, request timeout 15,000 ms, socket timeout 15,000 ms.

`tmdbAndroidDataModule` creates `OkHttp` and the Android `HttpClient`; no common repository or API implementation imports or depends on OkHttp.
The narrowly authorized `ktor-client-mock` catalog alias uses the already locked Ktor 3.5.0 version only in TMDB `commonTest`.

Common error mapping uses Ktor common request/connect/socket timeout exceptions, `kotlinx.io.IOException`, and common serialization exceptions.
Authentication, 401/403, 408, 429, 5xx, parsing, network, timeout, and unknown mappings retain their existing `AppError`/`ErrorSource` behavior.
`TmdbCallExecutor` still rethrows `CancellationException`; a focused common test proves this.

TMDB date mapping now uses `kotlinx-datetime` `LocalDate` at UTC start-of-day instead of Java calendar/time-zone APIs. ClientB search uses common
invariant `lowercase()` rather than `Locale.ROOT`; ordering and match behavior remain covered by the migrated tests.

## Auth storage compatibility

TMDB Android DataStore creation remains qualified by `tmdbAuthStore` and uses the exact existing filename:

```text
tmdb_auth.preferences_pb
```

The common auth store retains the exact preference keys:

```text
session_id
account_id
account_username
account_display_name
```

Compatibility tests seed the pre-migration keys, construct and reconstruct `TmdbPreferencesAuthStore` over the same DataStore, prove the legacy
session remains readable, then prove save/clear remains readable across recreation. The exact filename and all four keys are asserted as part of the
executable common suite. Storage IO recovery and cancellation behavior remain unchanged.

ClientB retains its exact `client_b_auth` filename, `clientBAuthStore` qualifier, and `is_logged_in` key.

## Playback ownership

Provider player modules no longer depend on `:playback:media3`, Android `Context`, or Koin Android. They bind only their portable
`PlaybackSourceRepository` implementations:

- TMDB: `https://storage.googleapis.com/shaka-demo-assets/sintel/dash.mpd`, `application/dash+xml`.
- ClientB: the existing Roku TrickPlayThumbnails DASH URL, `application/dash+xml`.

`:playback:media3` now owns `media3PlaybackModule` and the process-scoped `PlaybackSessionFactory` binding. The app common Android composition root
always includes the Android engine, while each flavor independently selects exactly one provider source module. Koin's strict verifier and isolated
startup tests pass for both flavor graphs; provider and engine selection are no longer coupled.

## Verification commands and results

The documented ticket names `compileCommonMainKotlinMetadata`; with the locked AGP 9.1.1/KGP 2.3.21 Android-only target graph the actual task is
`compileKotlinMetadata`. It exists for all four converted modules and succeeds as `SKIPPED` because no second publishable target needs standalone
metadata. The same `commonMain` production sources compile through `compileAndroidMain`.

| Command | Result |
|---|---|
| `./gradlew :client:tmdb:data:compileAndroidMain :client:clientB:data:compileAndroidMain :client:tmdb:player:compileAndroidMain :client:clientB:player:compileAndroidMain :playback:media3:compileDebugKotlin` | Pass after replacing unavailable `kotlin.io.IOException` with the common `kotlinx.io.IOException`; all five production targets compile |
| `./gradlew :client:tmdb:data:compileKotlinMetadata :client:clientB:data:compileKotlinMetadata :client:tmdb:player:compileKotlinMetadata :client:clientB:player:compileKotlinMetadata ... verifyKmpTestTargets verifyKmpDependencyCompatibility verifyDesignTokensLogFiles` | `BUILD SUCCESSFUL`; 59 actionable tasks; four metadata lifecycle tasks present/`SKIPPED`; 15 common-test modules/15 host-test targets; 14 locked common/Android coordinates; 300 production files checked |
| `./gradlew :client:tmdb:data:testAndroidHostTest :client:clientB:data:testAndroidHostTest :client:tmdb:player:testAndroidHostTest :client:clientB:player:testAndroidHostTest` | `BUILD SUCCESSFUL`; provider suites 46/25/1/1, zero failures/errors/skips |
| `./gradlew :playback:media3:compileDebugKotlin :app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin` | Pass; both app/provider graphs compile; the combined app compile run reported 360 actionable tasks |
| `./gradlew :app:testTmdbDebugUnitTest :app:testClientBDebugUnitTest --tests '*KoinGraphTest'` | `BUILD SUCCESSFUL`; two Koin tests per flavor; 706 actionable tasks |
| `./gradlew :app:assembleTmdbDebug --no-parallel '-Dorg.gradle.jvmargs=-Xmx4096m ...'` | `BUILD SUCCESSFUL` in 1m 15s; 791 actionable tasks |
| `./gradlew :app:assembleClientBDebug --no-parallel '-Dorg.gradle.jvmargs=-Xmx4096m ...'` | `BUILD SUCCESSFUL` in 20s; 791 actionable tasks |
| `./gradlew testAndroidHostTest --no-parallel '-Dorg.gradle.jvmargs=-Xmx4096m ...'` | `BUILD SUCCESSFUL`; 149 actionable tasks; all 15 KMP host suites executed |
| `./gradlew check -PverifyDesignTokensLogFiles=true --continue --no-parallel '-Dorg.gradle.jvmargs=-Xmx4096m ...'` | `BUILD SUCCESSFUL` in 1m 46s; 1,873 actionable tasks; unit/host tests, lint, dependency/KMP/design guards green |

The temporary 4 GiB heap is a command-line-only local setting consistent with the SDK 37 integration evidence; no repository Gradle memory setting
changed.

Static commands:

```powershell
rg -n "BuildConfig|io\.ktor\.client\.engine\.okhttp|^import (android|java|androidx\.annotation|androidx\.core)\." `
  client/tmdb/data/src/commonMain client/clientB/data/src/commonMain `
  client/tmdb/player/src/commonMain client/clientB/player/src/commonMain -g "*.kt"

rg -n "^import com\.pampoukidis\.streamcoretv\.client\.(tmdb|clientb)\.data\.model\..*Dto" `
  app core feature playback client/tmdb/ui client/clientB/ui -g "*.kt"

rg -n "(^|\s)wasmJs\s*\(" -g "*.gradle.kts" -g "*.kts"
git diff --check
```

Results: all three scans produced no matches; `git diff --check` passed.

## Executed test inventory and baseline comparison

The clean root-check XML inventory contains 64 suite files and 264 tests with zero failures, errors, or skips.

| Module | Task | KMP-05 tests | KMP-00 baseline | Delta |
|---|---|---:|---:|---:|
| `:client:tmdb:data` | `testAndroidHostTest` | 46 | 38 | +8 |
| `:client:clientB:data` | `testAndroidHostTest` | 25 | 25 | 0 |
| `:client:tmdb:player` | `testAndroidHostTest` | 1 | 0 | +1 |
| `:client:clientB:player` | `testAndroidHostTest` | 1 | 0 | +1 |
| **KMP-05 provider total** |  | **73** | **63** | **+10** |

The eight added TMDB data tests cover exact storage constants/recreation, full common request policy, empty config safety, timeout/network/parsing
mapping, authentication details, and cancellation. The two player tests preserve the exact source URLs/MIME types and Koin source bindings.

Every other KMP-04 suite retains its accepted count. App variants remain 25 tests each; provider UI remains TMDB 4 and ClientB 3. The repository
total increases from the KMP-04 accepted 254 to 264 solely through the ten KMP-05 tests. No expected test task executed zero tests.

## Limitations and deferred work

- KMP-06 owns shared Compose presentation and client UI/resources; they remain Android-only and unchanged.
- KMP-07 owns the final device parity campaign and release/profile/benchmark gate. KMP-05 performed host compilation, unit/host tests, lint, strict
  Koin verification, and both debug APK assemblies; it did not run the device matrix or regenerate Baseline Profiles.
- WEB-01 owns the first Wasm target/engine/storage adapter. No browser target or Fetch engine was added.
- WEB-04 owns browser DASH playback. KMP-05 adds no DRM or Shaka integration.

No unrelated production, UI/resource, build-convention, root build, generated profile, credential, or local-path file was modified. The only frozen
catalog change is the parent-authorized Ktor 3.5.0 mock-engine alias used exclusively in TMDB common tests. Final clean status is recorded in the
agent handoff after committing this evidence and implementation.
