# KMP-02 verification evidence

## Identity and scope

- Tested branch: `codex/kmp-02-foundation-core`
- Prerequisite/integration start: `3689adcff1a41ca7df97a9d7531f8894f7da60bd` (`refactor(di): replace Hilt with Koin`)
- Foundation commit: `9e72bebb6d064864a57b8d1cb16bc8b854e34b09` (`build(kmp): establish core multiplatform foundation`)
- Tested code candidate: `fb5fec7b395db1d0de218714132329de4bd9f3a4` (`fix(kmp): harden foundation verification`)
- The root gate ran on the exact production/build tree later committed as `fb5fec7b...`. Its direct child is documentation-only and records this
  evidence; that hash is reported in the final agent handoff to avoid a self-referential commit ID in this file.
- Review-fix verification date: 2026-09-01 (Europe/Athens)
- Target architecture: backend-agnostic Android-only KMP foundation; no Wasm, JS, native, JVM desktop, or web target added.

## Changed files by subsystem

### Root and build logic

- `build.gradle.kts`: KMP verification tasks, single-variant coexistence, exact transitional consumer configuration, and root `check` wiring.
- `settings.gradle.kts`: `:core:tracing-api` plus the two non-production convention fixtures.
- `gradle/libs.versions.toml`: locked plugin/library aliases and versions.
- `build-logic/build.gradle.kts`, `build-logic/settings.gradle.kts`.
- `build-logic/src/main/kotlin/StreamCoreKmpLibraryPlugin.kt` and `StreamCoreComposeKmpLibraryPlugin.kt`.
- `kmp-convention-fixtures/plain/build.gradle.kts` and `kmp-convention-fixtures/compose/build.gradle.kts`.

### `:core:data`

- `core/data/build.gradle.kts` migrated from JVM to the plain Android-KMP convention with explicit host tests.
- All 28 production model files moved from `core/data/src/main/kotlin` to `core/data/src/commonMain/kotlin`; packages and type names did not change.
- Added three `commonTest` files covering UTC release years/time boundaries, serialization compatibility, and `AppResult`/`AppError` behavior.

### `:core:domain`

- `core/domain/build.gradle.kts` migrated from JVM to the plain Android-KMP convention.
- Five repository interfaces moved from `core/domain/src/main/kotlin` to `core/domain/src/commonMain/kotlin` without package or API changes.
- The unused default `commonTest` source set is removed; the module remains the explicit compile-only exemption.

### Tracing API and Android tracing

- New `core/tracing-api/build.gradle.kts`.
- New `core/tracing-api/src/commonMain/.../PerformanceTracer.kt` and `commonTest/.../PerformanceTracerTest.kt`.
- `core/tracing/build.gradle.kts` now exposes `:core:tracing-api`.
- `core/tracing/src/main/.../BenchmarkTrace.kt` adds the Android implementation while preserving existing Modifier/counter/wrapper APIs and
  benchmark/profile BuildConfig behavior.

### Documentation

- `AGENTS.md` adds KMP placement, official plugin/single-variant, host-test, and common-code restrictions.
- `docs/kmp/build-conventions.md` documents convention usage and bridge removal ownership.
- `docs/kmp/dependency-compatibility-matrix.md` records exact common, Android, Wasm, and Shaka metadata availability.
- `docs/kmp/KMP-02-evidence.md` is this ticket handoff/evidence record.

No provider, feature implementation, Android screen, playback implementation, credential, generated output, or unrelated user file was changed.

## Public API and compatibility

KMP-02 adds:

- `PerformanceTracer.enabled`, `beginSection(name)`, `endSection()`, and `counter(name, value)`.
- `NoOpPerformanceTracer`.
- Inline `PerformanceTracer.traceIfEnabled(label, block)`.
- `AndroidPerformanceTracer` in the Android-only tracing implementation.
- Build APIs `streamcore.kmp.library`, `streamcore.kmp.compose.library`, and `streamCoreKmp { withHostTest() }`.

`traceIfEnabled` checks `enabled` before evaluating `label`, executes `block` exactly once, always ends an enabled section through `finally`, and
keeps the disabled inline path free of label interpolation/captured-lambda allocation. The no-op tracer emits no sections or counters. The Android
implementation delegates to `android.os.Trace` and remains enabled only by the existing benchmark/profile-specific `BuildConfig.ENABLED` values.

Existing core model/repository packages, type names, serialized field names, collection shapes, and public epoch-millisecond fields are compatible
and unchanged. `Calendar`/JVM timezone calculations were replaced with `kotlin.time.Instant` plus `kotlinx-datetime` UTC conversion without changing
release-year semantics.

## Convention and dependency proof

Command:

```powershell
.\gradlew.bat verifyKmpConventionPlugins verifyKmpDependencyCompatibility `
  :kmp-convention-fixtures:plain:compileAndroidMain `
  :kmp-convention-fixtures:compose:compileAndroidMain --console=plain
```

Result: `BUILD SUCCESSFUL`; eight actionable tasks. The plain and Compose fixtures apply only their convention plugin and contain no module-local
Android target declaration. Both expose `compileAndroidMain`, `assembleAndroidMain`, `androidApiElements`, and `androidRuntimeElements`; the Compose
fixture also has Android-only `-Xlambdas=class`.

`verifyKmpDependencyCompatibility` resolved all 14 locked direct components for common metadata and Android. Common selected
`metadataApiElements` for every coordinate. Android selected the exact Android/release/JVM variant recorded in
[`dependency-compatibility-matrix.md`](dependency-compatibility-matrix.md). The independent, non-transitive configurations produced 14 common and
12 Android files; two valid published selections are fileless, so the gate checks 14 resolved direct component/variant selections on each side.

## Core compilation and host tests

Command:

```powershell
.\gradlew.bat verifyKmpConventionPlugins verifyKmpDependencyCompatibility verifyKmpTestTargets `
  :core:data:compileAndroidMain :core:domain:compileAndroidMain `
  :core:data:testAndroidHostTest :core:tracing-api:testAndroidHostTest `
  :core:tracing:compileDebugKotlin --console=plain
```

Result: `BUILD SUCCESSFUL`; 32 actionable tasks.

| Module | Task | Tests | Failures | Errors | Skipped |
|---|---|---:|---:|---:|---:|
| `:core:data` | `testAndroidHostTest` | 11 | 0 | 0 | 0 |
| `:core:tracing-api` | `testAndroidHostTest` | 4 | 0 | 0 | 0 |

`:core:domain` remains the explicit compile-only exemption: no `commonTest` directory and no Android host-test compilation. `verifyKmpTestTargets`
reported two common-test modules and two Android host-test targets. No expected test task executed zero tests.

The generated task inventory was also checked with:

```powershell
.\gradlew.bat :core:data:tasks --all --console=plain
```

Result: pass; it exposes `compileAndroidMain`, `compileAndroidHostTest`, and `testAndroidHostTest`. KGP 2.3.21 names the Android-only common metadata
lifecycle task `compileKotlinMetadata`; it was invoked successfully but is `SKIPPED` when no second publishable target needs standalone metadata.
The same `commonMain` sources compile through `compileAndroidMain`.

## Provider consumers

| Command | Result |
|---|---|
| `.\gradlew.bat :app:compileTmdbDebugKotlin --console=plain` | Pass; 360 actionable tasks |
| `.\gradlew.bat :app:compileClientBDebugKotlin --console=plain` | Pass; 358 actionable tasks |

The temporary JVM consumer bridge is restricted to main/test compile/runtime classpaths in six named JVM modules. KMP-03 removes Login, Profiles,
Home, and Details-owned entries; KMP-04 removes Search. The bridge is deleted when the last listed consumer migrates.

## Root gate

Command:

```powershell
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain
```

Result: `BUILD SUCCESSFUL` in 2m 32s; 2,001 actionable tasks (1,444 executed, 158 from cache, 399 up-to-date).

- `verifyDesignTokens`: 298 production Kotlin files checked, including migrated `commonMain`; zero violations.
- `verifyKmpConventionPlugins`: two convention-only fixture modules verified.
- `verifyKmpDependencyCompatibility`: 14 common plus 14 Android direct selections verified.
- `verifyKmpTestTargets`: two common-test modules plus two host-test targets verified.
- `verifyKmpAndroidCompilerFlags`: zero production Compose-KMP Android tasks, as expected until KMP-06. The Compose convention fixture verifies the
  flag itself without being counted as a production module.
- Full generated XML inventory: 234 tests, zero failures/errors/skips. The frozen KMP-00 suites remain present; KMP-02 contributes 11 core-data and
  four tracing-api tests.

### Accepted baseline comparison

The accepted KMP-00 baseline contains 215 passing tests. The current KMP-02 inventory contains 234 passing tests, a net increase of 19 with no
missing expected suite:

- KMP-01 increased the two app variants from 23 to 25 tests each: +4 total.
- KMP-02 adds 11 `:core:data` host tests and four `:core:tracing-api` host tests: +15 total.
- Every other accepted baseline module/task retains its recorded test count.
- `:core:domain` is compile-only by ticket contract and therefore has no zero-test failure or test-count requirement.

## Static scans

```powershell
rg -n "^import (android|java|androidx\.annotation|androidx\.core)\." `
  core/data/src/commonMain core/domain/src/commonMain -g "*.kt"
```

Result: no matches.

`git diff --check` also passed. No unrelated files were modified. After the documentation-only evidence commit, `git status --short` produced no
output.

```powershell
rg -n "(^|\s)wasmJs\s*\(" -g "*.gradle.kts" -g "*.kts"
```

Result: no matches.

## Remaining limitations

- KGP 2.3.21 names the Android-only metadata lifecycle task `compileKotlinMetadata`; it is skipped when no separately publishable second target is
  present. `commonMain` is compiled by `compileAndroidMain`.
- KMP-06 owns the first production Compose-KMP compiler-task proof.
- WEB-01 owns the first Wasm dependency resolution/link. Published Wasm availability is metadata evidence only.
- `shaka-player@5.2.3` has no npm `module`, `type`, or `exports` field; WEB-04 must validate the compiled global build or a wrapper.
- The temporary bridge is limited to four classpaths in six named JVM consumers. KMP-03 removes Login, Profiles, Home, and Details-owned entries;
  KMP-04 removes Search. The final owner deletes the bridge when the last named consumer has migrated and both provider graphs compile without it.
