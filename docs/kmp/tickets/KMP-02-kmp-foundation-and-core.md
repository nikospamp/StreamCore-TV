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
- Root verification/build-logic adaptation for KMP source sets and the Android-KMP single-variant model.
- Version/dependency compatibility smoke for all locked KMP libraries not already resolved by KMP-01.
- `:core:data` and `:core:domain`.
- A KMP tracing API contract plus the existing Android tracing implementation.
- Common date/time replacement and core tests.

## Non-Goals

- No `wasmJs`, iOS, desktop, or JVM desktop target.
- No feature or provider module migration.
- No Compose/resource migration yet.
- No semantic change to app flavors/build types or the Android tracing build-type switches. Root build logic may and must be adapted so those
  Android-only features coexist with single-variant KMP libraries.

## Implementation Tasks

1. Add plugin aliases:
    - `org.jetbrains.kotlin.multiplatform` at the Kotlin version.
    - `com.android.kotlin.multiplatform.library` at the AGP version.
    - `org.jetbrains.compose` `1.12.0`.
    - Retain `org.jetbrains.kotlin.plugin.compose` at the Kotlin version.
2. Add and resolve the locked aliases required by later tickets: Compose Multiplatform `1.12.0`, `kotlinx-datetime` `0.8.0`, DataStore `1.2.1`,
   Coil `3.4.0` `coil-network-ktor3`, Ktor `3.5.0`, and Shaka version `5.2.3`. Koin `4.2.2` was already resolved by KMP-01. Record a compatibility
   matrix. Do not upgrade Coil to `3.6.0`, which is built with Kotlin `2.4.10` while this migration remains on Kotlin `2.3.21`.
   Resolve common/Android variants normally. Without adding a wasm target, inspect official Maven Gradle-module metadata and record the exact published
   `wasmJs` variants/coordinates for DataStore storage, Ktor JS/Fetch, Coil Ktor3, Koin Compose/ViewModel, and multiplatform Lifecycle/ViewModel.
   Record Shaka npm version/ESM availability separately. WEB-01 still owns the first actual wasm resolution/link.
3. Add minimal build conventions:
    - Plain KMP Android library: Android target, compile/min SDK, JVM 11, common tests.
    - Compose KMP Android library: plain convention plus Compose Multiplatform/compiler/resources.
    - Do not eagerly enable tests for modules without tests.
    - Every module with Kotlin files under `commonTest` must explicitly enable `withHostTest`; `testAndroidHostTest` is the Phase 1 executable for
      those common tests.
    - Add a root `verifyKmpTestTargets` check that fails when a KMP module contains common tests but has no Android host-test compilation.
    - Treat `:core:domain` as an explicit compile-only module: it contains repository interfaces only, creates no `commonTest`, enables no host-test
      compilation, and must not receive filler tests.
4. Convert `:core:data` and `:core:domain` in place:
    - Move portable code to `src/commonMain/kotlin`.
    - Move portable tests to `src/commonTest/kotlin`.
    - Place any unavoidable Android implementation in `androidMain`; none is expected in core domain.
5. Replace `Calendar`, `TimeZone`, and JVM time calculations with `kotlin.time` and `kotlinx-datetime`, retaining UTC release-year semantics and
   existing serialization shapes.
6. Add a KMP tracing contract exposing:
    - `PerformanceTracer.enabled`.
    - `beginSection(name)` and `endSection()` primitives.
    - `counter(name, value)`.
    - A no-op implementation.
    - An inline `traceIfEnabled(label: () -> String, block: () -> T)` helper that checks `enabled` before evaluating the label and keeps the disabled
      path free of interpolated-string/lambda allocation.
7. Keep `:core:tracing` as an Android library because its `BuildConfig` values differ by benchmark/profile build type. Make its Android implementation
   satisfy the KMP tracing contract without changing existing Android Modifier tracing APIs.
8. Add or migrate executable tests in `:core:data` for release-year conversion, time edge cases, and result/error models. Verify
   `:core:domain` repository contracts through common/Android compilation only.
9. Document the convention-plugin usage and source-set rules for later tickets.
10. Update the root design-token verifier to scan production Kotlin under `src/main/kotlin`, Kotlin files kept under Android's `src/main/java`,
    `src/commonMain/kotlin`, `src/androidMain/kotlin`, and `src/wasmJsMain/kotlin`; update allow-list paths for migrated token files. Fail/report if
    it checks zero production UI files.
11. Audit and adapt every root `subprojects` hook that currently keys only on `com.android.library` or `KotlinAndroidProjectExtension`:
    - Keep benchmark/profile build-type creation only for Android-only libraries; KMP libraries remain single-variant and must resolve from every app
      build type.
    - Move tracing dependencies toward explicit module ownership: Android UI modules use `:core:tracing`, while shared modules use the KMP tracing
      API. Remove obsolete hook entries as their modules migrate.
    - Configure `-Xlambdas=class` for the future Android compilation of Compose KMP UI modules through the KMP convention; never apply the JVM-only
      flag to wasm or common metadata compilations. Add `verifyKmpAndroidCompilerFlags`, which enumerates Compose KMP Android compile tasks and checks
      the effective argument. It may report zero applicable modules in KMP-02; KMP-06 owns the first nonzero proof.
    - Preserve Compose compiler report/metrics behavior for KMP Android compilation tasks or document the exact replacement tasks.
12. Wire root `check` to `verifyDesignTokens` and `verifyKmpTestTargets` and prove both tasks execute rather than becoming no-ops.
13. Update `AGENTS.md` with KMP source-set placement, Android-KMP plugin/single-variant rules, host/device test naming, and the prohibition on
    Android/JVM APIs in common code.

## Public API or Type Changes

- Introduce `PerformanceTracer` and `NoOpPerformanceTracer` in a shared tracing API package/module.
- Introduce the inline `traceIfEnabled` helper as the only shared tracing wrapper for allocation-sensitive call sites.
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
.\gradlew.bat :core:data:testAndroidHostTest
.\gradlew.bat :core:tracing:compileDebugKotlin
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
.\gradlew.bat check -PverifyDesignTokensLogFiles=true
rg -n "^import (android|java|androidx\.annotation|androidx\.core)\." core/data/src/commonMain core/domain/src/commonMain -g "*.kt"
```

The final search must return no matches.

## Test Scenarios

- Positive, zero, and invalid epoch values produce the same metadata text as the baseline.
- UTC year conversion is independent of host timezone.
- Serialized core models remain backward-compatible.
- No-op tracing executes the wrapped block exactly once and never emits counters.
- Android tracing still activates only in the existing benchmark/profile build types.

## Acceptance Criteria

- Core common metadata and Android targets compile. `:core:data:testAndroidHostTest` executes its expected tests; `:core:domain` is compile-only and
  has no test task/count requirement.
- Android app consumers compile for both providers.
- KMP conventions are documented and reusable by KMP-03/KMP-04 without root edits.
- Root design-token verification checks migrated source-set paths, and `verifyKmpTestTargets` protects every later common test suite.
- Existing app build types resolve single-variant KMP dependencies and Android-only benchmark/tracing behavior is proven. The Compose KMP convention
  contains the Android-only `-Xlambdas=class` configuration; effective-task proof is deferred explicitly to KMP-06.
- No web/native target exists yet.
- `commonMain` has no Android/JVM imports.

## Handoff Checklist

- [ ] Plugin/dependency versions recorded.
- [ ] Convention behavior and opt-in test behavior recorded.
- [ ] Dependency compatibility matrix recorded.
- [ ] Exact Maven wasm variant/coordinate availability recorded without adding a wasm target.
- [ ] Root build-logic hooks audited with before/after ownership.
- [ ] Executed host-test and design-token checked-file counts included.
- [ ] `AGENTS.md` KMP rules updated.
- [ ] Source moves summarized.
- [ ] Public tracing API documented.
- [ ] Common/Android/app verification results included.
- [ ] No unrelated files changed.
- [ ] Final working tree is clean after committing this ticket.
