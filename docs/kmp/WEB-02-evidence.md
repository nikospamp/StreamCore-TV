# WEB-02 login and profiles evidence

## Identity and scope

- Branch: `codex/web-02-login-profiles`.
- Base: accepted WEB-01 integration commit `2267306e5df827deded96addd91b6ec947e9f655`.
- Scope: TMDB web login, session restoration, profile selection, profile management, web design-system controls, and startup routing.
- The target architecture remains backend-agnostic. Feature UI consumes shared models, state, actions, effects, and repository contracts; no provider DTO or SDK type enters the new web UI modules.
- Home, Search, Library, Details, playback, ClientB web composition, and narrow/mobile browser layouts remain out of scope.

## Modules and public APIs

### `:core:ui-web`

- `StreamCoreWebButton` and `StreamCoreWebButtonVariant`.
- `StreamCoreWebPanel`.
- `StreamCoreWebLargeScreenBackground` and `StreamCoreWebScrim`.
- `StreamCoreWebContentCard`.
- `StreamCoreWebProfileCard`.
- `StreamCoreWebBlockingSurface`.
- `StreamCoreWebDimens` and `Modifier.webEscape`.

The components reuse `StreamCoreTheme`, Material color/typography/shape roles, shared artwork, and the existing profile-avatar composition local. Raw web dimensions are centralized in the module token file and included in the root design-token contract.

### `:feature:login:ui-web`

- `WebLoginRoute` resolves `LoginViewModel` with `koinViewModel()` at the route boundary and collects its single immutable state with `collectAsStateWithLifecycle()`.
- `WebLoginScreen` is stateless and previewable. It reuses the shared cinematic login background, strings, validation types, actions, and effects.
- The Wasm credential surface is one typed native `HtmlElementView` form. Identifier, password, visibility, and submit listeners are installed once,
  read current callbacks through `rememberUpdatedState`, and are removed exactly on release. Submit synchronously commits both DOM values through
  the existing `LoginAction` boundary before dispatching `Submit`, eliminating Compose hidden-input focus races without changing the backend-agnostic
  ViewModel contract. Android retains the existing Compose field implementation.
- Successful login clears identifier/password state before leaving the route. No credential is written to a route, log, screenshot, or committed configuration.

### `:feature:profiles:ui-web`

- `WebProfilesRoute` / `WebProfilesScreen` cover loading, content, empty, error, selection, manage, delete confirmation, and unknown-avatar fallback.
- `WebProfileEditorRoute` / `WebProfileEditorScreen` cover create/edit, avatar and maturity selection, validation, saving, deletion, and cancellation.
- TMDB profile create/edit/delete state is serialized in the existing browser DataStore under a key derived from the configured account. It survives
  reload and keeps configured accounts isolated on the same browser profile; this is account-scoped local persistence, not remote TMDB profile sync.
- The browser editor uses typed `HtmlElementView` boundaries for the display-name input and action strip. Stable DOM listeners are installed once,
  read current callbacks through `rememberUpdatedState`, and remove the exact listener instances on release.
- The delete confirmation is an `expect`/`actual` boundary: Android retains the Compose dialog; Wasm uses a native `HTMLDialogElement` for top-layer
  modality, native Tab containment, Escape cancellation, Cancel autofocus, and trigger-focus restoration (including WebKit's synchronous fallback).
- Lazy profile content uses stable profile IDs and explicit content types.

Android UI public APIs were not changed. The web editor route now distinguishes successful profile mutation from cancellation, and the profiles
route accepts a monotonic revision used to refresh its retained ViewModel after create/edit/delete.

## Input, focus, and accessibility contract

- Pointer hover changes lift/scale; click activates the same action as keyboard activation.
- Tab follows the browser's native order through the login and profile-editor text inputs and their adjacent native controls; login and profile routes
  request deterministic initial focus.
- Left/Right retain native caret/selection behavior while a text input owns focus. Explicit arrow-key focus edges apply to non-text Compose profile
  rows and native editor/dialog action controls.
- Enter/Space activate Compose button/card semantics.
- Escape cancels the active dialog/editor or exits profile manage mode.
- The editor modal uses the browser top layer and inert background. Cancel initially owns focus; arrows transfer between Cancel/Delete; Escape closes
  without leaving the editor; close restores the same native Delete trigger. A 20-cycle open/Escape scenario verifies cleanup and listener identity.
- Focus is visible through geometry plus a high-contrast outer border, not color alone.
- Foundation lazy/scroll containers bring focused children into the visible viewport.
- Fields expose email/password platform content semantics through native input types and autocomplete hints; errors, roles, disabled/loading state, dialog descriptions,
  selected avatar/maturity state, and profile labels are present in semantics. The native display-name error is visible, uses alert/live semantics,
  and is referenced by `aria-errormessage` through a real element ID. Profiles load-error gives Retry deterministic initial focus.

The login fields are native DOM inputs with `autocomplete="username"` and `autocomplete="current-password"`, but password-manager/autofill UI behavior
is **not claimed**. The profile display-name editor is a native DOM text input with `autocomplete="nickname"`; only deterministic typing/submission
is claimed. WEB-01's selector limitation remains authoritative for canvas content. Playwright uses Compose accessibility roles only to resolve semantic bounds, then drives real viewport canvas input; stable DOM
test IDs are limited to the explicit native login/editor controls. `docs/kmp/web-testing.md` records the exact role/name and native test-ID allowlist proven
with Compose Multiplatform 1.12.0 and Playwright 1.62.1; all other projected Compose selectors remain prohibited.

## Startup, session, and history routing

- Missing/invalid session: replace browser history with `/login`.
- Valid session without a valid persisted profile: `/profiles`.
- Valid session with a persisted profile ID that still exists: `/authenticated`, the temporary landing surface for WEB-03.
- Create/edit routes are `/profiles/new` and `/profiles/{profileId}/edit`; route payloads contain IDs only.
- Selected profile ID is stored in the existing official TMDB browser DataStore. Reload validates the TMDB session and profile membership before
  restoring the landing route. Successful profile mutation increments a shell revision, refreshes the retained Profiles ViewModel, and reconciles
  selected-profile state before `/authenticated`; cancellation does not refresh.
- Session expiry clears TMDB auth preferences (including the selected-profile key), replaces the route with Login, and prevents Back/Forward from resurrecting authenticated state.
- Production account-detail and session-validation requests use TMDB's required `session_id` query parameter. The runner does not log request URLs or
  values, and credentials plus request/read-access tokens are never placed in URLs.
- The historical guarded live run on wrapper commit `28d8fbf` asserted the canonical browser/request credential payload, reached session creation,
  captured a non-empty session only from a 2xx session-creation response, reached Profiles, restored Profiles after reload, and asserted a 2xx
  temporary-session deletion in `finally`. It did not explicitly assert the response status for request-token creation, login validation, or
  account details. The account request was optional in the product flow, so reaching Profiles did not prove its status. The failure observed before
  the final accepted run occurred in the following atomic preferences transaction because TMDB's account
  display name was absent. AndroidX Preferences 1.2.1 Wasm casts the `null` result of removing an absent key to non-null `T`; project-owned
  Wasm-reachable removals are now guarded with a presence check. The earlier status-code-30 result came from a zero-delay automation focus race,
  not rejected canonical credentials. Code-30 mapping remains hardened and displays deterministic “Sign-in failed” guidance.
- The current verified-identity contract does not persist or expose a login when configured-account loading fails after session creation. It reports
  the mapped error on Login and attempts best-effort deletion of the uncommitted remote session. Browser coverage for the account-503 case asserts
  the mapped class and non-empty presentation fields without assuming exact copy.
- The opt-in live smoke now observes the four exact API paths (request-token creation, login validation, session creation, and configured-account
  details) on the configured API origin and requires every observed response to be 2xx. Remote cleanup requires both a 2xx response and parsed JSON
  `success: true`; transport, non-2xx, malformed-body, and false/missing-success outcomes collapse to one fixed redacted error. Cleanup is nested so
  browser local/session storage is still cleared after any such failure. This strengthened assertion has **not** been run with live credentials.
- WEB-01 diagnostics remain available at `/diagnostic`; the previous Details/Player ID probes remain diagnostic-only.

## Browser screenshots and Android TV comparison

Deterministic, credential-free frames are committed under `webApp/e2e/screenshots/` for Chromium, Firefox, and WebKit at both `1280x720` and `1920x1080`:

- `<engine>-<width>-login.png`
- `<engine>-<width>-profiles.png`

Historical frames generated for commit `28d8fbf` were inspected after that commit's native-form matrix. Each contained complete login copy/background, native fields,
visibility control, primary action, and disabled auxiliary copy at both target viewports. Login capture
waits for exact accessible names for all five actions and exercises their real canvas hover bounds before capture; the profile screenshot additionally gates the focused
avatar crop for non-trivial rendered content before capture. At 1920, profile content is centered on a
1280px rail; the 1280 layout retains the established screen margins. The frames were visually compared with `TvLoginScreen`, `TvProfilesScreen`,
`TvProfileTile`, and `TvProfileEditorScreen` plus the shared design reference. The web result retains the dark cinematic canvas, shared landscape
artwork, left-side login panel, restrained ember accent, large profile imagery, 10-foot typography, and pronounced TV-like focus geometry. Browser
layout density and Material3 rendering differ intentionally from TV Material. This is a hierarchy/input-language comparison, not a pixel-equality claim.
Screenshot readiness never keys, remounts, or delays the production route beyond the shared bounded visual-settle signal; test-side semantic-bound
hover and crop polling exercise paint readiness without resetting application focus or scroll state.

## Verification

All commands were run from the WEB-02 worktree unless a subdirectory is shown.

| Command | Result |
|---|---|
| `.\gradlew.bat :core:ui-web:compileKotlinWasmJs` | Pass |
| `.\gradlew.bat :feature:login:ui-web:compileKotlinWasmJs` | Pass |
| `.\gradlew.bat :feature:profiles:ui-web:compileKotlinWasmJs` | Pass |
| `.\gradlew.bat :webApp:wasmJsBrowserDistribution` | Pass; production assets are approximately 1.29 MiB JS, 5.16 MiB app Wasm, and 8.24 MiB Skiko Wasm |
| `.\gradlew.bat :webApp:wasmJsBrowserTest` | Pass; 33 tests, zero failures/errors/skips across 8 suites |
| `npm ci` in `webApp/e2e` | Pass; 3 packages, 0 vulnerabilities |
| `npx playwright install` | Pass |
| Historical Tier-1 combined focused gate after the review fixes began: `.\gradlew.bat :client:tmdb:data:testAndroidHostTest :client:tmdb:data:wasmJsNodeTest :feature:login:ui-web:compileKotlinWasmJs :feature:login:ui-web:compileAndroidMain :webApp:wasmJsBrowserTest --console=plain --max-workers=1` | First run failed at `compileAndroidHostTest` on two test-fixture type mismatches; 23 actionable tasks. After a test-only fixture correction, TMDB executed 70 Android-host plus 70 Wasm-node tests with zero failures/errors/skips and login compiled for Android/Wasm, but `:webApp` ran 42 tests with 8 failures, all in new coordinator tests due global session-storage test assertions; overall failed, 307 actionable tasks. |
| Historical Tier-1 focused `:webApp` rerun after test isolation correction: `.\gradlew.bat :webApp:wasmJsBrowserTest --console=plain --max-workers=1` | Pass; 42/42, zero failures/errors/skips; 264 actionable tasks. |
| Historical Tier-1 development distribution: `.\gradlew.bat :webApp:wasmJsBrowserDevelopmentExecutableDistribution --console=plain --max-workers=1` | Pass; development assets reported approximately 12.1 MiB JS, 28.9 MiB app Wasm, and 8.24 MiB Skiko Wasm; 260 actionable tasks. |
| Historical Tier-1 Chromium development journey with `STREAMCORE_WEB_DISTRIBUTION=development`: `npx playwright test tests/product.spec.ts --project=chromium-1280 --grep "login, profile selection\|native credential form owns initial focus\|native credential form keeps punctuation-heavy fields separated and submits once\|profile create edit delete"` | Pass; 4/4 with one worker in 1.2 minutes. These four Tier-1 rows predate the reopened production auth/coordinator edits and are not current candidate proof. |
| Historical `npx playwright test` at commit `0709d3b` | Pass for that commit's production distribution and 84-case suite: 84/84 in 2.2 minutes across Chromium, Firefox, and WebKit at both target viewports (14 scenarios per project). Superseded by the later persistence, native-form, and WebKit-driver changes below; it is not a current full-suite result. |
| `:client:tmdb:data:testAndroidHostTest` and `:client:tmdb:data:wasmJsNodeTest` | Pass; live-matching HTTP 401/status-code-30, malformed HTTP-401, and HTTP-200/code-30 payloads map to authentication |
| `npm run test:live-auth` with all live variables explicitly absent | One test skipped; valid-credential smoke was not run and is not counted as pass |
| Focused TMDB Android/Wasm plus `:webApp:wasmJsBrowserTest` after guarded-removal fix | Pass; null/blank display name, account-null, repeated clear, and absent selected-profile removal covered |
| Focused production persistence scenarios after guarded-removal fix | Pass; 4/4 across Chromium and WebKit for WebLocalStorage and forced WebSessionStorage fallback |
| Expanded 96-case Playwright matrix | Initial run 95/96; all WEB-02 product/persistence cases passed. The inherited WebKit-1920 external-popup test missed an immediate fixed-coordinate click. Its Playwright-only driver now resolves semantic bounds, hover-settles, and sends a real canvas click; the single corrected case passed 1/1. Per fast-feedback protocol, the full matrix was not rerun and no 96/96 claim is made. |
| Focused native-login dev matrix | Pass; 6/6 across Chromium, Firefox, and WebKit at both viewports. Native Tab focus, password visibility state, Enter submission, punctuation-heavy field separation, exact intercepted JSON values, and navigation to Profiles were asserted. |
| Post-native-form production browser matrix at commit `28d8fbf` | Initial run 100/102 pass in 9.8 minutes. Every dedicated native-login, persistence, CRUD, runtime, and Chromium/Firefox case passed. The two broad WebKit journeys completed their route/persistence behavior but failed the terminal page-error assertion on hard-reload coroutine noise and an XML resource served without an explicit MIME. Historical corrected WebKit journeys passed 2/2; no current full rerun or 102/102 claim is made. |
| `run-live-auth.ps1` local launcher | Offline pass: PowerShell parser reports zero syntax errors; mixed-case colon/equal synthetic files with punctuation produced all configured booleans plus exact child-environment match; duplicate aliases and blank values were rejected; real ignored files reported all required booleans true; Playwright `--list` discovered exactly one live smoke. No browser, server, test, or live request ran. |
| Historical authorized `run-live-auth.ps1` acceptance smoke | Pass once against wrapper commit `28d8fbf`; no retry. All preflight booleans were true. The smoke asserted exact credential-field separation, reached Profiles, restored Profiles after hard reload, captured a non-empty session from a 2xx session-creation response, and deleted that temporary session with a 2xx response in `finally`. It did not explicitly assert request-token, validation, or account-detail response statuses. No credential, token, request body, session ID, or account data was printed, captured, or committed. This predates the strengthened four-endpoint status assertion. |
| Static Playwright discovery before the final matcher edit: `npx playwright test tests/product.spec.ts --list` and `npx playwright test --config playwright.live.config.ts --list` | Pass; 54 product registrations (9 scenarios × 6 browser/viewport projects) and exactly one live-smoke registration. No server, browser, test, or live request ran. |
| Latest focused combined Gradle gate before the final WebKit matcher edit: `.\gradlew.bat :client:tmdb:data:testAndroidHostTest :client:tmdb:data:wasmJsNodeTest :feature:login:ui-web:compileKotlinWasmJs :feature:login:ui-web:compileAndroidMain :webApp:wasmJsBrowserTest --console=plain --max-workers=1` | Pass; TMDB data executed 79 Android-host plus 79 Wasm-node tests and `:webApp` executed 46 browser tests, all with zero failures/errors/skips. Login Android/Wasm compilation passed; 307 actionable tasks. |
| Latest focused development distribution before the final WebKit matcher edit: `.\gradlew.bat :webApp:wasmJsBrowserDevelopmentExecutableDistribution --console=plain --max-workers=1` | Pass; development assets reported approximately 12.1 MiB JS, 28.9 MiB app Wasm, and 8.24 MiB Skiko Wasm; 260 actionable tasks. |
| Latest Chromium-1280 development `product.spec.ts` before the final WebKit matcher edit | Pass; 9/9 with one worker in 1.4 minutes. This is focused development evidence, not candidate or visual approval. |
| WebKit-1280 development coroutine falsifier | The first anchored grep matched zero tests and is recorded as not run. The corrected literal-title grep executed one broad login/profile journey and failed 0/1 solely on the unsuppressed complete coroutine signature. The observed signature is fixed except for three decimal runtime identity values. The final test-only matcher anchors the complete message, normalizes only those decimal values, tracks restoration and expiry separately, and permits at most one normalized coroutine match plus at most one exact `ClassCastException` per phase. |
| Focused corrected literal-title WebKit-1280 rerun after the normalized/per-phase matcher edit | Pass; 1/1 using the development build in 19.5 seconds wall time (18.4 seconds test time). This is a focused test-only correction result; no full-matrix pass is claimed. |
| Final focused provider/web Gradle gate after session-state reconciliation: `.\gradlew.bat :client:tmdb:data:testAndroidHostTest :client:tmdb:data:wasmJsNodeTest :webApp:wasmJsBrowserTest --console=plain --max-workers=1` | Pass; TMDB data executed 80 Android-host plus 80 Wasm-node tests and `:webApp` executed 48 browser tests, all with zero failures/errors/skips. The Gradle client output was truncated, while the daemon log recorded `BUILD SUCCESSFUL in 1m 4s`; 287 actionable tasks. |
| Final focused Chromium-1280 correction sequence on the development build | The full nine-scenario product run passed 8/9; the retry scenario first exposed remounted error-attribute ownership. After that integration fix, its focused rerun failed 0/1 because Escape from the native field did not dismiss the Compose error dialog. After the lifecycle-owned browser Escape listener was added and the development distribution rebuilt, the same focused retry scenario passed 1/1 in 5.1 seconds. These are original failures plus a focused correction, not an unexecuted 9/9 claim. |
| Latest development distribution after the error-dialog correction: `.\gradlew.bat :webApp:wasmJsBrowserDevelopmentExecutableDistribution --console=plain --max-workers=1` | Pass; development assets remained approximately 12.1 MiB JS, 28.9 MiB app Wasm, and 8.24 MiB Skiko Wasm; 256 actionable tasks. |
| `.\gradlew.bat :app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin` | Pass; 355 actionable tasks |
| `.\gradlew.bat verifyDesignTokensLogFiles --console=plain` | Pass; 324 production files checked, zero violations |
| Combined `:app:compileTmdbDebugKotlin`, `:app:compileClientBDebugKotlin`, and root `check -PverifyDesignTokensLogFiles=true --continue --max-workers=1` | Pass; authenticated configuration preflight values redacted; 1,986 actionable tasks |
| Static commonMain/provider/security scans plus `git diff --check` | Pass; no Android/TV/provider DTO boundary imports, credential/token URL use, sensitive logging, or whitespace errors |

The root check retains the accepted WEB-01/KMP inventory. The three web modules intentionally contain no `commonTest` source sets; portable state,
action identity, and Compose semantics are exercised in the executable `:webApp` test target. `runComposeUiTest` does not provide the DOM interop
container required by `HtmlElementView`, so native input/action/dialog behavior is verified in real Playwright browsers rather than hidden behind a
production test-mode flag.

The rows above are an evidence ledger, not a cumulative current pass. Changes after each scoped run supersede that run for current-tree
acceptance. Current Tier-3 candidate status is **not run**: there is no fresh production/Binaryen distribution or complete Playwright matrix after
the reopened production changes. Current strengthened live status is **not run**. Current-tree visual review status is **not run**.

### Queued falsifiers

- Non-live: The corrected literal-title WebKit-1280 development rerun executed exactly one test and passed 1/1. Any second normalized coroutine match
  in restoration or expiry, any second exact `ClassCastException` in either phase, or any nonmatching error remains fatal. The credential-free
  Playwright matrix must still falsify mocked endpoint sequencing, browser interaction, persistence, and narrowly scoped WebKit handling. A fresh
  production distribution and full matrix are required before any current full-pass or visual-approval claim.
- Live (requires explicit authorization): the strengthened smoke must observe a 2xx response for each exact auth/account endpoint, reach Profiles,
  restore after reload, capture a non-empty session, and confirm cleanup from `finally` through a 2xx JSON response with `success: true`. Any missing
  endpoint, non-2xx endpoint, route/restoration failure, absent session, cleanup transport failure, non-2xx response, malformed body, or false/missing
  cleanup success falsifies acceptance; the reported cleanup error remains fixed and redacted.

## Known limitations and follow-up

- Login and profile password-manager/autofill integration remains unproved and is not advertised. Native autocomplete hints are present; browser-
  specific autofill UI was not asserted.
- The historical authorized live acceptance smoke passed once on `28d8fbf` within the narrower assertions documented above. The new exact
  four-endpoint 2xx assertion remains unrun with live credentials. The opt-in smoke remains isolated from the mocked matrix and still requires
  explicit action-time authorization for any future invocation.
- TMDB profile CRUD is account-scoped local browser persistence across reloads. WEB-02 does not remotely synchronize those profiles with TMDB or
  another provider; remote profile synchronization remains dependent on a future provider implementation.
- WEB-01's WebKit synchronous image decoder remains in effect and retains its documented main-thread allocation cost; WEB-03 owns replacement/performance follow-up.
- Runtime performance measurement was intentionally skipped at the user's request. Production webpack still reports its existing large-bundle
  warnings; this ticket makes no performance-regression claim.
- `/authenticated` is intentionally temporary. WEB-03 replaces it with Home/browse surfaces without changing the frozen `StreamCoreWeb*` interaction vocabulary.
