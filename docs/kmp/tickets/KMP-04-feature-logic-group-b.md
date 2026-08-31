# KMP-04 — Migrate Feature Logic Group B, Persistence, and Playback Contracts

## Goal

Convert search, library, player, and provider-neutral playback contracts to KMP, including multiplatform persistence with Android storage adapters.

## Context

These modules contain the main Android-coupled shared-data concerns: DataStore construction, `Context`, JVM I/O exceptions, wall-clock access, and
Compose graphics in playback contracts. The repository logic remains portable once storage and platform engines are isolated.

## Dependencies and Parallelization

- **Depends on:** KMP-02.
- **Parallel with:** KMP-03.
- **Blocks:** KMP-05.
- **Ownership rule:** This ticket must not modify login, profiles, details, home, provider, root convention, or version-catalog files.

## In-Scope Modules

- `:feature:search:data`
- `:feature:search:domain`
- `:feature:library:data`
- `:feature:library:domain`
- `:feature:player:data`
- `:feature:player:domain`
- `:playback:api`
- Android persistence adapters owned by those modules.

`:playback:media3` remains Android-only and is changed only as required to implement the migrated API.

## Non-Goals

- No player UI migration.
- No web storage or browser playback implementation.
- No provider playback-source migration; that belongs to KMP-05.
- No Room/database introduction.
- No persistence schema/key changes.

## Implementation Tasks

1. Apply plain or Compose KMP conventions as appropriate:
    - Data/domain modules use plain KMP.
    - `:playback:api` uses Compose KMP because `PlaybackVideoSurface` and filmstrip frames expose Compose graphics/UI types.
2. Move portable production code to `commonMain` and portable tests to `commonTest`.
3. Enable `withHostTest` in every converted module that contains `commonTest`; `testAndroidHostTest` must execute those common tests.
4. Replace Android DataStore dependencies with `datastore-core` and `datastore-preferences-core` in common code.
5. Keep each repository dependent on an injected `DataStore<Preferences>`; it must not construct storage or access `Context`.
6. Define stable Koin qualifiers for three independent stores:
    - Search history.
    - Library.
    - Playback progress.
   Auth storage is explicitly owned by KMP-05 because it belongs to the TMDB provider.
7. Implement Android DataStore creation in `androidMain` using application context and the existing filenames. Register instances in Android Koin
   modules.
8. Preserve preference keys and serialized JSON so existing Android data remains readable after migration.
9. Replace `java.io.IOException` handling with common DataStore/storage failure handling. Cancellation must always be rethrown.
10. Replace wall-clock defaults with an injected/common clock abstraction. Tests use deterministic clocks; persisted values remain epoch milliseconds.
11. Keep `PlaybackSession`, `PlaybackSessionFactory`, `PlaybackSourceRepository`, `PlaybackProgressRepository`, and `PlaybackVideoSurface`
    provider-neutral.
12. Update `:playback:media3` dependencies/source imports only as needed to implement the KMP API; keep all Media3, Android bitmap/cache, and
    `Context` logic inside the Android module.
13. Migrate reducer/state/flow tests and add persistence compatibility tests that seed the pre-migration preference keys.

## Public API or Type Changes

- Playback API types become KMP-published but retain their names and behavior.
- Time-dependent use cases/repositories accept a clock/time provider instead of calling `System.currentTimeMillis()` directly.
- Persistence constructors receive qualified `DataStore<Preferences>` instances.
- Preference keys, epoch units, and playback progress semantics do not change.

## Verification Commands

```powershell
.\gradlew.bat :feature:search:data:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:search:domain:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:library:data:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:library:domain:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:player:data:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:player:domain:compileCommonMainKotlinMetadata
.\gradlew.bat :playback:api:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:search:data:testAndroidHostTest
.\gradlew.bat :feature:search:domain:testAndroidHostTest
.\gradlew.bat :feature:library:data:testAndroidHostTest
.\gradlew.bat :feature:library:domain:testAndroidHostTest
.\gradlew.bat :feature:player:data:testAndroidHostTest
.\gradlew.bat :playback:media3:compileDebugKotlin
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
rg -n "^import (android|java|androidx\.annotation|androidx\.core)\." feature/search/data/src/commonMain feature/search/domain/src/commonMain feature/library/data/src/commonMain feature/library/domain/src/commonMain feature/player/data/src/commonMain feature/player/domain/src/commonMain playback/api/src/commonMain -g "*.kt"
```

## Test Scenarios

- Existing recent searches load, add, remove, clear, and survive process recreation.
- Existing liked/my-list/library entries remain readable and mutation timestamps remain epoch milliseconds.
- Existing playback progress resumes and completion/removal policies remain unchanged.
- DataStore read corruption/I/O failure returns the current recoverable state; coroutine cancellation is not swallowed.
- Separate profiles do not leak library or progress data.
- Media3 creates independent playback sessions and closes them without leaks.
- Playback contract state/track/filmstrip models compile from common code.

## Acceptance Criteria

- Every in-scope module compiles as common metadata and Android.
- Every common test is executed through `testAndroidHostTest`, and per-module counts are not lower than the KMP-00 baseline.
- Persistence compatibility tests pass against pre-migration preference fixtures.
- Media3 and both Android app flavors compile against the migrated playback contract.
- No platform storage construction, Android/JVM imports, or provider implementation exists in common source sets.
- KMP-03 can merge before or after this ticket without source conflicts.

## Handoff Checklist

- [ ] Source-set moves listed by module.
- [ ] DataStore qualifiers and filenames recorded.
- [ ] Clock API and call sites documented.
- [ ] Preference compatibility results included.
- [ ] Executed host-test counts compared with KMP-00.
- [ ] Playback API/Media3 compilation results included.
- [ ] No KMP-03/root files modified.
- [ ] Final working tree is clean after committing this ticket.
