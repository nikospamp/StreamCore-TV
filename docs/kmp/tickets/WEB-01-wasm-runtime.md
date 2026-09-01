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
- A proved web-testing selector strategy and Playwright infrastructure for later web tickets.
- Wasm ES-module/`HtmlElementView` compatibility spikes used by WEB-04.

## Non-Goals

- No production login/profile/browse UI.
- No web playback engine.
- No ClientB web graph.
- No hosting/CDN/CI deployment.
- No mobile web, SSR, SEO, desktop/iOS targets, or compatibility JS fallback.
- No guarantee of browser password-manager/autofill integration for canvas-rendered Compose text fields.

## Implementation Tasks

1. Add a library-only `wasmJs()` target to each required shared KMP module and resolve every dependency at the wasm compilation boundary. Shared
   libraries must not call `browser()` or `binaries.executable()`.
2. Add `:webApp` with `wasmJs { browser(); binaries.executable() }` and production distribution. It is the only executable web module.
3. Create the web entry point with `ComposeViewport`, full-window CSS, theme root, a diagnostic loading/error/ready shell, and explicit startup
   sequencing. Include a minimal `HtmlElementView` probe so Compose `1.12.0` HTML interop is linked before player work.
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
    - Official DataStore `WebLocalStorage` for auth, library, search history, and playback progress using distinct stable names.
    - Probe local storage with a write/read/remove operation before graph creation. If browser policy denies persistent storage, fall back to official
      `WebSessionStorage` and display a non-blocking warning that state will be lost when the tab closes.
    - Map storage corruption, quota, and security failures without swallowing cancellation or destroying the current in-memory state.
    - Configure the web Ktor engine used by the Coil `3.4.0` `coil-network-ktor3` artifact introduced in KMP-06.
    - External HTTPS URI launching with `noopener`/`noreferrer` behavior.
    - No-op/browser performance tracer.
9. Add web navigation routes using ID-only browser-safe parameters. Bind the controller to browser history and verify Back/Forward/direct URL/reload
   behavior.
10. Keep Android navigation payload optimizations platform-specific; do not serialize entire `ContentModel` values into browser URLs.
11. Show an actionable blocking configuration screen when config fetch/parse/validation fails. Do not start Koin with invalid configuration.
12. Add web graph tests and a smoke page that resolves every TMDB repository/ViewModel factory without rendering production feature screens.
13. Generate `wasmJsBrowserDistribution` and document the exact output directory and local static-server command.
14. Establish the test strategy before WEB-02:
    - Create a probe UI containing a button, text field, focus transitions, roles, labels, and stable `testTag` values.
    - Verify node-level behavior with Compose Multiplatform UI Test v2 on `wasmJs`.
    - Inspect Chromium, Firefox, and WebKit DOM/accessibility projection with Playwright and record which role/name/tag selectors are actually stable.
    - Use Compose UI Test as the primary semantic/node test layer. Playwright may use only selectors proven by the spike; otherwise it targets the
      viewport/canvas and drives browser-level keyboard, pointer, history, storage, network, and screenshots.
    - Document the locked strategy in `docs/kmp/web-testing.md`. Later tickets may not assume canvas children are ordinary DOM nodes.
15. Move Playwright setup into this ticket, pin its stable version in the lockfile, and run the probe in Chromium, Firefox, and WebKit.
16. Prove Kotlin/Wasm ES-module interop with an npm `@JsModule` external declaration. Compile/link a Shaka `5.2.3` import and minimal create/destroy
    probe against `HTMLVideoElement`. If Shaka's export shape exceeds Wasm external-type restrictions, define a committed ESM `.mjs` adapter and
    import that adapter with `@JsModule`; do not fall back to an untyped global `<script>`.

## Public API or Type Changes

- Add `WebRuntimeConfig` in the web application boundary.
- Add wasm actual/platform factories for storage, networking, images, links, and tracing.
- Add browser-safe web route types; Android `AppRoute` remains platform-owned.
- Shared business contracts do not gain browser APIs.

## Verification Commands

```powershell
.\gradlew.bat :webApp:compileKotlinWasmJs
.\gradlew.bat :webApp:wasmJsBrowserDistribution
.\gradlew.bat :webApp:wasmJsBrowserTest
.\gradlew.bat testAndroidHostTest
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
rg -n "^import (android|java|androidx\.annotation|androidx\.core|androidx\.tv|androidx\.media3|io\.ktor\.client\.engine\.okhttp)\." webApp core client feature playback -g "**/src/wasmJsMain/**/*.kt"
Set-Location webApp/e2e
npm ci
npx playwright install
npx playwright test
```

Run the development server separately because this command intentionally blocks. Complete the manual smoke checks, then terminate it before running
the remaining gate:

```powershell
.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun
```

## Test Scenarios

- Missing `/config.json` displays configuration guidance and does not start the graph.
- Invalid JSON, non-HTTPS base URL, and missing fields fail deterministically.
- Valid config starts the TMDB web graph without ClientB/Android definitions.
- All four browser stores use distinct names and retain values across reload.
- Persistent-storage denial falls back to session storage with a visible warning; corruption/quota/security failures are recoverable and tested.
- TMDB Fetch requests include expected base URL/auth headers and map network failures to `AppError`.
- Image loading succeeds through the Ktor-backed Coil loader.
- External links cannot retain `window.opener`.
- Browser Back/Forward, direct URL, and reload preserve route identity.
- Compose UI Test v2 can address the probe by `testTag`, and the documented Playwright strategy uses only selectors proven by the browser spike.
- Shaka's ESM import compiles/links through direct `@JsModule` declarations or the committed ESM adapter selected by the spike.
- Android debug compilations remain green after wasm targets are added.

## Acceptance Criteria

- Every required shared module resolves and compiles for `wasmJs`.
- The diagnostic web shell runs with valid config and fails clearly without it.
- `wasmJsBrowserDistribution` succeeds without embedding real configuration.
- Web storage/network/image/navigation adapters are tested.
- `docs/kmp/web-testing.md` fixes the Compose-test/Playwright responsibilities and selector contract for WEB-02 through WEB-04.
- No browser password-manager/autofill claim is made unless the probe proves a stable native input integration.
- Android TMDB and ClientB compilations remain green.
- No Android/Media3/ClientB dependency is present in the web runtime graph.

## Handoff Checklist

- [x] Modules receiving wasm targets listed.
- [x] Runtime config schema and security properties documented.
- [x] Storage names and adapters documented.
- [x] Persistent-to-session storage fallback and failure tests documented.
- [x] Web graph contents documented.
- [x] Compose UI Test/Playwright selector strategy documented in `docs/kmp/web-testing.md`.
- [x] `HtmlElementView` and Shaka ESM interop spike results included.
- [x] Development and production build results included.
- [x] Android regression compilation results included.
- [x] Final working tree is clean after committing this ticket.
