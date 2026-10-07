# KMP build conventions

The backend-agnostic SDK and shared application modules target Android and Kotlin/Wasm. Versions come from `gradle/libs.versions.toml`; reusable configuration lives in `build-logic`. See the [module graph](../../../MODULE_DEPENDENCY_GRAPH.md) for ownership and [SDK integration](../sdk/integration.md) for publication requirements.

## Choose a convention

| Plugin | Use |
|---|---|
| `streamcore.kmp.library` | Plain shared Kotlin libraries |
| `streamcore.kmp.compose.library` | Shared Compose rendering, with local Android preview tooling |
| `streamcore.kmp.resources.library` | Optional SDK presentation resources/contracts, without preview tooling |
| `streamcore.sdk.publishing` | SDK publication and dependency-boundary checks |

The shared convention applies the official Android-KMP plugin and configures its single `android` target: compile SDK 37, minimum SDK 24, and JVM 11 bytecode. Do not recreate the Android target or add Android build types/flavors to these libraries. Android application/benchmark/profile targets remain at API 36; Android-only libraries have separate variant configuration.

The Compose and resource conventions add Compose Multiplatform and compiler plugins, resources, and Android-only `-Xlambdas=class`. Never apply that JVM flag to metadata or Wasm compilations. The UI convention adds `ui-tooling` to each module's local Android runtime classpath for previews, without adding it to published runtime dependencies. Sync Gradle after convention/source-root changes.

## Source sets and tests

| Source | Placement |
|---|---|
| Portable production Kotlin | `src/commonMain/kotlin` |
| Android implementations | `src/androidMain/kotlin` |
| Browser implementations | `src/wasmJsMain/kotlin` with a declared Wasm target |
| Portable tests | `src/commonTest/kotlin` with an executable test target |
| Android host-only tests | `src/androidHostTest/kotlin` |
| Android device tests | `src/androidDeviceTest/kotlin` with explicit device-test configuration |

Common sources must not import Android/JVM APIs or provider DTOs. Keep platform implementations behind the owning shared contract. Feature state, ViewModels, and portable rendering belong in `ui-common`; platform input/layout/DOM behavior belongs in the platform UI module.

Host tests are an explicit module opt-in:

```kotlin
plugins {
    id("streamcore.kmp.library")
}

streamCoreKmp {
    withHostTest()
    withWasmJs()
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
```

Use `withHostTest()` when a KMP module owns common tests. Compile-only modules remove their unused `commonTest` source set instead of adding filler tests. `:sdk:testing` is a base-plugin source-sharing project: each provider includes `sdk/testing/src/commonTest/kotlin` in its own suite. It has no independent compilation, test task, or publication.

`withWasmJs()` creates a library target with Node support, not an application executable. Plain shared tests run through `wasmJsNodeTest`. The convention suppresses library Wasm test compilations for Compose owners because the pinned Compose version creates duplicate common-test resource tasks. A plain module requiring Skiko at test runtime can explicitly use `withWasmJs(withTests = false)`.

SDK model/runtime/provider modules have Node suites; runtime also has browser-storage tests. Profiles/Player web UI and `:playback:web` declare browser test bundles explicitly. Node tests do not verify browser persistence, DOM behavior, or rendered UI. Current browser suites are listed in [web testing](web-testing.md).

## Resources and SDK boundaries

Resource owners enable Android resource processing on the official Android-KMP target so `composeResources` reaches published AARs and consuming APKs. Common vectors use literal supported colors, not Android framework references. Public Compose resource types must have exported dependencies.

Three optional SDK UI artifacts use the resource convention and are tested by independent Android/Wasm presentation consumers. Five headless SDK artifacts prohibit rendering dependencies. Koin, application screens, and playback engines stay outside all SDK publications. See [SDK verification](../sdk/verification.md) for those separate checks.

## Toolchains and gates

The daemon criteria select JDK 21 while library bytecode stays JVM 11. The build disables Node/Yarn downloads: provide `node` and `yarn` on `PATH`, or choose Node with `-PstreamcoreNodeExecutable=<path>`.

Root `check` includes:

| Gate | Contract |
|---|---|
| `verifyDesignTokens` | Production UI uses the allowed design-token definitions |
| `verifyKmpTestTargets` | Common-test owners expose an Android host-test target |
| `verifyKmpAndroidCompilerFlags` | Compose Android compilations receive the required JVM-only flag |
| `verifyKmpConventionPlugins` | Plain/Compose fixtures expose usable Android tasks and variants |
| `verifyKmpDependencyCompatibility` | Locked common/Android dependency coordinates resolve with the expected platform attributes |

`testAndroidHostTest` aggregates enabled KMP host suites. Android compilation uses `compileAndroidMain`; host tests use `compileAndroidHostTest` / `testAndroidHostTest`. Metadata, Wasm, SDK publication, independent consumers, and device execution require their own checks. An Android compile does not prove those targets.

Update the [dependency compatibility gate](dependency-compatibility-matrix.md) when changing shared dependencies. Compose reports/metrics remain opt-in for release/benchmark Android-only compilation; use the [benchmark workflow](../performance/mobile-navigation.md) when investigating performance.

## Local TMDB configuration

`:app:verifyTmdbRuntimeConfig` checks nonblank token/account settings without printing values. Authenticated builds pass `-PrequireTmdbRuntimeConfig=true`. Credential-free unit and graph builds remain supported.

A linked worktree can read the primary checkout's ignored configuration:

```powershell
.\gradlew.bat :app:verifyTmdbRuntimeConfig :app:assembleTmdbDebug `
  -PstreamcoreLocalPropertiesPath="<primary-checkout>\local.properties" `
  -PrequireTmdbRuntimeConfig=true
```

`STREAMCORE_LOCAL_PROPERTIES` is the environment equivalent. Resolution order is Gradle properties, checkout-local `local.properties`, then the selected external file. Keep credentials local rather than copying them into worktrees or artifacts.
