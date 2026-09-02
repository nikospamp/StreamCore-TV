# WEB-01 Wasm runtime evidence

## Identity and scope

- Branch: `codex/web-01-wasm-runtime`.
- Base: accepted KMP-07 integration commit `d6f93965e9c72eda6d841c33b9e2ca2dff875293`.
- Scope: Wasm runtime, platform adapters, diagnostic shell, test infrastructure, and WEB-04 interop probes only.
- The target architecture remains backend-agnostic. No production login/profile/browse UI, ClientB web graph, Media3 dependency, or playback engine is
  present.

## Wasm target graph

The following 28 shared libraries receive `wasmJs`:

- Core/playback: `:core:data`, `:core:domain`, `:core:tracing-api`, `:core:ui`, `:playback:api`.
- TMDB: `:client:tmdb:data`, `:client:tmdb:ui`, `:client:tmdb:player`.
- Login: `:feature:login:{data,domain,ui-common}`.
- Profiles: `:feature:profiles:{data,domain,ui-common}`.
- Home: `:feature:home:{domain,ui-common}`.
- Search: `:feature:search:{data,domain,ui-common}`.
- Details: `:feature:details:{data,domain,ui-common}`.
- Library: `:feature:library:{data,domain,ui-common}`.
- Player: `:feature:player:{data,domain,ui-common}`.

Shared modules remain libraries. They do not call `browser()` or `binaries.executable()`. Kotlin 2.3.21 requires an npm-capable environment when one
executable consumes a multi-project Wasm graph, so the library convention selects non-browser `nodejs()` without producing executable binaries.
Ten plain KMP libraries compile and execute 94 common tests through `wasmJsNodeTest`. Compose-owning libraries omit library Wasm test compilations
because Compose `1.12.0` creates duplicate `commonTest` resource tasks. Three plain libraries transitively exposing Compose/Skiko through
`:playback:api` (`:client:tmdb:player`, `:feature:library:domain`, and `:feature:player:data`) also keep their 11 tests on Android host: Node cannot
load Skiko's browser runtime. The complete 201-test common inventory continues through 24 Android host-test targets. `:webApp` alone calls
`browser()` and `binaries.executable()` and owns 23 executable browser tests.

## Runtime and security contract

`WebRuntimeConfig` is serializable and contains `tmdbBaseUrl`, `tmdbReadAccessToken`, and `tmdbAccountId`. `/config.json` is fetched before Koin starts.
Blank fields, malformed JSON, missing responses, and non-HTTPS base URLs produce an actionable blocking state and do not create the product graph.
The read-access token is intentionally browser-visible deployment configuration, not a secret. Only `config.example.json` is committed; the real
resource path is ignored.

The isolated web Koin application contains the shared feature modules, TMDB data/UI/source modules, four web DataStore modules, the Ktor JS/Fetch client, the
Ktor-backed Coil loader, secure URI handler, and `NoOpPerformanceTracer`. The diagnostic playback-session factory exists only so the Player
ViewModel factory can be resolved; it performs no playback. The smoke resolves 9 repository contracts and 8 ViewModels (17 definitions total).

## Storage

Stable DataStore names are:

- `tmdb_auth.preferences_pb`
- `search_history.preferences_pb`
- `library.preferences_pb`
- `playback_progress.preferences_pb`

Startup acquires `localStorage` and `sessionStorage` lazily inside guarded write/read/remove probes, so property-getter `SecurityError` failures enter
the same fallback policy. Each official Preferences DataStore installs `ReplaceFileCorruptionHandler { emptyPreferences() }`; Playwright proves an
invalid persisted protobuf is replaced while persistent mode remains ready. The diagnostic graph writes and reads one idempotent Boolean canary
through each store. A recoverable persistent open/canary security, quota, corruption, or I/O failure closes only that not-yet-ready isolated graph
and retries once with official `WebSessionStorage`, retaining a non-blocking tab-lifetime warning. Cancellation is rethrown, non-storage graph defects
are not masked by retry, and failure of both modes blocks startup. Playwright proves quota-driven session retry and the four distinct data/version
keys in Chromium, Firefox, and WebKit.

Android remains on stable DataStore `1.2.1`. That artifact publishes Wasm core APIs but does not contain `WebLocalStorage` or `WebSessionStorage`;
AndroidX introduced those required explicit classes in `1.3.0-alpha08`. WEB-01 therefore pins `datastore-core-okio:1.3.0-alpha08` only in
`wasmJsMain`; Android resolution remains `1.2.1`.

## Images, links, navigation, and tracing

- TMDB networking uses Ktor `Js`/Fetch and the existing authenticated `createTmdbHttpClient` contract. A diagnostic request through the real
  `SearchRepository` proves `/3/configuration`, `/3/genre/movie/list`, and `/3/search/movie`, Bearer/Accept headers, and HTTP 503 mapping to
  backend-agnostic `AppError.Server` in all three browsers.
- Coil uses `coil-network-ktor3` with a Ktor JS client. Coil 3.4's worker decoder does not complete under Playwright WebKit even though Fetch returns
  HTTP 200, so WebKit alone uses a synchronous Skia decoder registered before Coil's worker decoder. Chromium and Firefox keep the worker path.
  This fallback avoids a hung request but allocates the encoded image and decoded bitmap on the browser main thread; replacement with a compatible
  worker decoder is a WEB-03 performance follow-up.
- External links accept HTTPS only and open with `noopener,noreferrer`; all three engines prove `window.opener === null`.
- Routes contain IDs only. Direct URL, reload, pointer navigation, Back, and Forward preserve `/details/{id}` and `/player/{id}` identity.
- Web tracing binds `NoOpPerformanceTracer`; Android tracing remains Android-owned.

## Selector and interop spikes

`docs/kmp/web-testing.md` is authoritative. Compose UI Test v2 is the node/semantics layer. Playwright found no ordinary DOM role/name/tag projection
for Compose children in Chromium, Firefox, or WebKit, so browser tests use the host surface plus explicit shell/`HtmlElementView` DOM contracts.

Compose 1.12 `HtmlElementView` links and renders an `HTMLVideoElement`. Shaka `5.2.3` has no advertised ESM export, so committed
`shaka-adapter.mjs` normalizes its CommonJS/global-compatible export. Kotlin imports the adapter through `@JsModule`; the runtime probe creates and
destroys a Shaka Player and reports `data-shaka-probe="linked"` in all three engines. No playback source is loaded.

## Distribution and local serving

Production output:

```text
webApp/build/dist/wasmJs/productionExecutable
```

The directory contains `index.html`, `streamcore-web.js`, optimized app/Skiko Wasm assets, Compose resources, `config.example.json`, and the Shaka
adapter. It contains no real `config.json`.

For a simple production-artifact smoke server:

```powershell
Set-Location webApp/e2e
node server.mjs
```

The development server is:

```powershell
.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun
```

The required command was launched at `http://localhost:8080/`; with no real config present, Chromium reported
`data-runtime-state=blocking-error` and zero HTML video probes, proving the graph did not start.

## Verification summary

- `:webApp:compileKotlinWasmJs`: pass.
- `:webApp:wasmJsBrowserDistribution`: pass; final production assets include approximately 1.29 MiB JS, 4.52 MiB app Wasm, and 8.24 MiB Skiko Wasm.
- `:webApp:wasmJsBrowserTest`: 23 tests in 6 suites, zero failures/errors/skips.
- Ten compatible library `wasmJsNodeTest` tasks: 94 tests, zero failures/errors/skips. `:client:tmdb:data` contributes 46, including request policy
  and error mapping.
- Playwright `1.62.1`: 30/30 across Chromium, Firefox, and WebKit.
- Combined `:app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin`: pass; 355 actionable tasks.
- `testAndroidHostTest`: 201/201 across 24 KMP modules, matching KMP-07's KMP-host subset; zero failures/errors/skips.
- The complete Android XML inventory remains 265 tests across 64 suites when the two 25-test app flavor suites and 14 Android-only Player UI tests are
  included; this exactly matches KMP-07, with zero failures/errors/skips and no zero-test regression.
- Root `check -PverifyDesignTokensLogFiles=true --continue --max-workers=1`: pass; 1,940 actionable tasks. Design-token verification checked 307 production files
  (KMP-07 303 + four new `core`/`feature` Wasm adapters), with zero violations.
