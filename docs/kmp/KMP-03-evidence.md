# KMP-03 verification evidence

## Identity and scope

- Branch: `codex/kmp-03-feature-logic-a`.
- Prerequisite commit: `b08e30599a5b9cd4a0c366fc864aee979300fe11` (`docs(kmp): complete KMP-02 handoff evidence`).
- Verification date: 2026-09-01 (Europe/Athens).
- Scope: login data/domain, profiles data/domain, details data/domain, and home domain only.
- Target architecture: backend-agnostic Android-only KMP; no Wasm, provider, UI, navigation, persistence, playback, root convention, or version-catalog change.

## Changes by module

### `:feature:login:data`

- Replaced the JVM plugin/toolchain with `streamcore.kmp.library`.
- Moved all four production contracts from `src/main/kotlin` to `src/commonMain/kotlin` without package, visibility, field, enum, or shape changes.
- Removed the unused default `commonTest` source set. This remains a production-contract-only module and has no filler test.

### `:feature:login:domain`

- Replaced the JVM plugin/toolchain with `streamcore.kmp.library`; dependencies now belong to `commonMain` and host tests are explicitly enabled.
- Moved three production files to `commonMain` and the existing three validation tests to `commonTest` using `kotlin.test`.
- Added whitespace normalization validation and common Koin resolution coverage with a fake `AuthenticateRepository`.

### `:feature:profiles:data`

- Replaced the JVM plugin/toolchain with `streamcore.kmp.library`.
- Moved all five draft/editor/validation contracts to `commonMain` without API changes.
- Removed the unused default `commonTest` source set. This remains a production-contract-only module and has no filler test.

### `:feature:profiles:domain`

- Replaced the JVM plugin/toolchain with `streamcore.kmp.library`; dependencies now belong to `commonMain` and host tests are explicitly enabled.
- Moved eight production use-case/module files to `commonMain`.
- Added nine focused common tests for trimmed/blank/too-long names, missing/unknown selections, create/edit request identity, create/update mapping, update-without-identity failure, and Koin resolution with a fake `ProfileRepository`.

### `:feature:details:data`

- Replaced the JVM plugin/toolchain with `streamcore.kmp.library`; the core-data dependency now belongs to `commonMain`.
- Moved `DetailsModel` and `DetailsRequest` to `commonMain` without changing their fields or packages.
- Removed the unused default `commonTest` source set. This remains a production-contract-only module and has no filler test.

### `:feature:details:domain`

- Replaced the JVM plugin/toolchain with `streamcore.kmp.library`; dependencies now belong to `commonMain` and host tests are explicitly enabled.
- Moved two production files to `commonMain` and the existing two delegation tests to `commonTest` using `kotlin.test`.
- Strengthened request identity assertions and added common Koin resolution coverage with a fake `DetailsRepository`.

### `:feature:home:domain`

- Replaced the JVM plugin/toolchain with `streamcore.kmp.library`; dependencies now belong to `commonMain` and host tests are explicitly enabled.
- Moved two production files to `commonMain` and the existing delegation test to `commonTest` using `kotlin.test`.
- Added unchanged failure propagation/mapping-policy coverage and common Koin resolution coverage with a fake `HomeRepository`.

## API and compatibility

There are no public API or visibility changes. Existing packages, type names, constructors, repository interfaces, function signatures, Koin module
symbols, result/error semantics, collection shapes, and serialized shapes remain unchanged. All moved production code is common and provider-neutral.
Koin definitions use only Koin core constructor DSL APIs.

The three data modules contain no behavioral implementation to test, so they explicitly remove the unused default `commonTest` source set and do not
enable host tests. All four domain modules contain real common tests and explicitly opt in to `withHostTest()`.

## Compilation and targeted tests

The ticket's written `compileCommonMainKotlinMetadata` name does not exist in this AGP 9.1.1/KGP 2.3.21 Android-only target graph. The actual metadata
lifecycle task is `compileKotlinMetadata`; it succeeds but is `SKIPPED` because there is no separately publishable second target. The same
`commonMain` sources execute through `compileAndroidMain`.

```powershell
.\gradlew.bat :feature:login:data:compileAndroidMain :feature:login:domain:compileAndroidMain `
  :feature:profiles:data:compileAndroidMain :feature:profiles:domain:compileAndroidMain `
  :feature:details:data:compileAndroidMain :feature:details:domain:compileAndroidMain `
  :feature:home:domain:compileAndroidMain `
  :feature:login:domain:testAndroidHostTest :feature:profiles:domain:testAndroidHostTest `
  :feature:details:domain:testAndroidHostTest :feature:home:domain:testAndroidHostTest --console=plain
```

Result: `BUILD SUCCESSFUL`; 61 actionable tasks. Every production module compiled through `compileAndroidMain`; every expected common test executed
through `testAndroidHostTest`.

```powershell
.\gradlew.bat :feature:login:data:compileKotlinMetadata :feature:login:domain:compileKotlinMetadata `
  :feature:profiles:data:compileKotlinMetadata :feature:profiles:domain:compileKotlinMetadata `
  :feature:details:data:compileKotlinMetadata :feature:details:domain:compileKotlinMetadata `
  :feature:home:domain:compileKotlinMetadata verifyKmpTestTargets --console=plain
```

Result: `BUILD SUCCESSFUL`; 12 actionable tasks. All seven metadata lifecycle tasks were present and intentionally skipped; the verifier found six
repository-wide common-test modules and six matching Android host-test targets.

| Module | Task | Tests | KMP-00 baseline | Delta | Failures | Errors | Skipped |
|---|---|---:|---:|---:|---:|---:|---:|
| `:feature:login:domain` | `testAndroidHostTest` | 5 | 3 | +2 | 0 | 0 | 0 |
| `:feature:profiles:domain` | `testAndroidHostTest` | 9 | 0 | +9 | 0 | 0 | 0 |
| `:feature:details:domain` | `testAndroidHostTest` | 3 | 2 | +1 | 0 | 0 | 0 |
| `:feature:home:domain` | `testAndroidHostTest` | 3 | 1 | +2 | 0 | 0 | 0 |
| **KMP-03 owned total** |  | **20** | **6** | **+14** | **0** | **0** | **0** |

No expected host-test task executed zero tests. The three data modules have no test task by explicit compile-only design.

## Android consumers

```powershell
.\gradlew.bat :app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin --console=plain
```

Result: `BUILD SUCCESSFUL`; 401 actionable tasks. Both provider graphs consume the migrated backend-agnostic contracts.

## Root gate

```powershell
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain
```

Result: `BUILD SUCCESSFUL` in 3m 7s; 2,110 actionable tasks (1,326 executed, 365 from cache, 419 up-to-date). Generated XML inventory contains 247 tests,
zero failures, errors, or skips. `verifyKmpTestTargets`, convention fixtures, dependency compatibility, compiler-flag policy, tests, and lint are green.

The actual clean KMP-02 base contains 233 XML tests. KMP-03 adds 14, producing the verified total of 247. The KMP-02 evidence states 234 because it
retains one stale accepted-baseline entry for `:feature:player:ui-tablet`; that module and its one-test XML source are absent from the actual
`b08e305...` repository. KMP-03 did not modify player or shared evidence. Integration cleanup owns correction of that inherited 234-to-233 evidence
discrepancy.

```powershell
.\gradlew.bat verifyDesignTokensLogFiles --console=plain
```

Result: `BUILD SUCCESSFUL`; five actionable tasks. The verifier checked 298 production Kotlin files, explicitly including the KMP-03 `commonMain`
paths, with zero violations.

## Static verification

```powershell
rg -n "^import (android|java|androidx\.annotation|androidx\.core)\." `
  feature/login/data/src/commonMain feature/login/domain/src/commonMain `
  feature/profiles/data/src/commonMain feature/profiles/domain/src/commonMain `
  feature/details/data/src/commonMain feature/details/domain/src/commonMain `
  feature/home/domain/src/commonMain -g "*.kt"

rg -n "dagger\.|hilt|javax\.inject" `
  feature/login/data/src/commonMain feature/login/domain/src/commonMain `
  feature/profiles/data/src/commonMain feature/profiles/domain/src/commonMain `
  feature/details/data/src/commonMain feature/details/domain/src/commonMain `
  feature/home/domain/src/commonMain -g "*.kt"

git diff --check
```

Results: both scans produced no matches; `git diff --check` passed.

## Transitional bridge handoff

KMP-03 intentionally does not edit the root transitional bridge. These entries are now ready for post-merge removal:

- `:feature:login:domain`
- `:feature:profiles:domain`
- `:feature:home:domain`
- `:feature:details:data`
- `:feature:details:domain`

`:feature:search:domain` remains KMP-04-owned. The bridge block itself can be deleted only after the last entry is migrated and integrated.

## Limitations and deferred work

- KMP-04 owns search/library/player/playback modules; none were changed here.
- KMP-05 owns provider implementations; both existing Android provider graphs are green but providers were not migrated here.
- KMP-06 owns shared Compose/UI migration. No UI or previews changed.
- KMP-07 owns the device parity gate. No device campaign or Baseline Profile generation was required by KMP-03.
- WEB-01 owns the first Wasm target; no additional target was added.

No unrelated files were modified. The final committed status is recorded in the agent handoff.
