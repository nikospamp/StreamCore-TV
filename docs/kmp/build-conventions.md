# KMP build conventions

KMP-02 establishes Android-only Kotlin Multiplatform foundations. The target architecture remains backend-agnostic, and no Wasm, JS, native, JVM
desktop, or web application target exists in Phase 1.

## Convention usage

Plain shared module:

```kotlin
plugins {
    id("streamcore.kmp.library")
}
```

Shared module with common tests:

```kotlin
streamCoreKmp {
    withHostTest()
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
```

Shared Compose module:

```kotlin
plugins {
    id("streamcore.kmp.compose.library")
}
```

The plain convention applies the official `com.android.kotlin.multiplatform.library` plugin, which creates and registers the single `android` target,
then owns compile SDK 37, minimum SDK 24, and JVM 11 configuration. API 37 is required by Compose Multiplatform 1.12 Android artifacts and is
supported by the locked AGP 9.1.1 toolchain. This compile-only integration prerequisite does not change the application's, benchmark's, or
baseline-profile producer's target SDK 36 runtime behavior. Module build scripts do not call `kotlin { android {} }`. The Compose convention
additionally applies Compose Multiplatform 1.12.0, the Kotlin Compose compiler plugin, Compose resources support, and Android-target-only
`-Xlambdas=class`.

Test compilations are intentionally not enabled by either convention. A module containing `src/commonTest/kotlin/**/*.kt` must opt in with
`streamCoreKmp { withHostTest() }`. This explicit module call invokes the official target's `withHostTest {}` without recreating the target. Root
`verifyKmpTestTargets` compares common test sources with actual `testAndroidHostTest` tasks. `:core:domain` is compile-only: it removes the unused
default `commonTest` source set, has no test directory, and enables no host test.

## Source placement

| Source | Placement |
|---|---|
| Portable production Kotlin | `src/commonMain/kotlin` |
| Android implementation Kotlin | `src/androidMain/kotlin` |
| Portable tests | `src/commonTest/kotlin` plus explicit `streamCoreKmp { withHostTest() }` |
| Android host-only tests | `src/androidHostTest/kotlin` |
| Android device tests | `src/androidDeviceTest/kotlin` plus explicit `withDeviceTest {}` |
| Browser implementation Kotlin | `src/wasmJsMain/kotlin`, after WEB-01 adds the target |

Common sources cannot import Android or JVM APIs. Platform implementations and provider models stay behind shared interfaces.

Shared dependencies must publish compatible KMP metadata and Android variants and be covered by `verifyKmpDependencyCompatibility`. Android-only
libraries belong in `androidMain` or an Android-only module. KMP membership does not imply Wasm compatibility; web support is established only by a
ticket that adds and verifies `wasmJs`.

Compose Resource owners must enable Android resource processing on the official Android-KMP target. This packages generated
`composeResources` assets into the AAR and consuming APK; KMP-07 verifies both provider UI modules this way. Common vector XML uses literal
Compose-supported colors and never Android framework resource references such as `@android:color/*`.

## Root ownership and task behavior

- The version catalog owns Kotlin 2.3.21, AGP 9.1.1, Compose Multiplatform 1.12.0, and every locked library version.
- `build-logic` owns reusable plain/Compose KMP configuration; KMP-03 and KMP-04 must not edit root build logic or the catalog.
- The existing `com.android.library` hook still creates benchmark/profile build types only for Android-only libraries. The Android-KMP plugin has a
  different ID and remains single-variant.
- The temporary JVM-to-Android-KMP compatibility bridge was removed after KMP-03/04 integration migrated all six transitional consumers: Login,
  Profiles, Home, and Search domain plus Details data/domain. No root configuration now overrides their platform, environment, or artifact-type
  attributes.
- Existing Android trace dependency injection remains scoped to Android application/library plugins. Shared modules use `:core:tracing-api` when
  migrated; Android UI/platform modules use `:core:tracing`.
- Compose compiler reports/metrics continue on release/benchmark Android-only Kotlin compile tasks. For future Compose KMP modules, the replacement
  single-variant task is `compileAndroidMain`; KMP-06 owns the first nonzero report/flag proof.
- New feature state, actions, effects, route-effect helpers, and ViewModels start in `ui-common`; platform input, adaptive layout, and TV focus remain
  in Android platform UI modules.
- AGP 9.1.1 exposes `compileAndroidMain`, `compileAndroidHostTest`, and `testAndroidHostTest`. With this Android-only target graph, KGP 2.3.21 names the
  common metadata lifecycle task `compileKotlinMetadata`; it is skipped when no separately publishable metadata compilation is required, while the
  same `commonMain` sources compile as part of `compileAndroidMain`.
- `:kmp-convention-fixtures:plain` and `:kmp-convention-fixtures:compose` apply only their convention plugin. `verifyKmpConventionPlugins` proves both
  receive `compileAndroidMain`/`assembleAndroidMain`, consumable Android API/runtime variants, and the Compose Android compiler flag without a local
  Android target declaration. They contain no production sources.
- Root `check` depends on `verifyDesignTokens`, `verifyKmpTestTargets`, `verifyKmpAndroidCompilerFlags`, `verifyKmpConventionPlugins`, and
  `verifyKmpDependencyCompatibility`.

## Linked-worktree TMDB configuration

`:app:verifyTmdbRuntimeConfig` validates only that `tmdbReadAccessToken` and `tmdbAccountId` are non-blank; it never logs values. Authenticated
builds use `-PrequireTmdbRuntimeConfig=true`, which wires the preflight ahead of TMDB variant pre-build tasks.

The root ignored `local.properties` remains the normal source. A linked worktree must reference the primary ignored file without copying it:

```powershell
.\gradlew.bat :app:verifyTmdbRuntimeConfig :app:assembleTmdbDebug `
  -PstreamcoreLocalPropertiesPath="<primary-checkout>\local.properties" `
  -PrequireTmdbRuntimeConfig=true
```

`STREAMCORE_LOCAL_PROPERTIES` is the environment-variable equivalent. Gradle properties still take precedence, followed by the worktree-local
ignored file and then the linked file. Credential-free unit/graph builds retain the existing safe empty-config behavior.

The design-token verifier scans Kotlin production files under `main/kotlin`, Kotlin-under-`main/java`, `commonMain/kotlin`, `androidMain/kotlin`, and
`wasmJsMain/kotlin`, and fails if the configured production set is empty.
