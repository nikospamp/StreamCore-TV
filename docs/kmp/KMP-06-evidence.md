# KMP-06 verification evidence

## Identity and scope

- Branch: `codex/kmp-06-shared-compose-resources`.
- Prerequisite commit: `e93f5b144d4dceb296b1de4cfe4619948a5ec19f`.
- Verification date: 2026-09-01 (Europe/Athens).
- Target architecture: backend-agnostic Android-only KMP Phase 1. No Wasm, browser target, web module, navigation-shell migration, or platform-screen
  conversion was added.
- Compile SDK remains 37 through the frozen convention. Application/benchmark/profile target SDK remains 36.

KMP-06 converts `:core:ui`, all seven feature `ui-common` modules, and both provider UI modules to the frozen
`streamcore.kmp.compose.library` convention. Android mobile/tablet/TV modules remain Android libraries and compile against the migrated APIs.

## Source-set and module inventory

| Module | `commonMain` Kotlin | `androidMain` Kotlin | `commonTest` Kotlin | Compose Resources |
|---|---:|---:|---:|---:|
| `:core:ui` | 57 | 6 | 0 | 25 files |
| `:feature:login:ui-common` | 10 | 0 | 2 | 0 |
| `:feature:profiles:ui-common` | 17 | 1 | 1 | 0 |
| `:feature:home:ui-common` | 10 | 0 | 4 | 0 |
| `:feature:search:ui-common` | 9 | 0 | 2 | 0 |
| `:feature:details:ui-common` | 9 | 0 | 3 | 0 |
| `:feature:library:ui-common` | 8 | 0 | 2 | 0 |
| `:feature:player:ui-common` | 10 | 0 | 2 | 0 |
| `:client:tmdb:ui` | 4 | 0 | 2 | 21 files |
| `:client:clientB:ui` | 4 | 0 | 2 | 9 files |

Portable UDF contracts, immutable state, ViewModels, route-effect helpers, neutral composables, design tokens, preview data, Koin modules, provider
presentation adapters, and behavioral tests now live in common source sets. Every module with common tests explicitly enables
`testAndroidHostTest`. `:core:ui` has no common tests and removes the unused default `commonTest` source set instead of creating a zero-test task.
Its three existing Compose UI test files live in `androidDeviceTest`; the enabled AGP 9 Android-KMP device target executes 10 tests.

Android-only placement is intentionally limited to:

- `:core:ui`: four `androidx.tv.material3` components, `Configuration`/`LocalConfiguration` platform selection, and Android preview annotations;
- `:feature:profiles:ui-common`: the existing mixed touch/TV `ProfileEditorScreen`, because it directly selects `StreamCoreTvButton` behavior;
- `:core:ui` Android resources: `Theme.StreamCoreTV` and one `app_name` manifest compatibility string. Android resource processing is explicitly
  enabled for this Android-KMP target. Compose rendering does not read these compatibility resources.

Common reusable previews use Compose Multiplatform's `androidx.compose.ui.tooling.preview.Preview`. Configuration-based
`@PreviewMobile`/`@PreviewTablet`/`@PreviewTV` remain Android-only. `AGENTS.md` now records these source-set and resource rules.

## Resource migration

`:core:ui/src/commonMain/composeResources` now owns:

- 31 shared strings;
- 21 vector drawables;
- `logo.webp` and the two login background WebP assets.

`:client:tmdb:ui` owns 20 avatar vectors and three TMDB authentication strings in Compose Resources. `:client:clientB:ui` owns nine avatar vectors
in Compose Resources. Generated resource classes are used by common rendering and Android consumers. No common source imports Android `R`,
`androidx.compose.ui.res`, `@StringRes`, or `@DrawableRes`.

The shared resource class is public because Android platform modules consume shared login strings/artwork. Provider resource classes remain
module-owned; provider mappers/resolvers expose only `StringResource`/`DrawableResource` through backend-agnostic UI contracts.

## Public API migration

The Android-resource-ID presentation types were removed from `:core:data`. `AppError`, `AppResult`, and `ErrorSource` remain there unchanged.
`:core:ui` now owns:

```kotlin
data class ErrorUiModel(
    val title: StringResource,
    val message: StringResource,
    val confirmAction: StringResource,
    val dismissible: Boolean = true,
)

fun interface ErrorPresentationMapper {
    fun map(error: AppError): ErrorUiModel
}
```

The default mapper retains the previous copy/action/dismissibility for every `AppError`. TMDB authentication still overrides the three
provider-specific resources; all other TMDB and all ClientB errors delegate to the default mapper. `MainActivity`, app tests, Koin bindings, and
provider tests consume the new UI-owned contract.

`ProfileAvatarArtworkResolver.resolve(avatarId)` now returns `DrawableResource?`. All known TMDB (20) and ClientB (9) IDs are verified; unknown IDs
remain `null`, so `StreamCoreProfileArtwork` retains its color fallback. Compose resource handles are stored in immutable resolver maps and remembered
by avatar ID, avoiding per-recomposition lookup/allocation.

## Portable APIs, lifecycle, and DI

- Common ViewModels use multiplatform Lifecycle/ViewModel artifacts and Koin 4.2.2 constructor DSL. Koin lookups remain only in module/route
  composition; stateless screens do not use Koin.
- `DetailsTrailerLauncher` replaces `java.net.URI` with common HTTPS validation and retains malformed/non-HTTPS/security failure behavior.
- The validator uses Ktor 3.5.0 `ktor-http` (`parseUrl`, `URLProtocol`, percent decoding, and IP parsing), plus strict raw-authority and DNS-label
  postconditions. This is the only new catalog alias/dependency. It rejects blank authorities/hosts, invalid ports, malformed percent escapes,
  `https://%`, `https://:443/path`, invalid host labels/IPv4 addresses, and every non-HTTPS URL without invoking `UriHandler`.
- Search comparison normalization uses common invariant `lowercase()` instead of `Locale.ROOT`.
- `PlayerViewModel` receives `PlayerClock`; `SystemPlayerClock` uses `kotlin.time.Clock.System`. A focused test injects epoch `1234` and verifies the
  persisted progress timestamp without changing playback/resume policy.

## Tracing and allocation proof

`HomeViewModel` now receives `PerformanceTracer`. The app graph selects `AndroidPerformanceTracer` only when the Android tracing variant's
`BuildConfig.ENABLED` is true (benchmark/profile variants) and `NoOpPerformanceTracer` otherwise. Common tests inject the no-op implementation.
`:feature:home:ui-common` was removed only from the Android `traceModules` dependency hook; it now depends on `:core:tracing-api`, while
`:feature:home:ui-mobile` retains Android tracing.

Publishing uses inline `traceIfEnabled(label = { "SC.Home.publish.$reason" })`. Counters and publication-generation bookkeeping are explicitly
guarded by `performanceTracer.enabled`. The disabled-tracer common test verifies zero begin/end/counter callbacks while state still publishes.

Bytecode command:

```powershell
& 'C:\Program Files\Android\Android Studio\jbr\bin\javap.exe' -c -p `
  feature/home/ui-common/build/classes/kotlin/android/main/com/pampoukidis/streamcoretv/feature/home/common/home/HomeViewModel.class
```

Result: `publish` calls `PerformanceTracer.getEnabled()` at bytecode offset 13 and branches directly into the state update at offset 18 when false.
The trace-label `makeConcatWithConstants` occurs only in the enabled branch at offset 367, immediately before `beginSection`; the class contains no
tracing `Function0` allocation. This is the required disabled-path interpolation/lambda allocation proof.

## Coil Ktor3 and cache evidence

Coil remains locked at 3.4.0. `:core:ui` now uses `coil-network-ktor3`; the obsolete `coil-network-okhttp` catalog alias and all usages are removed.
The Android source set supplies Ktor 3.5.0's OkHttp engine. `StreamCoreContentImage` uses Coil's common `LocalPlatformContext`.

`CoilDiskCacheTest` runs on the existing `Medium_Phone_API_36.1` AVD. A loopback HTTP server returns a deterministic 1x1 GIF. The test performs a
cold request, evicts the exact memory-cache key, repeats the request, and asserts the second result is `DataSource.DISK` with the server still at one
request. The debug network-security config denies cleartext in its base policy and permits it only for `localhost`/`127.0.0.1`; the former global
debug `usesCleartextTraffic=true` setting was removed. Release/main manifests remain unaffected.

Command:

```powershell
.\gradlew.bat :app:connectedTmdbDebugAndroidTest `
  '-Pandroid.testInstrumentationRunnerArguments.class=com.pampoukidis.streamcoretv.image.CoilDiskCacheTest' `
  --no-parallel '-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8' --console=plain
```

Result: `BUILD SUCCESSFUL`; one test, zero failures/errors/skips. Device log:

```text
KMP06_COIL_CACHE coldMs=1121 warmMs=28 coldSource=NETWORK warmSource=DISK requests=1
```

The times are observations, not thresholds. This deterministic test proves image success and disk reuse after memory eviction. Authenticated remote
TMDB content-row visual/cold-warm smoke remains part of the complete KMP-07 phone/tablet/TV parity campaign; no credential was read or introduced by
KMP-06.

## Verification

The AGP 9.1.1/KGP 2.3.21 Android-only target names the metadata lifecycle task `compileKotlinMetadata`. All ten tasks exist and complete as
`SKIPPED` because no separately publishable second target exists; the same common sources compile through `compileAndroidMain`.

| Command | Result |
|---|---|
| Ten converted modules: `compileAndroidMain` | Pass; all common production/resource sources compiled for Android |
| Ten converted modules: `compileKotlinMetadata` | Pass; ten lifecycle tasks present/expected `SKIPPED` |
| Nine migrated behavioral suites: `testAndroidHostTest` | Pass; 70 tests after one new Home tracing test; zero failures/errors/skips |
| `:app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin` | Pass; 343 actionable tasks |
| `:app:compileTmdbDebugAndroidTestKotlin` | Pass; deterministic cache test compiles |
| `:core:ui:connectedAndroidDeviceTest` | Pass; 10/10 Compose UI tests, zero failures/errors/skips, 26.484s test time |
| Focused connected `CoilDiskCacheTest` | Pass; one test, network→disk, one HTTP request under loopback-only policy |
| `verifyKmpAndroidCompilerFlags` | Pass; 21 Compose-KMP Android compile tasks, Android-only `-Xlambdas=class` |
| `verifyDesignTokensLogFiles` | Pass; 303 production Kotlin files checked, including `commonMain` and `androidMain` |
| `check :app:assembleTmdbDebug :app:assembleClientBDebug -PverifyDesignTokensLogFiles=true --continue --no-parallel ...` | Final exact-tree combined gate passed in 52s; 1,823 actionable tasks (114 executed, 1,709 up-to-date) |
| Common forbidden-import/resource scan | No matches |
| `coil-network-okhttp` scan | No matches |
| `git diff --check` | Pass |

The first full root run executed all continued work but failed only because the five design-token allow paths still named the pre-migration
`src/main` locations. The exact paths were moved to `commonMain`; the focused verifier and final root gate then passed. No rule, pattern, source root,
or checked module was excluded.

### Executed test inventory

Final root XML inventory: 64 suites, 265 tests, zero failures/errors/skips. The clean KMP-05 total was 264; KMP-06 adds one Home disabled-tracing test.

| Module/task | KMP-06 | KMP-00 | Delta |
|---|---:|---:|---:|
| `:feature:login:ui-common:testAndroidHostTest` | 5 | 5 | 0 |
| `:feature:profiles:ui-common:testAndroidHostTest` | 3 | 3 | 0 |
| `:feature:home:ui-common:testAndroidHostTest` | 15 | 14 | +1 |
| `:feature:search:ui-common:testAndroidHostTest` | 12 | 12 | 0 |
| `:feature:details:ui-common:testAndroidHostTest` | 14 | 14 | 0 |
| `:feature:library:ui-common:testAndroidHostTest` | 3 | 3 | 0 |
| `:feature:player:ui-common:testAndroidHostTest` | 11 | 11 | 0 |
| `:client:tmdb:ui:testAndroidHostTest` | 4 | 4 | 0 |
| `:client:clientB:ui:testAndroidHostTest` | 3 | 3 | 0 |
| **KMP-06 migrated host total** | **70** | **69** | **+1** |

The connected Coil cache suite and 10 core UI device tests are outside the 265 host-test inventory. No expected host-test task executed zero tests.
The core UI device-test runtime includes the Compose `ui-test-manifest`, so `ComponentActivity` resolves in every test.

### Independent-review hardening

The initial core UI connected report exposed 10/10 failures because `ComponentActivity` was absent from the Android-KMP device-test APK. The
follow-up adds the existing `libs.androidx.compose.ui.test.manifest` alias to the device-test runtime configuration and retains all three source files;
the replacement connected report is 10/10 green. No suite was hidden, excluded, or moved out of executable coverage.

The review also replaced the first permissive trailer regex with the Ktor structured parser and explicit validation described above, expanded the
malformed matrix to invalid named/out-of-range ports and host labels, and narrowed debug cleartext from a global opt-in to the loopback-only network
security config. Both connected suites and the final root gate were rerun after these fixes.

## Static boundaries and remaining KMP-07 work

Final scans find no `android.*`, `java.*`, `androidx.annotation.*`, `androidx.core.*`, TV Material, Android resource API, Hilt, `System.currentTimeMillis`,
`Locale.ROOT`, or `java.net.URI` in migrated common production sources. Provider DTOs/resources do not leak into core/domain/feature boundaries.
Provider avatar tests assert the complete ID sets, unique resources, every exact ID→generated resource mapping, and unknown-ID `null` fallback.

KMP-07 still owns the complete authenticated phone/tablet/TV visual and journey parity matrix, release/R8/benchmark/baseline-profile assembly gate, and
same-environment remote content-row observation. KMP-06 does not add a web target or regenerate Baseline Profiles.

No credential, generated output, `.gitignore`, unrelated production source, platform-screen architecture, or provider/data implementation was
modified. The final clean committed status and commit hash are reported in the agent handoff.
