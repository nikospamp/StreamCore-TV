# StreamCore TV

Streaming application for Android phones, tablets, Android TV, and browsers, with a reusable Kotlin Multiplatform SDK. The target architecture is backend-agnostic: shared UI consumes SDK services and models; provider DTOs, networking, and authentication protocols stay inside provider modules.

The repository contains both the application family and a headless SDK for Android and Kotlin/Wasm. The SDK is a development `0.1.0-alpha02` candidate with locally staged Maven artifacts; remote publication and stable API compatibility are not implied.

## Application surfaces

| Client | UI and interaction | Provider wiring |
|---|---|---|
| Android mobile | Compose / Material 3, touch navigation | `tmdb` or `clientB` flavor |
| Android tablet | Adaptive Compose layouts; shared mobile Player | `tmdb` or `clientB` flavor |
| Android TV | Compose for TV, D-pad navigation and focus | `tmdb` or `clientB` flavor |
| Browser | Compose Multiplatform / Wasm, TV visual language adapted for pointer/keyboard and resizable windows | TMDB |

One Android application serves phones, tablets, and TV. The browser has its own application root. Current flows include:

- Sign-in, session restoration, profile selection and management, with provider-supported PIN entry.
- Home collections and carousel, content details and recommendations, search and recent searches.
- My List, Likes, Continue Watching, and profile-scoped playback progress and resume.
- Playback through Android Media3 or the browser Shaka/native-video integration, with shared player state and platform-specific controls.

TMDB supplies authentication and catalogue data with local reference profiles. ClientB simulates authentication and catalogue data; it does not validate passwords against a real service. Both Android flavors and the browser app enable **sample-media playback**; catalogue titles are not full-length TMDB streams. Protected-profile reference scenarios are available through ClientB. See the [SDK capability and profile contracts](docs/tracked/sdk/integration.md) for supported operations and limitations.

## Start here

1. Follow the setup and commands below to run the application.
2. Read the [module graph](MODULE_DEPENDENCY_GRAPH.md) for ownership and [project conventions](AGENTS.md) before changing code. For SDK-only integration, start with the [credential-free quickstart](docs/tracked/sdk/quickstart.md).
3. Use the [developer documentation map](docs/tracked/README.md) to find the runbook for your task and the [maintenance register](docs/tracked/maintenance.md) for unresolved work.

## Requirements

- Android Studio compatible with AGP 9.1.1
- JDK 21 for the Gradle daemon, selected by `gradle/gradle-daemon-jvm.properties`
- Android SDK 37; the application targets API 36 and runs on API 26 or newer
- Node.js 22+ and Yarn on `PATH` for Wasm builds/tests

Open the repository root in Android Studio. Use the checked-in Gradle 9.3.1 wrapper. Dependency versions live in `gradle/libs.versions.toml`; shared build setup lives in `build-logic`. SDK Android libraries have minimum API 24, independently of the application's API 26 minimum.

The shared stack uses Kotlin 2.3.21, Compose Multiplatform 1.12.0, Coroutines/Flow, and Ktor. Koin 4.2.2 uses the classic constructor DSL in application composition; the published SDK is independent of Koin. Node and Yarn automatic downloads are disabled, so install them before running Wasm tasks. Optional [development tools](tools/README.md) additionally use PowerShell 7, Python, and Playwright/Chrome as described in their runbooks.

## Configure and run Android

The simulated `clientB` flavor runs without TMDB credentials. For TMDB, add the following to the ignored root `local.properties`, alongside your Android SDK path:

```properties
tmdbReadAccessToken=YOUR_TMDB_READ_ACCESS_TOKEN
tmdbAccountId=YOUR_TMDB_ACCOUNT_ID
```

Select `tmdbDebug` or `clientBDebug` in Android Studio's **Build Variants**, then run `app` on a phone, tablet, or Android TV device/emulator.

For TMDB, sign in with the account matching `tmdbAccountId`; the application uses it as an expected-account restriction. For ClientB, enter any nonblank demo identifier and password.

```powershell
# Credential-free provider
.\gradlew.bat :app:assembleClientBDebug

# Configured TMDB application
.\gradlew.bat :app:verifyTmdbRuntimeConfig :app:assembleTmdbDebug -PrequireTmdbRuntimeConfig=true
```

On other platforms, replace `.\gradlew.bat` with `./gradlew`. Keep `requireTmdbRuntimeConfig` enabled for authenticated verification; the preflight checks configuration without printing values.

For a ClientB protected-profile demo, add `-PstreamcoreClientBProfileScenario=SingleProtected` to the build and use demo PIN `1234`. Other scenarios are `Standard` (default), `Single`, and `HouseholdProtected`; see the [profile-entry contract](docs/tracked/sdk/integration.md#profile-entry-and-pin-activation).

Linked worktrees can read an existing ignored configuration file with `-PstreamcoreLocalPropertiesPath="<primary-checkout>\local.properties"` or `STREAMCORE_LOCAL_PROPERTIES`. Resolution order is Gradle properties, checkout-local `local.properties`, then the selected external file. Do not copy or commit credentials.

## Run the browser application

With TMDB configured, run `main()` in `webApp/src/wasmJsMain/kotlin/com/pampoukidis/streamcoretv/web/Main.kt` from Android Studio, or:

```powershell
.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun
```

The task generates `/config.json` under `webApp/build/generated/webDevelopmentConfig` and serves it only through the development server. `tmdbBaseUrl` optionally overrides `https://api.themoviedb.org/`. Restart after changing configuration. Missing settings fail before the server starts; browser configuration is visible to browser users.

Production builds exclude real configuration and require a separately supplied `/config.json`. Stop development serving before building a production distribution. See [web testing](docs/tracked/kmp/web-testing.md) and [deployment](docs/tracked/kmp/web-release.md).

## Architecture

Routes collect state with lifecycle awareness and navigate; stateless screens render immutable UI state; ViewModels expose `StateFlow` and call public SDK services. Application roots create one provider client, register its public services and optional presentation mappings with Koin, and close the client with its owning container. Rendering, navigation, input handling, and playback engines remain application-owned.

The headless dependency direction is `:sdk:providers:<provider> -> :sdk:runtime -> :sdk:api -> :sdk:model`. Runtime services are organized by domain around a shared session owner and enforce validation, authorization, content policy, and saved-state rules. Features consume public API/model contracts; provider DTOs, runtime provider ports, and storage contracts remain outside feature UI.

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
| Performance measurement and profiles | `benchmark`, `benchmark/ui-driver`, `baselineprofile`, `core/tracing-api`, `core/tracing` |

Player has mobile, TV, and web modules, with no separate tablet module. Optional SDK presentation supplies Compose resources, avatar mappings, and error wording without application screens or playback engines. Provider-shared test journeys live in `sdk/testing/src/commonTest/kotlin` and run inside each provider's suites; `sdk:testing` has no publication or independent test task.

SDK construction performs no network work. Restore through `client.auth.restoreSession()` or log in directly, observe `client.context`, then activate a profile through `client.profiles`. Restoration can recover account identity; it never restores profile authorization. Library, search history, and playback progress are SDK-local and isolated by backend, storage namespace, account, and profile; old development storage is not imported. See [SDK integration](docs/tracked/sdk/integration.md) for lifecycle and persistence contracts and [the module graph](MODULE_DEPENDENCY_GRAPH.md) for dependency boundaries.

## Use the SDK independently

Start with the [SDK quickstart](docs/tracked/sdk/quickstart.md) for a complete ClientB journey without credentials. Consumers declare a provider artifact from the locally staged Maven repository; public API/model dependencies are exported transitively, and optional presentation is a separate dependency. Both headless providers support Android and Kotlin/Wasm, even though the browser application currently wires only TMDB.

The [independent consumer samples](samples/README.md) resolve Maven coordinates without SDK project dependencies or composite-build substitution. They cover headless Android/Node journeys and optional Android/browser resource loading. The [publication workflow](docs/tracked/sdk/integration.md#maintainer-publication-and-verification) stages the eight supported artifacts and verifies those consumers. Read the [compatibility notes](docs/tracked/sdk/integration.md#compatibility) before updating an existing consumer, and upgrade the SDK artifacts together.

## Verify a change

Run the checks appropriate to the change. These commands do not require a device:

```powershell
# Repository design and build-convention gates
.\gradlew.bat :check

# All enabled KMP Android host-test suites
.\gradlew.bat :testAndroidHostTest

# Android application unit tests for both providers
.\gradlew.bat :app:testTmdbDebugUnitTest :app:testClientBDebugUnitTest

# Focused feature and SDK suites
.\gradlew.bat :feature:home:ui-common:testAndroidHostTest :sdk:runtime:testAndroidHostTest
```

`:check` runs the root gates; it does not run all tests. Application assembly, SDK source/publication checks, browser suites, and device tests are separate. Choose the relevant workflow:

| Change | Verification guide |
|---|---|
| SDK contracts, providers, or published artifacts | [SDK verification](docs/tracked/sdk/verification.md) and [consumer samples](samples/README.md) |
| Browser runtime, UI, or playback | [Browser tests and Playwright matrix](docs/tracked/kmp/web-testing.md) |
| Shared Android/browser UI | [Local review and capture workflow](docs/tracked/agent-workflow.md) and [control regression checklist](docs/tracked/shared-control-styling.md#regression-checklist) |
| Android runtime performance | [Benchmark and Baseline Profile workflow](docs/tracked/performance/mobile-navigation.md) |

The review helpers support Android captures, browser captures, and deterministic comparison boards. Their certified build path is for authenticated TMDB acceptance reviews; ordinary IDE/debug/development-server iteration remains available. A successful compile or an older verification report does not establish current device/browser acceptance. Pending release and acceptance work is tracked in the [maintenance register](docs/tracked/maintenance.md).

## Documentation and local output

The [documentation map](docs/tracked/README.md) lists maintained developer guides. [Product direction](PRODUCT.md), [design rules](DESIGN.md), and the [maintenance register](docs/tracked/maintenance.md) describe current decisions and open work.

SDK interactive pages are optional generated output. Their authored sources remain tracked under `docs/tracked/sdk/guide-content` and `docs/tracked/sdk/explorer-content`; build them using the [SDK documentation instructions](docs/tracked/sdk/README.md). Generated HTML, captures, machine-specific notes, and historical working records stay local. They are not required to take over development.

## TMDB attribution

This product uses the TMDB API but is not endorsed or certified by TMDB. TMDB credentials and usage remain subject to TMDB's terms and policies.
