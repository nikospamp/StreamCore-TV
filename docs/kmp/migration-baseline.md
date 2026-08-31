# Android migration baseline

> **Status: PROVISIONAL — KMP-01 is blocked.** The initial pass at commit `2dcd325c2c9c27ec45ca170783f03af007c77e4a`
> found two host blockers and captured no device evidence. KMP-00 is reopened. This report becomes canonical only after its status is changed to
> `Accepted`, the remediation commit is recorded, root `check` is green with test/design-token counts, and mobile/tablet/TV results replace every
> `Not run` entry.

Provisional Android baseline evidence for the KMP migration. The target architecture is
backend-agnostic; core, domain, and feature UI code must remain independent of
provider SDKs, DTOs, API responses, and client-specific models.

## Baseline identity

| Item | Value |
|---|---|
| Status | `PROVISIONAL` — do not start KMP-01 |
| Date | 2026-08-31 (Europe/Athens) |
| Commit | `2dcd325c2c9c27ec45ca170783f03af007c77e4a` |
| Branch | `master` |
| Commit subject | `feature: Start kpm migration.` |
| Initial working tree | Clean (`git status --short` produced no output) |
| Host | Windows 11 10.0 amd64 |
| Gradle | 9.3.1 (`gradle-wrapper.properties`) |
| Android Gradle Plugin | 9.1.1 |
| Project Kotlin plugin | 2.3.21 |
| Gradle embedded Kotlin | 2.2.21 |
| JDK | JetBrains Runtime 21.0.11 |
| JDK path | `C:\Users\Nikos\.jdks\jbr-21.0.11` |
| Android SDK | `C:\Users\Nikos\AppData\Local\Android\Sdk` |
| ADB | 1.0.41 / platform-tools 37.0.1 |
| Compile SDK | Android 36, minor API 1 for the app/profile producer; 36 for benchmark |
| App min/target SDK | 26 / 36 |
| Benchmark/profile min/target SDK | 29 / 36 |
| Java source/target compatibility | 11 |
| Connected devices | None (`adb devices -l` returned an empty device list) |

The initial pass changed only this report; Gradle outputs/test reports remain ignored. Reopened KMP-00 is narrowly authorized to clear production
Login credential defaults and repair design-token task input construction. The final accepted report must replace the commit/device rows above and
describe those two remediations; no dependency, source-set, threshold, generated Baseline Profile, or unrelated behavior change is authorized.

## Gradle module inventory

The inventory was derived from `settings.gradle.kts` and each included
module's applied plugin. There are 53 included modules.

| Classification | Count | Modules |
|---|---:|---|
| Android application | 1 | `:app` |
| Benchmark Android test | 1 | `:benchmark` |
| Baseline-profile Android test | 1 | `:baselineprofile` |
| JVM library | 10 | `:core:data`, `:core:domain`, `:feature:login:data`, `:feature:login:domain`, `:feature:profiles:data`, `:feature:profiles:domain`, `:feature:home:domain`, `:feature:search:domain`, `:feature:details:data`, `:feature:details:domain` |
| Android library | 40 | `:benchmark:ui-driver`, `:core:tracing`, `:core:ui`, `:client:tmdb:data`, `:client:tmdb:ui`, `:client:clientB:data`, `:client:clientB:ui`, `:feature:login:ui-common`, `:feature:login:ui-mobile`, `:feature:login:ui-tablet`, `:feature:login:ui-tv`, `:feature:profiles:ui-common`, `:feature:profiles:ui-mobile`, `:feature:profiles:ui-tablet`, `:feature:profiles:ui-tv`, `:feature:home:ui-common`, `:feature:home:ui-mobile`, `:feature:home:ui-tablet`, `:feature:home:ui-tv`, `:feature:search:data`, `:feature:search:ui-common`, `:feature:search:ui-mobile`, `:feature:search:ui-tv`, `:feature:details:ui-common`, `:feature:details:ui-mobile`, `:feature:details:ui-tablet`, `:feature:details:ui-tv`, `:feature:library:data`, `:feature:library:domain`, `:feature:library:ui-common`, `:feature:library:ui-mobile`, `:feature:library:ui-tv`, `:playback:api`, `:playback:media3`, `:feature:player:data`, `:feature:player:domain`, `:feature:player:ui-common`, `:feature:player:ui-mobile`, `:client:tmdb:player`, `:client:clientB:player` |

There are no standalone generic Android-test modules beyond `:benchmark` and
`:baselineprofile`.

## Provider and variant wiring

The app has one `client` flavor dimension:

| Flavor | Flavor-only app dependencies |
|---|---|
| `tmdb` | `:client:tmdb:data`, `:client:tmdb:ui`, `:client:tmdb:player` |
| `clientB` | `:client:clientB:data`, `:client:clientB:ui`, `:client:clientB:player` |

Both flavors use the same backend-agnostic core, feature, and playback API
modules. Provider data modules implement the common repository contracts and
provider player modules adapt `:playback:api` through `:playback:media3`.

The app defines `debug`, `release`, `releaseR8`, `benchmark`, `benchmarkR8`,
and `profile` build types. `releaseR8` is the isolated minified/resource-shrunk
rollout candidate. `benchmark` is release-derived, non-debuggable, debug-signed,
and has application ID suffix `.benchmark`; `benchmarkR8` adds shrinking.
`profile` is the isolated profile-generation target. The Baseline Profile
consumer imports only the `tmdbProfile` producer variant and merges it into the
app main source set. The benchmark and Baseline Profile producer modules target
only `tmdb`.

## Direct project dependency graph

This snapshot was extracted from the module build scripts at the baseline
commit. It is authoritative for KMP-00; the older root
`MODULE_DEPENDENCY_GRAPH.md` uses superseded module names and is not a reliable
migration gate.

`api` edges expose a contract transitively. Unlabelled edges below are
`implementation` unless another configuration is shown.

```text
:app
  baselineProfile from -> :baselineprofile
  tmdbImplementation -> :client:tmdb:{data,ui,player}
  clientBImplementation -> :client:clientB:{data,ui,player}
  -> :core:{data,domain,ui}
  -> :feature:login:{ui-mobile,ui-tablet,ui-tv}
  -> :feature:profiles:{ui-mobile,ui-tablet,ui-tv}
  -> :feature:home:{ui-mobile,ui-tablet,ui-tv}
  -> :feature:search:{data,ui-mobile,ui-tv}
  -> :feature:details:{ui-mobile,ui-tablet,ui-tv}
  -> :feature:library:{data,ui-mobile,ui-tv}
  -> :feature:player:{data,ui-mobile}
  -> :playback:api

:benchmark -> :benchmark:ui-driver
:baselineprofile -> :benchmark:ui-driver
:core:domain -> :core:data
:core:ui -> :core:data

:client:tmdb:data -> :core:{data,domain}, :feature:search:domain
:client:tmdb:ui -> :core:{data,ui}
:client:tmdb:player -> :playback:{api,media3}
:client:clientB:data -> :core:{data,domain}, :feature:search:domain
:client:clientB:ui -> :core:{data,ui}
:client:clientB:player -> :playback:{api,media3}

:feature:login:domain -> :core:{data,domain}, :feature:login:data
:feature:login:ui-common
  api -> :feature:login:{data,domain}
  -> :core:{data,domain,ui}
:feature:login:{ui-mobile,ui-tablet,ui-tv}
  api -> :feature:login:ui-common
  -> :core:{data,ui}

:feature:profiles:domain -> :core:{data,domain}, :feature:profiles:data
:feature:profiles:ui-common
  api -> :feature:profiles:{data,domain}
  -> :core:{data,domain,ui}
:feature:profiles:{ui-mobile,ui-tablet,ui-tv}
  api -> :feature:profiles:ui-common
  -> :core:{data,ui}

:feature:home:domain -> :core:{data,domain}
:feature:home:ui-common
  api -> :feature:home:domain
  -> :core:{data,domain}, :playback:api
:feature:home:{ui-mobile,ui-tablet,ui-tv}
  api -> :feature:home:ui-common
  -> :core:{data,ui}

:feature:search:data -> :feature:search:domain
:feature:search:domain -> :core:data
:feature:search:ui-common
  api -> :feature:search:domain
  -> :core:data
:feature:search:{ui-mobile,ui-tv}
  api -> :feature:search:ui-common
  -> :core:{data,ui}

:feature:details:data -> :core:data
:feature:details:domain -> :core:{data,domain}, :feature:details:data
:feature:details:ui-common
  api -> :feature:details:{data,domain}, :playback:api
  -> :core:{data,domain}, :feature:library:domain, :feature:player:domain
:feature:details:{ui-mobile,ui-tablet,ui-tv}
  api -> :feature:details:ui-common
  -> :core:{data,ui}

:feature:library:data -> :core:{data,domain}
:feature:library:domain
  api -> :core:{data,domain}, :playback:api
:feature:library:ui-common
  api -> :feature:library:domain
  -> :core:data
:feature:library:{ui-mobile,ui-tv}
  api -> :feature:library:ui-common
  -> :core:{data,ui}

:playback:api
  api -> :core:data
:playback:media3
  api -> :playback:api
:feature:player:data
  api -> :playback:api
:feature:player:domain
  api -> :playback:api
:feature:player:ui-common
  api -> :feature:player:domain, :playback:api
:feature:player:ui-mobile
  api -> :feature:player:ui-common
  -> :core:ui
```

Modules absent from this graph have no project-to-project dependency.

## Host build and test baseline

Commands were run from the repository root on 2026-08-31. The first attempted
Client B assembly was blocked before Gradle startup by the execution sandbox's
network policy while locating the wrapper distribution; it was rerun with the
same command after granting wrapper/dependency access. That infrastructure-only
attempt is not an Android baseline failure.

| Command | Result | Evidence |
|---|---|---|
| `.\gradlew.bat :app:assembleTmdbDebug` | Pass | `BUILD SUCCESSFUL`; 789 actionable tasks |
| `.\gradlew.bat :app:assembleClientBDebug` | Pass | `BUILD SUCCESSFUL`; 788 actionable tasks |
| `.\gradlew.bat :app:compileTmdbDebugKotlin` | Pass | `BUILD SUCCESSFUL`; 366 actionable tasks |
| `.\gradlew.bat :app:compileClientBDebugKotlin` | Pass | `BUILD SUCCESSFUL`; 365 actionable tasks |
| `.\gradlew.bat test` | **Fail** | `:feature:login:ui-common:testDebugUnitTest`; 913 actionable tasks |
| `.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain` | **Fail** | Exactly two failures: the Login test and Gradle 9 input validation for `:verifyDesignTokens`; all lint tasks completed successfully |
| `.\gradlew.bat :feature:home:ui-mobile:compileDebugAndroidTestKotlin` | Pass | `BUILD SUCCESSFUL`; 62 actionable tasks |
| `.\gradlew.bat :feature:home:ui-tablet:compileDebugAndroidTestKotlin` | Pass | `BUILD SUCCESSFUL`; 62 actionable tasks |
| `.\gradlew.bat :feature:login:ui-tv:compileDebugKotlin` | Pass | `BUILD SUCCESSFUL`; 37 actionable tasks |
| `.\gradlew.bat :benchmark:assemble` | Pass | `BUILD SUCCESSFUL`; 98 actionable tasks |
| `.\gradlew.bat :baselineprofile:assemble` | Pass | `BUILD SUCCESSFUL`; 160 actionable tasks |

### Provisional baseline host blockers

The Login failure is a real baseline defect, not an accepted expected failure. `LoginUiState` hardcodes non-empty production identifier/password
defaults, so an empty Submit uses valid credentials and cannot produce the required validation errors. KMP-00 must clear production defaults and
keep sample values only in previews/tests; the test must not be weakened.

```text
:feature:login:ui-common:testDebugUnitTest
LoginViewModelTest > invalid submit surfaces validation errors FAILED
LoginViewModelTest.kt:49
expected: LoginFieldError.Required
actual: null
```

Focused reproduction:

```powershell
.\gradlew.bat :feature:login:ui-common:testDebugUnitTest --tests "*LoginViewModelTest"
```

The expanded gate exposed a second blocker before the verifier action could run:

```text
:verifyDesignTokens
Gradle 9 implicit-dependency validation rejects broad FileTree roots at core/, feature/, and app/
because those roots overlap subproject build outputs.
```

KMP-00 must pass an explicit provider/set of individual production Kotlin source files, include Kotlin files under `app/src/main/java`, and exclude
all build/generated/test trees. The original verifier scope manually resolves to 255 checked files after its five allow-listed token files; this is
diagnostic only, not the accepted checked-file count because the task did not execute. The remediated task's logged count is authoritative.

All Android lint tasks completed successfully under the expanded `check --continue` run. There is no separate known lint baseline failure.

### Provisional host test inventory

These counts were recovered from included modules' XML reports after the expanded gate. The accepted remediation run must preserve the same test
cases with zero failures; generated reports from non-included stale modules are excluded.

| Module | Task | Tests | Failures | Errors | Skipped |
|---|---|---:|---:|---:|---:|
| `:app` | `testClientBDebugUnitTest` | 15 | 0 | 0 | 0 |
| `:app` | `testTmdbDebugUnitTest` | 15 | 0 | 0 | 0 |
| `:client:clientB:data` | `testDebugUnitTest` | 20 | 0 | 0 | 0 |
| `:client:clientB:ui` | `testDebugUnitTest` | 3 | 0 | 0 | 0 |
| `:client:tmdb:data` | `testDebugUnitTest` | 36 | 0 | 0 | 0 |
| `:client:tmdb:ui` | `testDebugUnitTest` | 4 | 0 | 0 | 0 |
| `:feature:details:domain` | `test` | 2 | 0 | 0 | 0 |
| `:feature:details:ui-common` | `testDebugUnitTest` | 14 | 0 | 0 | 0 |
| `:feature:home:domain` | `test` | 1 | 0 | 0 | 0 |
| `:feature:home:ui-common` | `testDebugUnitTest` | 14 | 0 | 0 | 0 |
| `:feature:library:data` | `testDebugUnitTest` | 5 | 0 | 0 | 0 |
| `:feature:library:domain` | `testDebugUnitTest` | 4 | 0 | 0 | 0 |
| `:feature:library:ui-common` | `testDebugUnitTest` | 3 | 0 | 0 | 0 |
| `:feature:login:domain` | `test` | 3 | 0 | 0 | 0 |
| `:feature:login:ui-common` | `testDebugUnitTest` | 5 | 1 | 0 | 0 |
| `:feature:player:data` | `testDebugUnitTest` | 3 | 0 | 0 | 0 |
| `:feature:player:ui-common` | `testDebugUnitTest` | 10 | 0 | 0 | 0 |
| `:feature:player:ui-mobile` | `testDebugUnitTest` | 12 | 0 | 0 | 0 |
| `:feature:search:data` | `testDebugUnitTest` | 2 | 0 | 0 | 0 |
| `:feature:search:domain` | `test` | 2 | 0 | 0 | 0 |
| `:feature:search:ui-common` | `testDebugUnitTest` | 12 | 0 | 0 | 0 |
| **Total** |  | **185** | **1** | **0** | **0** |

## Device smoke baseline

No Android device or emulator was connected during KMP-00. Consequently, no
new behavior was claimed from APK assembly alone. These are explicitly
uncovered baseline checks, not passes and not known behavior failures.

| Surface/journey | Mobile | Tablet | TV | Outcome |
|---|---|---|---|---|
| Login, logout, session restoration | Not run | Not run | Not run | No connected device |
| Profile selection, creation, editing, deletion | Not run | Not run | Not run | No connected device |
| Home loading, refresh, navigation | Not run | Not run | Not run | No connected device |
| Search discovery, query, recents, result selection | Not run | Not run | Not run | No connected device |
| Library states, details mutations | Not run | Not run | Not run | No connected device |
| Details, trailer, recommendations, back | Not run | Not run | Not run | No connected device |
| Player start, seek, pause/resume, settings, exit, restoration | Not run | Not run | Not run | No connected device |
| D-pad focus traversal and restoration | N/A | N/A | Not run | No connected TV/emulator |

This absence is blocking rather than an accepted limitation. Before KMP-01, reopened KMP-00 must run the reference matrix on identified mobile,
tablet, and Android TV targets at the remediated baseline commit. Emulator evidence is acceptable. The report must also include at least
boot/auth/profile/home/details/player provider smoke for ClientB. KMP-07 remains a parity gate only because KMP-00 will supply this evidence.

## Existing performance and Baseline Profile evidence

No new device campaign or profile generation was run. Existing evidence was
recorded without changing thresholds.

### Accepted controlled campaign

The accepted result is
[`docs/performance/samsung-controlled-v2-final.md`](../performance/samsung-controlled-v2-final.md),
with raw evidence under
`benchmark-results/navigation-20260829-samsung-controlled-v2/`.

- Evidence commit: `82697b1a5aac65247cb52468f16dd7dbb77f36f7`
  (predates the KMP-00 baseline commit).
- Device: Samsung Galaxy S23 Ultra (`SM-S918B`), Android 16 / API 36,
  approximately 120 Hz.
- Dataset: profile `Nikos`,
  `home:content:continue-watching:1516698`.
- Coverage: 320/320 journeys, 32/32 cells, ten iterations per cell.
- Builds: non-debuggable `benchmark` and R8/resource-shrunk `benchmarkR8`.
- Compilation: `None` versus warm-up `Partial` with
  `BaselineProfileMode.Disable`.
- Unminified APK SHA-256:
  `2EFB344BEC94A2EFA65F51C60DEB7C1267A0C4CD38E8F58623886484598C7461`.
- R8 APK SHA-256:
  `DB6C77092091D1E36C1BA57DF61A5BE7926D39770BCC98F78EDEF0A9C40C8CCE`.
- Driver SHA-256:
  `6A3B9FC39E7D07224A2FEA9148761C2CB0163C805BFD40620D493B85FDF0FF4C`.

Frame CPU P95 in milliseconds:

| Journey | Entry | Unminified None | Unminified Partial | R8 None | R8 Partial |
|---|---|---:|---:|---:|---:|
| Profiles to Home | First | 25.34 | 12.84 | 17.71 | 16.27 |
| Profiles to Home | Repeated | 24.07 | 10.77 | 16.44 | 11.23 |
| Home to Details | First | 34.08 | 15.57 | 20.16 | 14.91 |
| Home to Details | Repeated | 28.84 | 13.04 | 20.94 | 11.15 |
| Player to Details | First | 23.14 | 15.16 | 16.60 | 13.27 |
| Player to Details | Repeated | 23.03 | 12.69 | 13.81 | 12.54 |
| Initial Search | First | 29.84 | 12.32 | 17.30 | 12.12 |
| Initial Search | Repeated | 33.63 | 13.83 | 25.31 | 17.83 |

Warm-up Partial reduced unminified P95 by 34-59% and R8 P95 by 8-47% across
the eight journey/entry pairs. R8 reduced uncompiled P95 by 25-42%. These are
observations, not KMP pass/fail thresholds.

### Shipped generated profiles

| File | Size | Lines | SHA-256 |
|---|---:|---:|---|
| `app/src/main/generated/baselineProfiles/baseline-prof.txt` | 4,512,603 bytes | 39,774 | `E83C0A0E3019B551E8595D168A356FBFBA8F400DFC9F9E82AA07FFCDEB3648DE` |
| `app/src/main/generated/baselineProfiles/startup-prof.txt` | 2,274,211 bytes | 22,529 | `1ACE4DEAFDDAC2F2D9C5B9ED303209085DCD0D1C8271E21E02C8B1086A1DA372` |

`:baselineprofile:assemble` passes, but profile generation was not rerun.

### Invalid partial diagnostic

`benchmark-results/navigation-20260830-baseline-profile-r8/` is an interrupted
`BaselineProfileMode.Require` diagnostic at evidence commit
`82697b1a5aac65247cb52468f16dd7dbb77f36f7`. It contains 53 traces, five
complete cells, one 3/10 cell, and two missing Search cells. It is statistically
invalid as a campaign and must not be pooled, compared as a treatment result,
or used as a KMP threshold. Its `PARTIAL.md` marker is authoritative.

## KMP-07 repeatable gate

Start from a clean tree and record the candidate commit, toolchain paths,
`adb devices -l`, and device fingerprints. Run these commands independently
from the repository root so the first failure remains attributable:

```powershell
.\gradlew.bat :app:assembleTmdbDebug
.\gradlew.bat :app:assembleClientBDebug
.\gradlew.bat :app:assembleTmdbReleaseR8
.\gradlew.bat :app:assembleClientBReleaseR8
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain
.\gradlew.bat testAndroidHostTest
.\gradlew.bat lint
.\gradlew.bat :feature:home:ui-mobile:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:home:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:login:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:search:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :benchmark:assemble
.\gradlew.bat :baselineprofile:assemble
```

Expected comparison:

- Every command must pass. The accepted KMP-00 remediation baseline has no expected-failure allow-list.
- `testAndroidHostTest` replaces migrated JVM/Android unit-test tasks. Compare executed test-case counts by owning module/feature against the accepted
  KMP-00 table; task-name changes do not excuse missing tests.
- `verifyDesignTokens` must report migrated source-set files and a nonzero checked count.
- Repeat the complete device smoke table on equivalent form factors and compare results with the accepted KMP-00 evidence.
- Do not regenerate profiles merely to run the gate.

When a controlled performance comparison is required, use the repository
protocol in
[`docs/performance/mobile-navigation.md`](../performance/mobile-navigation.md).
Use an Android 14+ physical device, authenticate the isolated benchmark app,
pin one exact content tag after preflight, and keep the device unlocked and
unused:

```powershell
.\tools\performance\run-navigation.ps1 -Serial <device> -Phase Preflight -RunId <unique-preflight>
.\tools\performance\run-campaign.ps1 -Serial <device> -ContentTag '<pinned-tag>' -RunPrefix <unique-campaign>
```

Do not overwrite or append to the accepted campaign. A new driver, APK hash,
dataset, device, or commit requires a new run identity. Performance remains an
evidence comparison, not an acceptance threshold, unless a later ticket
explicitly approves thresholds.

## Current provisional blockers and durable limitations

- Provisional blocker: the Login default-credential defect must be fixed by reopened KMP-00.
- Provisional blocker: `verifyDesignTokens` broad FileTree inputs must be repaired and counted by reopened KMP-00.
- Provisional blocker: mobile/tablet/TV device evidence must replace every `Not run` result before KMP-01.
- Accepted controlled performance evidence predates the baseline commit; it is
  retained as contextual evidence, not same-commit proof.
- The Baseline Profile `Require` run is partial and non-reportable.
- `MODULE_DEPENDENCY_GRAPH.md` is stale; use this report and the build scripts.
- Only the two narrowly authorized baseline remediations may change production/build configuration before acceptance.
