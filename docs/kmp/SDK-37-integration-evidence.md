# SDK 37 integration evidence

## Decision and scope

- Branch: `codex/kmp-sdk37-integration`.
- Base: `175c38ae5d598c58c4f230b85a780dc4a8b293c5`.
- Verification date: 2026-09-01 (Europe/Athens).
- Target architecture: backend-agnostic Android-only KMP Phase 1; no web or Wasm target was added.
- Compose Multiplatform 1.12 Android artifacts require consumers to compile against API 37 or later. The locked AGP 9.1.1 toolchain supports API
  37, and the existing local SDK provides `platforms/android-37.0` and build-tools 37.0.0.
- This is a compile-only integration prerequisite. It is not a dependency downgrade and changes no production Kotlin, resources, build types,
  flavors, min SDK, or runtime targeting.
- `targetSdk = 36` remains unchanged in `:app`, `:benchmark`, and `:baselineprofile`.

## Changed build files

The KMP convention now assigns `androidTarget.compileSdk = 37` in
`build-logic/src/main/kotlin/StreamCoreKmpLibraryPlugin.kt`. Existing core and KMP-03 modules inherit that value without local Android target or
compile SDK declarations.

The following 40 Android-only application, library, and test module build files now use `compileSdk = 37`:

- Application/test infrastructure: `:app`, `:benchmark`, `:benchmark:ui-driver`, `:baselineprofile`.
- Core Android: `:core:tracing`, `:core:ui`.
- Client/provider Android: `:client:tmdb:{data,player,ui}`, `:client:clientB:{data,player,ui}`.
- Details UI: `:feature:details:{ui-common,ui-mobile,ui-tablet,ui-tv}`.
- Home UI: `:feature:home:{ui-common,ui-mobile,ui-tablet,ui-tv}`.
- Library UI: `:feature:library:{ui-common,ui-mobile,ui-tablet,ui-tv}`.
- Login UI: `:feature:login:{ui-common,ui-mobile,ui-tablet,ui-tv}`.
- Player UI: `:feature:player:{ui-common,ui-mobile,ui-tv}`.
- Profiles UI: `:feature:profiles:{ui-common,ui-mobile,ui-tablet,ui-tv}`.
- Search UI: `:feature:search:{ui-common,ui-mobile,ui-tablet,ui-tv}`.
- Android playback implementation: `:playback:media3`.

KMP-04 actively owns these files, so this branch intentionally did not edit them:
`feature/search/data/build.gradle.kts`, `feature/search/domain/build.gradle.kts`,
`feature/library/data/build.gradle.kts`, `feature/library/domain/build.gradle.kts`,
`feature/player/data/build.gradle.kts`, `feature/player/domain/build.gradle.kts`, and
`playback/api/build.gradle.kts`. Six currently declare compile SDK 36; Search domain is still a JVM module. KMP-04 converts these modules to the
API-37 KMP convention during integration, avoiding parallel merge conflicts.

Documentation changes are limited to `AGENTS.md`, `docs/kmp/build-conventions.md`, and this evidence file.

## Verification

All Gradle commands used the installed SDK 37 and Android Studio JBR. Artifact-producing commands that can exceed the repository's default 2 GiB
heap ran serially with the temporary command-line override
`--no-parallel '-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8'`; no repository memory setting changed.

| Command | Result |
|---|---|
| `.\gradlew.bat help --console=plain` | Pass; focused configuration/sync check; 5 actionable tasks |
| `.\gradlew.bat :app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin --console=plain` | Pass; both provider graphs; 401 actionable tasks |
| `.\gradlew.bat :app:assembleClientBDebug --no-parallel ... --console=plain` | Pass; 847 actionable tasks |
| `.\gradlew.bat :app:assembleTmdbDebug --no-parallel ... --console=plain` | Pass; 849 actionable tasks |
| `.\gradlew.bat :app:compileTmdbBenchmarkKotlin :app:compileTmdbBenchmarkR8Kotlin :app:compileTmdbProfileKotlin :benchmark:assemble :baselineprofile:assemble --no-parallel ... --console=plain` | Pass; 1,273 actionable tasks; profiles were not regenerated |
| `.\gradlew.bat verifyDesignTokensLogFiles verifyKmpTestTargets verifyKmpAndroidCompilerFlags verifyKmpDependencyCompatibility verifyKmpConventionPlugins :kmp-convention-fixtures:plain:compileAndroidMain :kmp-convention-fixtures:compose:compileAndroidMain --no-parallel ... --console=plain` | Pass; 11 actionable tasks |
| `.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --no-parallel ... --console=plain` | Integration-order limitation; 1,932 actionable tasks, 980 executed, 172 from cache, 780 up-to-date; tests and guards ran, then six KMP-04-owned API-36 modules failed AAR metadata checks against API-37 core artifacts |

The initial combined `:app:assembleTmdbDebug :app:assembleClientBDebug` attempt exhausted the default 2 GiB Gradle heap in D8 while merging
ClientB external dex archives. Both flavors passed when retried serially with a temporary 4 GiB heap. This was a local memory limit, not an API-37
or toolchain compatibility failure.

Focused guard results:

- `verifyDesignTokensLogFiles`: 298 production Kotlin files, zero violations.
- `verifyKmpTestTargets`: six common-test modules and six Android host-test targets; `:core:domain` remains the explicit compile-only exemption.
- `verifyKmpDependencyCompatibility`: 14 locked coordinates resolved for both common metadata and Android (14/12 artifact files).
- `verifyKmpConventionPlugins`: two convention-only fixtures received the registered API-37 Android target and expected variants/tasks.
- `verifyKmpAndroidCompilerFlags`: zero production Compose-KMP compile tasks, as expected until KMP-06.

## Executed tests

The continued root check produced 59 JUnit XML suite files containing 247 tests, with zero failures, errors, or skips. This exactly matches the
KMP-03 repository baseline, so the SDK-only integration has a zero test-count delta. No expected test task executed zero tests.

| Module | Task | Tests |
|---|---|---:|
| `:app` | `testClientBDebugUnitTest` | 25 |
| `:app` | `testTmdbDebugUnitTest` | 25 |
| `:client:clientB:data` | `testDebugUnitTest` | 25 |
| `:client:clientB:ui` | `testDebugUnitTest` | 3 |
| `:client:tmdb:data` | `testDebugUnitTest` | 38 |
| `:client:tmdb:ui` | `testDebugUnitTest` | 4 |
| `:core:data` | `testAndroidHostTest` | 11 |
| `:core:tracing-api` | `testAndroidHostTest` | 4 |
| `:feature:details:domain` | `testAndroidHostTest` | 3 |
| `:feature:details:ui-common` | `testDebugUnitTest` | 14 |
| `:feature:home:domain` | `testAndroidHostTest` | 3 |
| `:feature:home:ui-common` | `testDebugUnitTest` | 14 |
| `:feature:library:data` | `testDebugUnitTest` | 5 |
| `:feature:library:domain` | `testDebugUnitTest` | 4 |
| `:feature:library:ui-common` | `testDebugUnitTest` | 3 |
| `:feature:login:domain` | `testAndroidHostTest` | 5 |
| `:feature:login:ui-common` | `testDebugUnitTest` | 5 |
| `:feature:player:data` | `testDebugUnitTest` | 3 |
| `:feature:player:ui-common` | `testDebugUnitTest` | 11 |
| `:feature:player:ui-mobile` | `testDebugUnitTest` | 12 |
| `:feature:player:ui-tv` | `testDebugUnitTest` | 2 |
| `:feature:profiles:domain` | `testAndroidHostTest` | 9 |
| `:feature:profiles:ui-common` | `testDebugUnitTest` | 3 |
| `:feature:search:data` | `testDebugUnitTest` | 2 |
| `:feature:search:domain` | `test` | 2 |
| `:feature:search:ui-common` | `testDebugUnitTest` | 12 |

## Static proof and handoff

`rg` confirms exactly 41 API-37 declarations: the KMP convention plus the 40 owned Android-only modules. The only API-36 compile SDK declarations
are the six explicitly excluded KMP-04 build files. `rg -n --glob '*.gradle.kts' 'targetSdk' .` reports only:

- `app/build.gradle.kts`: `targetSdk = 36`
- `benchmark/build.gradle.kts`: `targetSdk = 36`
- `baselineprofile/build.gradle.kts`: `targetSdk = 36`

The continued root gate's six AAR metadata failures are limited to the KMP-04-owned modules
`:feature:search:data`, `:feature:library:{data,domain}`, `:feature:player:{data,domain}`, and `:playback:api`. Each failure explicitly requests
compile SDK 37 because it consumes API-37 KMP core artifacts. KMP-04's convention conversion is the handoff that closes this integration-order gap;
there is no tool incompatibility and no module requires a permanent API-36 exception.

No dependency version, production source, resource, web target, min SDK, target SDK, build type, flavor, generated profile, or unrelated file was
changed.

