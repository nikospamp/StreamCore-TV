# WEB-01 — Add the Wasm Runtime and Web Platform Adapters

## Goal

Add `wasmJs` compilation to the migrated shared modules and produce a locally runnable, production-buildable web shell with runtime configuration,
browser persistence, networking, images, Koin, and navigation.

## Context

Phase 1 intentionally proves Android before web compilation. This ticket is the portability canary for the entire shared stack. It must solve
target/platform adapters without implementing product screens beyond a diagnostic shell.

Compose Multiplatform web and Koin Compose web are accepted pre-stable dependencies for this project. Keep their usage isolated at the web composition
boundary.

## Dependencies and Parallelization

- **Depends on:** KMP-07 accepted green commit.
- **Blocks:** WEB-02.
- **Parallelization:** None. This ticket owns `wasmJs` target declarations, `:webApp`, runtime config, and platform adapters.

## In Scope

- `wasmJs` target for every KMP module required by the TMDB web graph.
- New `:webApp` executable module.
- Web Koin graph and target adapters.
- Runtime configuration, DataStore local storage, Ktor Fetch networking, Coil networking, URI launching, browser navigation, and production
  distribution.

## Non-Goals

- No production login/profile/browse UI.
- No web playback engine.
- No ClientB web graph.
- No hosting/CDN/CI deployment.
- No mobile web, SSR, SEO, desktop/iOS targets, or compatibility JS fallback.

## Implementation Tasks

1. Add `wasmJs { browser(); binaries.executable() }` to each required KMP module and resolve every dependency at the wasm compilation boundary.
2. Add `:webApp` with a `wasmJs` browser executable and production distribution.
3. Create the web entry point with `ComposeViewport`, full-window CSS, theme root, a diagnostic loading/error/ready shell, and explicit startup
   sequencing.
4. Define a serializable runtime configuration:

```kotlin
data class WebRuntimeConfig(
    val tmdbBaseUrl: String,
    val tmdbReadAccessToken: String,
    val tmdbAccountId: String,
)
```

5. Fetch `/config.json` before creating the product graph. Validate HTTPS base URL and required nonblank fields. Include `config.example.json` only;
   ignore real `config.json`.
6. Map `WebRuntimeConfig` to the shared `TmdbRuntimeConfig`. Treat the token as intentionally browser-visible; never label it secret or bake it into
   Wasm.
7. Start an isolated web Koin graph with TMDB provider modules, shared feature modules, web storage/network modules, and no Android/ClientB/Media3
   definitions.
8. Implement wasm platform adapters:
    - Ktor JS/Fetch engine and TMDB client.
    - DataStore `WebLocalStorage` for auth, library, search history, and playback progress using distinct stable names.
    - Coil `coil-network-ktor3` image loader.
    - External HTTPS URI launching with `noopener`/`noreferrer` behavior.
    - No-op/browser performance tracer.
9. Add web navigation routes using ID-only browser-safe parameters. Bind the controller to browser history and verify Back/Forward/direct URL/reload
   behavior.
10. Keep Android navigation payload optimizations platform-specific; do not serialize entire `ContentModel` values into browser URLs.
11. Show an actionable blocking configuration screen when config fetch/parse/validation fails. Do not start Koin with invalid configuration.
12. Add web graph tests and a smoke page that resolves every TMDB repository/ViewModel factory without rendering production feature screens.
13. Generate `wasmJsBrowserDistribution` and document the exact output directory and local static-server command.

## Public API or Type Changes

- Add `WebRuntimeConfig` in the web application boundary.
- Add wasm actual/platform factories for storage, networking, images, links, and tracing.
- Add browser-safe web route types; Android `AppRoute` remains platform-owned.
- Shared business contracts do not gain browser APIs.

## Verification Commands

```powershell
.\gradlew.bat :webApp:compileKotlinWasmJs
.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun
.\gradlew.bat :webApp:wasmJsBrowserDistribution
.\gradlew.bat :webApp:allTests
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
rg -n "android\.|java\.|androidx\.tv|media3|okhttp" webApp */*/src/wasmJsMain -g "*.kt"
```

Run the development server interactively only for the smoke checks; terminate it after validation.

## Test Scenarios

- Missing `/config.json` displays configuration guidance and does not start the graph.
- Invalid JSON, non-HTTPS base URL, and missing fields fail deterministically.
- Valid config starts the TMDB web graph without ClientB/Android definitions.
- All four browser stores use distinct names and retain values across reload.
- TMDB Fetch requests include expected base URL/auth headers and map network failures to `AppError`.
- Image loading succeeds through the Ktor-backed Coil loader.
- External links cannot retain `window.opener`.
- Browser Back/Forward, direct URL, and reload preserve route identity.
- Android debug compilations remain green after wasm targets are added.

## Acceptance Criteria

- Every required shared module resolves and compiles for `wasmJs`.
- The diagnostic web shell runs with valid config and fails clearly without it.
- `wasmJsBrowserDistribution` succeeds without embedding real configuration.
- Web storage/network/image/navigation adapters are tested.
- Android TMDB and ClientB compilations remain green.
- No Android/Media3/ClientB dependency is present in the web runtime graph.

## Handoff Checklist

- [ ] Modules receiving wasm targets listed.
- [ ] Runtime config schema and security properties documented.
- [ ] Storage names and adapters documented.
- [ ] Web graph contents documented.
- [ ] Development and production build results included.
- [ ] Android regression compilation results included.
- [ ] Final working tree is clean after committing this ticket.

