# KMP-02 verification evidence

## Identity and scope

- Branch: `codex/kmp-02-foundation-core`
- Foundation commit under test: `9e72bebb6d064864a57b8d1cb16bc8b854e34b09`
- Review-fix verification date: 2026-09-01 (Europe/Athens)
- Target architecture: backend-agnostic Android-only KMP foundation; no Wasm, JS, native, JVM desktop, or web target added.

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

## Static scans

```powershell
rg -n "^import (android|java|androidx\.annotation|androidx\.core)\." `
  core/data/src/commonMain core/domain/src/commonMain -g "*.kt"
```

Result: no matches.

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
