# KMP-04 verification evidence

## Identity and scope

- Branch: `codex/kmp-04-feature-logic-b`.
- Start commit: `b08e30599a5b9cd4a0c366fc864aee979300fe11`.
- KMP-04 implementation commit: `98032346534e444404c062af4ba999954ace7992`.
- Integrated SDK 37 foundation commit: `c11bd0834a07e1b74faf33d4639b1a72e994b2d2`.
- Tested merge commit: `aa70b0a7773963eabea06cf6ae46e39ccd361965`.
- Verification date: 2026-09-01 (Europe/Athens).
- Target architecture: backend-agnostic Android-only KMP; no Wasm target, web storage, browser playback, provider migration, or player UI migration.

KMP-04 migrates Search data/domain, Library data/domain, Player data/domain, and the provider-neutral Playback API. Media3 remains Android-only and
compiled unchanged against the migrated API. Login, Profiles, Details, Home, provider modules, root build files, the version catalog, and KMP build
conventions were not edited.

## Source-set moves by module

| Module | Production | Tests | Convention |
|---|---|---|---|
| `:feature:search:data` | Repository, serializer model, qualifiers to `commonMain`; DataStore creation/Koin module to `androidMain` | Repository tests to `commonTest` | `streamcore.kmp.library` + host test |
| `:feature:search:domain` | Repository contracts, normalizer, use cases, Koin module to `commonMain` | Use-case tests to `commonTest` | `streamcore.kmp.library` + host test |
| `:feature:library:data` | Repository, compact persistence models, mappers, qualifiers to `commonMain`; DataStore creation/Koin module to `androidMain` | Repository tests to `commonTest` | `streamcore.kmp.library` + host test |
| `:feature:library:domain` | Use cases, Koin module, and new clock contract/implementation to `commonMain` | Use-case tests to `commonTest` | `streamcore.kmp.library` + host test |
| `:feature:player:data` | Progress repository and qualifiers to `commonMain`; DataStore creation/Koin module to `androidMain` | Repository tests to `commonTest` | `streamcore.kmp.library` + host test |
| `:feature:player:domain` | Resume policy to `commonMain` | Compile-only; no filler test | `streamcore.kmp.library` |
| `:playback:api` | All provider-neutral contracts/models, Compose video surface, and filmstrip model to `commonMain` | Compile-only; no filler test | `streamcore.kmp.compose.library` |

The compile-only modules remove their unused default `commonTest` source set. KMP-04 contributes five common-test modules and five executable
Android host-test targets; after KMP-03 integration, `verifyKmpTestTargets` reports 11 repository-wide common-test modules and 11 host-test targets.

## Persistence compatibility

Android construction remains in the owning data module and uses `androidContext().applicationContext.preferencesDataStoreFile(...)`.

| Store | Koin qualifier | Existing filename | Existing preference key |
|---|---|---|---|
| Search history | `searchHistoryStore` | `search_history.preferences_pb` | `recent_searches_json` |
| Library | `libraryStore` | `library.preferences_pb` | `library_json` |
| Playback progress | `playbackProgressStore` | `playback_progress.preferences_pb` | `entries_json` |

The JSON qualifiers remain `searchHistoryJson` and `libraryJson`; playback JSON is now explicitly qualified as `playbackProgressJson` to avoid an
unqualified process-wide `Json` binding. Common repositories depend only on injected `DataStore<Preferences>` and `Json` instances. Common
dependencies use DataStore core/preferences-core; Android creation retains `datastore-preferences`.

Compatibility tests seed the pre-migration keys with the old JSON shape and recreate repository instances over the same store. Search queries,
compact Library content/membership timestamps, and Playback progress all remain readable. Library mutation and Playback update timestamps remain
epoch milliseconds; no keys, filenames, versions, field names, thresholds, completion fraction, or per-profile caps changed.

Read failures recover according to the existing repository contract: Search and Playback expose an empty current state; Library exposes an
`AppResult.Failure`. `CancellationException` is always rethrown. Mutations remain atomic through `DataStore.updateData`/`edit`, and profile filters
remain isolated.

## Public API and clock

Playback public names and behavior are unchanged. `PlaybackSession`, `PlaybackSessionFactory`, `PlaybackSourceRepository`,
`PlaybackProgressRepository`, `PlaybackVideoSurface`, track/state/filmstrip models, and serialized request/progress models are now KMP-published.
Media3, `Context`, Android bitmap/cache, and playback-engine code remain outside common source sets.

Library adds the feature-owned `LibraryClock` contract and `SystemLibraryClock`, implemented with `kotlin.time.Clock.System`. The liked/my-list use
cases accept the clock by constructor, use `nowEpochMillis()` for omitted mutation timestamps, and retain an explicit timestamp parameter for
callers/tests. The domain Koin module registers the system clock; tests use deterministic fake clocks. A default system clock preserves source
compatibility for direct constructors in existing Android/JVM tests.

`PlayerViewModel` remains Android UI and still calls `System.currentTimeMillis()` when creating progress entries. Player UI migration and its clock
injection belong to KMP-06; KMP-04 does not edit `:feature:player:ui-common`.

## Verification

### Shared compilation and host tests

Command (AGP 9 task names; `compileAndroidMain` compiles the Android-only target's `commonMain` sources):

```powershell
.\gradlew.bat :feature:search:data:compileAndroidMain :feature:search:domain:compileAndroidMain `
  :feature:library:data:compileAndroidMain :feature:library:domain:compileAndroidMain `
  :feature:player:data:compileAndroidMain :feature:player:domain:compileAndroidMain `
  :playback:api:compileAndroidMain :feature:search:data:testAndroidHostTest `
  :feature:search:domain:testAndroidHostTest :feature:library:data:testAndroidHostTest `
  :feature:library:domain:testAndroidHostTest :feature:player:data:testAndroidHostTest --console=plain
```

Result: `BUILD SUCCESSFUL` in 17s; 61 actionable tasks. No expected test task executed zero tests.

| Module | Tests | KMP-00 baseline | Delta | Failures/errors/skips |
|---|---:|---:|---:|---:|
| `:feature:search:data` | 4 | 2 | +2 | 0/0/0 |
| `:feature:search:domain` | 2 | 2 | 0 | 0/0/0 |
| `:feature:library:data` | 7 | 5 | +2 | 0/0/0 |
| `:feature:library:domain` | 5 | 4 | +1 | 0/0/0 |
| `:feature:player:data` | 5 | 3 | +2 | 0/0/0 |
| **Total** | **23** | **16** | **+7** | **0/0/0** |

The added seven tests cover legacy preference fixtures, repository recreation, read failure/cancellation, and deterministic epoch-millisecond clocks.

Clock source-compatibility rerun:

```powershell
.\gradlew.bat :feature:library:domain:testAndroidHostTest `
  :feature:details:ui-common:compileDebugUnitTestKotlin --console=plain
```

Result: `BUILD SUCCESSFUL` in 15s; 46 actionable tasks.

### Media3 and provider graphs

```powershell
.\gradlew.bat :playback:media3:compileDebugKotlin `
  :app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin --console=plain
```

Result: `BUILD SUCCESSFUL` in 43s; 368 actionable tasks. Media3 and both provider player modules compiled against the KMP Playback API; both app
flavors compiled without source/API changes.

### Static boundaries

```powershell
rg -n "^import (android|java|androidx\.annotation|androidx\.core)\." `
  feature/search/data/src/commonMain feature/search/domain/src/commonMain `
  feature/library/data/src/commonMain feature/library/domain/src/commonMain `
  feature/player/data/src/commonMain feature/player/domain/src/commonMain `
  playback/api/src/commonMain -g "*.kt"
```

Result: no matches. A separate scan found no `System.currentTimeMillis`, `java.io.IOException`, `PreferenceDataStoreFactory`, or
`preferencesDataStoreFile` in the migrated common source sets. `git diff --check` passed.

### Integrated root verification

```powershell
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --no-parallel `
  '-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8' --console=plain
```

Result on tested merge commit `aa70b0a7773963eabea06cf6ae46e39ccd361965`: `BUILD SUCCESSFUL` in 3m 2s; 1,977 actionable tasks (728 executed,
230 from cache, 1,019 up-to-date). The SDK 37 integration raises compile SDK only; target SDK remains 36. All prior Compose 1.12 AAR metadata
failures are absent, including Player data, Playback API/Media3, both provider players, Android UI consumers, and both app variants.

- `verifyKmpTestTargets`: 11 common-test modules and 11 Android host-test targets; `:core:domain` remains the compile-only exemption.
- `verifyKmpAndroidCompilerFlags`: one production Compose-KMP Android task with `-Xlambdas=class`.
- `verifyKmpConventionPlugins`: two convention fixtures green.
- `verifyKmpDependencyCompatibility`: 14 locked common and Android component selections green.
- `verifyDesignTokens`: 300 production Kotlin files checked.
- XML inventory: 254 tests across 26 executed test tasks; zero failures, errors, or skips. KMP-04's five host-test tasks remain 23/23 green.
- `:playback:media3:compileDebugKotlin`, `:app:compileTmdbDebugKotlin`, and `:app:compileClientBDebugKotlin` executed successfully in the root graph.

The earlier pre-integration root attempt is retained as diagnostic history: it exposed the Compose 1.12/compile-SDK 36 AAR metadata conflict and a
direct-constructor clock compatibility issue. The reviewed SDK 37 foundation integration fixed the former without changing target SDK; KMP-04's
default `SystemLibraryClock` fixed the latter without editing KMP-03 files.

## Bridge-removal candidate and deferred work

- `:feature:search:domain` is ready for removal from the root transitional JVM-core consumer list after integration. KMP-04 intentionally preserved
  the bridge entry. No other KMP-04 module is in that list.
- KMP-05 owns provider migration. KMP-06 owns Player UI migration and replacing its Android wall-clock call. WEB-01/WEB-04 own web storage/playback.

The KMP-04 implementation commit modified no KMP-03-owned production/test file, provider file, root build file, convention, version catalog,
generated output, credential, or unrelated user file. The later merge commit brings in the separately reviewed KMP-03 and SDK 37 integration
changes without an owned-file conflict.
