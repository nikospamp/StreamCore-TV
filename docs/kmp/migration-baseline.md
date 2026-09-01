# Android migration baseline

> **Status: Accepted.** Candidate `c2f90825bd336489f98361c8ba3a124800a51d04` passed the complete KMP-00H host and device gate with
> 214 passing host tests, 284 checked design-token source files, both provider graphs, and green phone/tablet/TV journeys. This is the frozen
> Android comparison baseline for KMP-01 through KMP-07.

## KMP-07 Phase 1 closure

> **Status: Pending authenticated TMDB TV completion and controlled physical-device campaign.** Production/test candidate `4c7cdaf` passes both debug/R8 providers, the full root
> gate, 265 host tests, 303 design-checked files, 21 Compose-KMP compiler tasks, benchmark/profile assembly, and 166 connected tests across the
> accepted phone/tablet/TV matrix. ClientB manual parity, authenticated TMDB phone/tablet parity, and the exact installed KMP-00
> auth/Search/Library/Playback upgrade are green. TMDB runtime configuration is present only in approved ignored local configuration and validated
> by a redacted linked-worktree preflight. TMDB TV awaits explicit emulator-login authorization; no Android 14+ physical device is connected for the
> required new controlled campaign. Phase 1 is not marked accepted and WEB-01 must not start until both gates pass.

KMP-07 preserves compile SDK 37 and target SDK 36 and adds no Wasm target. The generated Baseline Profiles are byte-for-byte unchanged and are
consumed by the release APK. The accepted physical-device campaign remains the before reference, not same-commit after evidence.

Accepted Android baseline evidence for the KMP migration. The target architecture is
backend-agnostic; core, domain, and feature UI code must remain independent of
provider SDKs, DTOs, API responses, and client-specific models.

## Baseline identity

| Item | Value |
|---|---|
| Status | `Accepted` |
| Date | 2026-09-01 (Europe/Athens) |
| Tested candidate commit | `c2f90825bd336489f98361c8ba3a124800a51d04` |
| Host remediation commit | `8bc11dc4300e55822bbae689dfdad271ef3fe769` |
| Planning checkpoint | `10276fe776e3ad616895ce7237c2c79ab47bcea9` |
| Branch | `codex/kmp-00-android-baseline` |
| Commit subject | `test(player): focus TV timeline before scrubbing` |
| Tested working tree | Clean (`git status --short` produced no output) |
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
| Connected devices | `Medium_Phone_API_36.1`, `Medium_Tablet`, and `Television_1080p`, run locally and sequentially except for the opaque session transfer |

KMP-00A through KMP-00G clear Login defaults, repair the design-token verifier, add the complete auth/logout lifecycle, restore tablet navigation and
details/player parity, restore TV profile deletion/details/player parity, and make TV return focus deterministic. The accepted candidate adds no
KMP/Koin/Wasm configuration, performance threshold, generated Baseline Profile, or provider coupling to the backend-agnostic feature contracts.

## Gradle module inventory

The inventory was derived from `settings.gradle.kts` and each included
module's applied plugin. There are 56 included modules.

| Classification | Count | Modules |
|---|---:|---|
| Android application | 1 | `:app` |
| Benchmark Android test | 1 | `:benchmark` |
| Baseline-profile Android test | 1 | `:baselineprofile` |
| JVM library | 10 | `:core:data`, `:core:domain`, `:feature:login:data`, `:feature:login:domain`, `:feature:profiles:data`, `:feature:profiles:domain`, `:feature:home:domain`, `:feature:search:domain`, `:feature:details:data`, `:feature:details:domain` |
| Android library | 43 | `:benchmark:ui-driver`, `:core:tracing`, `:core:ui`, `:client:tmdb:data`, `:client:tmdb:ui`, `:client:clientB:data`, `:client:clientB:ui`, `:feature:login:ui-common`, `:feature:login:ui-mobile`, `:feature:login:ui-tablet`, `:feature:login:ui-tv`, `:feature:profiles:ui-common`, `:feature:profiles:ui-mobile`, `:feature:profiles:ui-tablet`, `:feature:profiles:ui-tv`, `:feature:home:ui-common`, `:feature:home:ui-mobile`, `:feature:home:ui-tablet`, `:feature:home:ui-tv`, `:feature:search:data`, `:feature:search:ui-common`, `:feature:search:ui-mobile`, `:feature:search:ui-tablet`, `:feature:search:ui-tv`, `:feature:details:ui-common`, `:feature:details:ui-mobile`, `:feature:details:ui-tablet`, `:feature:details:ui-tv`, `:feature:library:data`, `:feature:library:domain`, `:feature:library:ui-common`, `:feature:library:ui-mobile`, `:feature:library:ui-tablet`, `:feature:library:ui-tv`, `:playback:api`, `:playback:media3`, `:feature:player:data`, `:feature:player:domain`, `:feature:player:ui-common`, `:feature:player:ui-mobile`, `:feature:player:ui-tv`, `:client:tmdb:player`, `:client:clientB:player` |

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
commit. It is authoritative only for the historical KMP-00 comparison. The root
`MODULE_DEPENDENCY_GRAPH.md` was superseded at that baseline commit, then replaced
by KMP-07 with the current post-migration graph extracted from the final build scripts.

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
  -> :feature:search:{data,ui-mobile,ui-tablet,ui-tv}
  -> :feature:details:{ui-mobile,ui-tablet,ui-tv}
  -> :feature:library:{data,ui-mobile,ui-tablet,ui-tv}
  -> :feature:player:{data,ui-mobile,ui-tv}
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
:feature:search:{ui-mobile,ui-tablet,ui-tv}
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
:feature:library:{ui-mobile,ui-tablet,ui-tv}
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
:feature:player:{ui-mobile,ui-tv}
  api -> :feature:player:ui-common
  -> :core:ui
```

Modules absent from this graph have no project-to-project dependency.

## Host build and test baseline

Commands were run independently from the repository root on 2026-09-01 at exact candidate commit
`c2f90825bd336489f98361c8ba3a124800a51d04`. Wrapper-only sandbox network denials were rerun with approved wrapper/dependency access and are not
Android baseline failures.

| Command | Result | Evidence |
|---|---|---|
| `.\gradlew.bat :app:assembleTmdbDebug` | Pass | `BUILD SUCCESSFUL`; 789 actionable tasks |
| `.\gradlew.bat :app:assembleClientBDebug` | Pass | `BUILD SUCCESSFUL`; 788 actionable tasks |
| `.\gradlew.bat :app:assembleTmdbReleaseR8` | Pass | `BUILD SUCCESSFUL`; 1,415 actionable tasks |
| `.\gradlew.bat :app:assembleClientBReleaseR8` | Pass | `BUILD SUCCESSFUL`; 1,414 actionable tasks |
| `.\gradlew.bat :app:compileTmdbDebugKotlin` | Pass | `BUILD SUCCESSFUL`; 366 actionable tasks |
| `.\gradlew.bat :app:compileClientBDebugKotlin` | Pass | `BUILD SUCCESSFUL`; 365 actionable tasks |
| `.\gradlew.bat :feature:login:ui-common:testDebugUnitTest --tests "*LoginViewModelTest"` | Pass | 5 tests; 0 failures/errors/skipped; 42 actionable tasks |
| `.\gradlew.bat verifyDesignTokensLogFiles --console=plain` | Pass | 284 production Kotlin files checked; zero violations; configuration cache reused |
| `.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain` | Pass | `BUILD SUCCESSFUL`; 1,921 actionable tasks; all unit tests and Android lint tasks green |
| `.\gradlew.bat :feature:home:ui-mobile:compileDebugAndroidTestKotlin` | Pass | `BUILD SUCCESSFUL`; 62 actionable tasks |
| `.\gradlew.bat :feature:home:ui-tablet:compileDebugAndroidTestKotlin` | Pass | `BUILD SUCCESSFUL`; 62 actionable tasks |
| `.\gradlew.bat :feature:login:ui-tv:compileDebugAndroidTestKotlin` | Pass | `BUILD SUCCESSFUL`; 45 actionable tasks |
| `.\gradlew.bat :feature:search:ui-tv:compileDebugAndroidTestKotlin` | Pass | `BUILD SUCCESSFUL`; 40 actionable tasks |
| `.\gradlew.bat :feature:search:ui-tablet:compileDebugAndroidTestKotlin` | Pass | `BUILD SUCCESSFUL` |
| `.\gradlew.bat :feature:library:ui-tablet:compileDebugAndroidTestKotlin` | Pass | `BUILD SUCCESSFUL` |
| `.\gradlew.bat :feature:player:ui-tv:compileDebugAndroidTestKotlin` | Pass | `BUILD SUCCESSFUL` |
| `.\gradlew.bat :benchmark:assemble` | Pass | `BUILD SUCCESSFUL`; 98 actionable tasks |
| `.\gradlew.bat :baselineprofile:assemble` | Pass | `BUILD SUCCESSFUL`; 160 actionable tasks; profiles not regenerated |

### Completed host remediations

- `LoginUiState` production identifier/password defaults are empty. Sample values remain explicit preview/test arguments, and the original invalid
  submit test is unchanged and green.
- `verifyDesignTokens` now roots file trees at explicit `src/main/kotlin`, Kotlin-under-`src/main/java`, `commonMain`, `androidMain`, and `wasmJsMain`
  production source sets for `app`/`core`/`feature`, excluding build/generated/test trees. Gradle 9 input validation and configuration-cache reuse pass.
- KMP-00A classified 96 pre-edit violations semantically and replaced them without changing numeric/color values or adding suppressions, allow-lists,
  exclusions, or behavior changes. Missing roles live only in approved token files; shape/color reads are stable and allocation-safe.
- The verifier's authoritative checked count is **284**, an increase of 18 attributable to the expected tablet Search/Library/navigation, TV
  player/profile/focus, and supporting app-shell production files added by KMP-00B through KMP-00G. Representative checked paths include
  `app/src/main/java/com/pampoukidis/streamcoretv/navigation/StreamCoreNavHost.kt`,
  `core/ui/src/main/kotlin/com/pampoukidis/streamcoretv/core/ui/components/StreamCorePagerCarousel.kt`, and
  `feature/player/ui-mobile/src/main/kotlin/com/pampoukidis/streamcoretv/feature/player/mobile/player/MobilePlayerScreen.kt`.

### Green host test inventory

Counts were read from the XML reports produced by the committed root `check`. The original inventory accidentally included one stale generated
XML test from the non-included `:feature:player:ui-tablet` module. That module was absent from `settings.gradle.kts`, so Gradle did not execute its
test task for the accepted candidate; the stale row is excluded from the corrected accepted total below.

| Module | Task | Tests | Failures | Errors | Skipped |
|---|---|---:|---:|---:|---:|
| `:app` | `testClientBDebugUnitTest` | 23 | 0 | 0 | 0 |
| `:app` | `testTmdbDebugUnitTest` | 23 | 0 | 0 | 0 |
| `:client:clientB:data` | `testDebugUnitTest` | 25 | 0 | 0 | 0 |
| `:client:clientB:ui` | `testDebugUnitTest` | 3 | 0 | 0 | 0 |
| `:client:tmdb:data` | `testDebugUnitTest` | 38 | 0 | 0 | 0 |
| `:client:tmdb:ui` | `testDebugUnitTest` | 4 | 0 | 0 | 0 |
| `:feature:details:domain` | `test` | 2 | 0 | 0 | 0 |
| `:feature:details:ui-common` | `testDebugUnitTest` | 14 | 0 | 0 | 0 |
| `:feature:home:domain` | `test` | 1 | 0 | 0 | 0 |
| `:feature:home:ui-common` | `testDebugUnitTest` | 14 | 0 | 0 | 0 |
| `:feature:library:data` | `testDebugUnitTest` | 5 | 0 | 0 | 0 |
| `:feature:library:domain` | `testDebugUnitTest` | 4 | 0 | 0 | 0 |
| `:feature:library:ui-common` | `testDebugUnitTest` | 3 | 0 | 0 | 0 |
| `:feature:login:domain` | `test` | 3 | 0 | 0 | 0 |
| `:feature:login:ui-common` | `testDebugUnitTest` | 5 | 0 | 0 | 0 |
| `:feature:player:data` | `testDebugUnitTest` | 3 | 0 | 0 | 0 |
| `:feature:player:ui-common` | `testDebugUnitTest` | 11 | 0 | 0 | 0 |
| `:feature:player:ui-mobile` | `testDebugUnitTest` | 12 | 0 | 0 | 0 |
| `:feature:player:ui-tv` | `testDebugUnitTest` | 2 | 0 | 0 | 0 |
| `:feature:profiles:ui-common` | `testDebugUnitTest` | 3 | 0 | 0 | 0 |
| `:feature:search:data` | `testDebugUnitTest` | 2 | 0 | 0 | 0 |
| `:feature:search:domain` | `test` | 2 | 0 | 0 | 0 |
| `:feature:search:ui-common` | `testDebugUnitTest` | 12 | 0 | 0 | 0 |
| **Total** |  | **214** | **0** | **0** | **0** |

## Device smoke baseline

The matrix ran locally against candidate `c2f90825bd336489f98361c8ba3a124800a51d04`. Serial values are scoped to each isolated form-factor run;
the phone temporarily used `emulator-5556` while its explicitly authorized opaque 58-byte TMDB session DataStore was streamed directly into the TV
app sandbox. The payload was never decoded, printed, logged, or written to the host. TMDB credential submission is covered on phone, TMDB restored
session and post-auth behavior are covered on every form factor, and the authorized generated ClientB fixture was submitted on phone/tablet/TV.

| AVD | Serial | API | Resolution | Orientation | Variants | Tested commit |
|---|---|---:|---:|---|---|---|
| `Medium_Phone_API_36.1` | `emulator-5554` | 36.1 | 1080x2400 | Portrait; player 2400x1080 landscape | TMDB + ClientB debug | `c2f90825` |
| `Medium_Tablet` | `emulator-5554` | 36.1 | 2560x1600 | Landscape | TMDB + ClientB debug | `c2f90825` |
| `Television_1080p` | `emulator-5554` | 31 | 1920x1080 | Landscape | TMDB + ClientB debug | `c2f90825` |

| Surface/journey | Mobile | Tablet | TV | Outcome |
|---|---|---|---|---|
| Login, logout, session restoration | Pass | Pass | Pass | Confirmation, cancel/Back restoration, reset, login-again, and restored-session paths green |
| Profile selection, creation, editing, deletion | Pass | Pass | Pass | Eligible-only delete and nearest-profile/Add fallback covered; protected profile cannot delete |
| Home loading, refresh, navigation | Pass | Pass | Pass | Adaptive tablet rail and TV drawer navigation green |
| Search discovery, query, recents, result selection | Pass | Pass | Pass | Result selection/state and return-focus coverage green |
| Library states, details mutations | Pass | Pass | Pass | Empty/content/mutation/profile-scoped states green |
| Details, trailer, recommendations, back | Pass | Pass | Pass | Trailer intent/actions, recommendations, mutations, and Back green |
| Player start, seek, pause/resume, settings, exit, restoration | Pass | Pass | Pass | Phone 7/7 and TV 9/9 connected suites; manual progress/resume/seek/exit green |
| D-pad focus traversal and restoration | N/A | N/A | Pass | Drawer Left/Right/Back, profile/delete dialogs, timeline and exact-item return focus green |

ClientB boot/auth/profile/Home/Details/player smoke passed on phone/tablet/TV, including phone and TV logout/login-again. All applicable journeys pass;
there is no expected-failure, Blocked, or `Not run` result.

Local ignored evidence is under `build/kmp-00-evidence/` and `build/kmp-00h-evidence/`. Current connected reports include phone player 7/7, TV Home
9/9, Profiles 18/18, Player 9/9, and green Search/Library/Details/Login suites. The evidence remains local and is not committed as production input.

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
.\gradlew.bat check -PverifyDesignTokensLogFiles=true
.\gradlew.bat testAndroidHostTest
.\gradlew.bat lint
.\gradlew.bat :feature:home:ui-mobile:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:home:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:login:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:search:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :benchmark:assemble
.\gradlew.bat :baselineprofile:assemble
rg -n "dagger\.|hilt|javax\.inject|dagger-hilt|hilt-android" app core client feature playback build.gradle.kts settings.gradle.kts gradle/libs.versions.toml -g "*.kt" -g "*.kts" -g "*.toml"
```

Expected comparison:

- Every command must pass. KMP-00 has no expected-failure allow-list.
- `testAndroidHostTest` replaces migrated JVM/Android unit-test tasks. Compare executed test-case counts by owning module/feature against the accepted
  KMP-00 table; task-name changes do not excuse missing tests.
- `verifyDesignTokens` must report migrated source-set files and a nonzero checked count.
- Repeat the complete device smoke table on equivalent form factors; every provisional functional blocker above must be fixed before acceptance.
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

## Accepted limitations and follow-up context

- Host remediation and KMP-00B through KMP-00G are complete; there is no expected-failure allow-list.
- The combined TMDB-session/ClientB-login credential-handling contract is documented evidence, not production behavior or a provider abstraction.
- Accepted controlled performance evidence predates the baseline commit; it is
  retained as contextual evidence, not same-commit proof.
- The Baseline Profile `Require` run is partial and non-reportable.
- At the KMP-00 commit, `MODULE_DEPENDENCY_GRAPH.md` was stale. KMP-07 replaces it with the current graph; this section remains the immutable
  historical comparison snapshot.
- KMP-01 must start only from the `codex/kmp-migration` branch created by the KMP-00H documentation acceptance commit.
