# StreamCore TV

Streaming application for Android phones, tablets, Android TV, and browsers, with a reusable Kotlin Multiplatform SDK. The target architecture is backend-agnostic: shared UI consumes SDK services and models; provider DTOs, networking, and authentication protocols stay inside provider modules.

Android has TMDB and simulated ClientB flavors. The browser application currently selects TMDB. The SDK is a development `0.1.0-alpha02` candidate with locally staged Maven artifacts; remote publication and stable API compatibility are not implied.

## Start here

1. Follow the setup and commands below to run the application.
2. Read the [module graph](MODULE_DEPENDENCY_GRAPH.md) for ownership and [project conventions](AGENTS.md) before changing code.
3. Use the [developer documentation map](docs/tracked/README.md) to find the runbook for your task and the [maintenance register](docs/tracked/maintenance.md) for unresolved work.

## Requirements

- Android Studio compatible with AGP 9.1.1
- JDK 21 for the Gradle daemon, selected by `gradle/gradle-daemon-jvm.properties`
- Android SDK 37; the application targets API 36 and runs on API 26 or newer
- Node.js 22+ and Yarn on `PATH` for Wasm builds/tests

Open the repository root in Android Studio. Use the checked-in Gradle 9.3.1 wrapper. Dependency versions live in `gradle/libs.versions.toml`; shared build setup lives in `build-logic`. SDK Android libraries have minimum API 24, independently of the application's API 26 minimum.

## Configure and run Android

The simulated `clientB` flavor runs without TMDB credentials. For TMDB, add the following to the ignored root `local.properties`, alongside your Android SDK path:

```properties
tmdbReadAccessToken=YOUR_TMDB_READ_ACCESS_TOKEN
tmdbAccountId=YOUR_TMDB_ACCOUNT_ID
```

Select `tmdbDebug` or `clientBDebug` in Android Studio's **Build Variants**, then run `app` on a phone, tablet, or Android TV device/emulator.

```powershell
# Credential-free provider
.\gradlew.bat :app:assembleClientBDebug

# Configured TMDB application
.\gradlew.bat :app:verifyTmdbRuntimeConfig :app:assembleTmdbDebug -PrequireTmdbRuntimeConfig=true
```

On other platforms, replace `.\gradlew.bat` with `./gradlew`. Keep `requireTmdbRuntimeConfig` enabled for authenticated verification; the preflight checks configuration without printing values.

Linked worktrees can read an existing ignored configuration file with `-PstreamcoreLocalPropertiesPath="<primary-checkout>\local.properties"` or `STREAMCORE_LOCAL_PROPERTIES`. Resolution order is Gradle properties, checkout-local `local.properties`, then the selected external file. Do not copy or commit credentials.

## Run the browser application

With TMDB configured, run `main()` in `webApp/src/wasmJsMain/kotlin/com/pampoukidis/streamcoretv/web/Main.kt` from Android Studio, or:

```powershell
.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun
```

The task generates `/config.json` under `webApp/build/generated/webDevelopmentConfig` and serves it only through the development server. `tmdbBaseUrl` optionally overrides `https://api.themoviedb.org/`. Restart after changing configuration. Missing settings fail before the server starts; browser configuration is visible to browser users.

Production builds exclude real configuration and require a separately supplied `/config.json`. Stop development serving before building a production distribution. See [web testing](docs/tracked/kmp/web-testing.md) and [deployment](docs/tracked/kmp/web-release.md).

## Architecture

Routes collect state and navigate; stateless screens render it; ViewModels call public SDK services. Runtime services own validation, authorization, account/profile isolation, saved state, and progress. Application roots create one provider client and register its public services with Koin. Rendering and playback engines remain application-owned.

| Area | Where to work |
|---|---|
| Shared models and consumer services | `sdk/model`, `sdk/api` |
| SDK workflows, session guards, storage, provider ports | `sdk/runtime` |
| Backend integrations | `sdk/providers/tmdb`, `sdk/providers/clientB` |
| Optional presentation resources/mappings | `sdk/ui`, `sdk/providers/<provider>/ui` |
| Feature state and platform screens | `feature/<name>/ui-common`, `ui-mobile`, `ui-tablet`, `ui-tv`, `ui-web` |
| Application design system | `core/ui`, `core/ui-web` |
| Application entry points and DI | `app`, `webApp` |
| Playback contracts and engines | `playback/api`, `playback/media3`, `playback/web` |

Player has mobile, TV, and web modules, with no separate tablet module. Provider-shared test journeys live in `sdk/testing/src/commonTest/kotlin` and run inside each provider's suites. See [SDK documentation](docs/tracked/sdk/README.md) for integration and [the module graph](MODULE_DEPENDENCY_GRAPH.md) for full dependency boundaries.

## Verify a change

Run the checks appropriate to the change. These commands do not require a device:

```powershell
# Repository design and build-convention gates
.\gradlew.bat check

# All enabled KMP Android host-test suites
.\gradlew.bat testAndroidHostTest

# Focused feature and SDK suites
.\gradlew.bat :feature:home:ui-common:testAndroidHostTest :sdk:runtime:testAndroidHostTest
```

Application assembly, SDK publication/independent-consumer checks, browser suites, and device tests are separate. Use [SDK verification](docs/tracked/sdk/verification.md), [web testing](docs/tracked/kmp/web-testing.md), and the [local review workflow](docs/tracked/agent-workflow.md) for their commands and limits. A successful compile is not device/browser acceptance.

## Documentation and local output

The [documentation map](docs/tracked/README.md) lists maintained developer guides. [Product direction](PRODUCT.md), [design rules](DESIGN.md), and the [maintenance register](docs/tracked/maintenance.md) describe current decisions and open work.

SDK interactive pages are optional generated output. Their authored sources remain tracked under `docs/tracked/sdk/guide-content` and `docs/tracked/sdk/explorer-content`; build them using the [SDK documentation instructions](docs/tracked/sdk/README.md). Generated HTML, captures, machine-specific notes, and historical working records stay local. They are not required to take over development.

## TMDB attribution

This product uses the TMDB API but is not endorsed or certified by TMDB. TMDB credentials and usage remain subject to TMDB's terms and policies.
