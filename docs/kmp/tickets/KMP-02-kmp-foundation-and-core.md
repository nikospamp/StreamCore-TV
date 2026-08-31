# KMP-02 — Introduce KMP Build Conventions and Migrate Core

## Goal

Establish the supported AGP 9 Kotlin Multiplatform build model and migrate the lowest-level core modules without adding a web target.

## Context

AGP 9 requires `com.android.kotlin.multiplatform.library` for KMP Android libraries. It uses single-variant Android targets, `commonMain`/
`androidMain`, and opt-in host/device tests. The Android application and platform libraries remain unchanged.

## Dependencies and Parallelization

- **Depends on:** KMP-01.
- **Blocks:** KMP-03 and KMP-04.
- **Parallelization:** None. This ticket owns root build logic, version aliases, and shared KMP conventions.

## In Scope

- Root/version-catalog KMP plugin configuration.
- Minimal convention plugins for plain and Compose KMP libraries.
- `:core:data` and `:core:domain`.
- A KMP tracing API contract plus the existing Android tracing implementation.
- Common date/time replacement and core tests.

## Non-Goals

- No `wasmJs`, iOS, desktop, or JVM desktop target.
- No feature or provider module migration.
- No Compose/resource migration yet.
- No change to app flavors/build types or the Android tracing build-type switches.

## Implementation Tasks

1. Add plugin aliases:
    - `org.jetbrains.kotlin.multiplatform` at the Kotlin version.
    - `com.android.kotlin.multiplatform.library` at the AGP version.
    - `org.jetbrains.compose` `1.12.0`.
    - Retain `org.jetbrains.kotlin.plugin.compose` at the Kotlin version.
2. Add `kotlinx-datetime` `0.8.0` and the future shared dependency aliases required by KMP-03/KMP-04 so parallel tickets do not edit the version
   catalog.
3. Add minimal build conventions:
    - Plain KMP Android library: Android target, compile/min SDK, JVM 11, common tests.
    - Compose KMP Android library: plain convention plus Compose Multiplatform/compiler/resources.
    - Do not automatically enable host/device tests; each module opts in with `withHostTest`/`withDeviceTest` only when it owns those tests.
4. Convert `:core:data` and `:core:domain` in place:
    - Move portable code to `src/commonMain/kotlin`.
    - Move portable tests to `src/commonTest/kotlin`.
    - Place any unavoidable Android implementation in `androidMain`; none is expected in core domain.
5. Replace `Calendar`, `TimeZone`, and JVM time calculations with `kotlin.time` and `kotlinx-datetime`, retaining UTC release-year semantics and
   existing serialization shapes.
6. Add a KMP tracing contract exposing:
    - `PerformanceTracer.enabled`.
    - `trace(name, block)`.
    - `counter(name, value)`.
    - A no-op implementation.
7. Keep `:core:tracing` as an Android library because its `BuildConfig` values differ by benchmark/profile build type. Make its Android implementation
   satisfy the KMP tracing contract without changing existing Android Modifier tracing APIs.
8. Add or migrate tests for release-year conversion, time edge cases, result/error models, and repository-contract compilation.
9. Document the convention-plugin usage and source-set rules for later tickets.

## Public API or Type Changes

- Introduce `PerformanceTracer` and `NoOpPerformanceTracer` in a shared tracing API package/module.
- Core model/repository packages and serialized field names remain unchanged.
- Time implementation changes must not change public epoch-millisecond fields.

## Verification Commands

Use the generated task list once to confirm AGP 9 task names, then run:

```powershell
.\gradlew.bat :core:data:tasks --all
.\gradlew.bat :core:data:compileCommonMainKotlinMetadata
.\gradlew.bat :core:data:compileAndroidMain
.\gradlew.bat :core:domain:compileCommonMainKotlinMetadata
.\gradlew.bat :core:domain:compileAndroidMain
.\gradlew.bat :core:data:allTests
.\gradlew.bat :core:domain:allTests
.\gradlew.bat :core:tracing:compileDebugKotlin
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
rg -n "^import (android|java)\." core/data/src/commonMain core/domain/src/commonMain -g "*.kt"
```

The final search must return no matches.

## Test Scenarios

- Positive, zero, and invalid epoch values produce the same metadata text as the baseline.
- UTC year conversion is independent of host timezone.
- Serialized core models remain backward-compatible.
- No-op tracing executes the wrapped block exactly once and never emits counters.
- Android tracing still activates only in the existing benchmark/profile build types.

## Acceptance Criteria

- Core common metadata, Android target, and common tests pass.
- Android app consumers compile for both providers.
- KMP conventions are documented and reusable by KMP-03/KMP-04 without root edits.
- No web/native target exists yet.
- `commonMain` has no Android/JVM imports.

## Handoff Checklist

- [ ] Plugin/dependency versions recorded.
- [ ] Convention behavior and opt-in test behavior recorded.
- [ ] Source moves summarized.
- [ ] Public tracing API documented.
- [ ] Common/Android/app verification results included.
- [ ] No unrelated files changed.
- [ ] Final working tree is clean after committing this ticket.

