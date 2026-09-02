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
- Successful login clears identifier/password state before leaving the route. No credential is written to a route, log, screenshot, or committed configuration.

### `:feature:profiles:ui-web`

- `WebProfilesRoute` / `WebProfilesScreen` cover loading, content, empty, error, selection, manage, delete confirmation, and unknown-avatar fallback.
- `WebProfileEditorRoute` / `WebProfileEditorScreen` cover create/edit, avatar and maturity selection, validation, saving, deletion, and cancellation.
- The browser editor uses typed `HtmlElementView` boundaries for the display-name input and action strip. Stable DOM listeners are installed once,
  read current callbacks through `rememberUpdatedState`, and remove the exact listener instances on release.
- The delete confirmation is an `expect`/`actual` boundary: Android retains the Compose dialog; Wasm uses a native `HTMLDialogElement` for top-layer
  modality, native Tab containment, Escape cancellation, Cancel autofocus, and trigger-focus restoration (including WebKit's synchronous fallback).
- Lazy profile content uses stable profile IDs and explicit content types.

Android UI public APIs were not changed. The web editor route now distinguishes successful profile mutation from cancellation, and the profiles
route accepts a monotonic revision used to refresh its retained ViewModel after create/edit/delete.

## Input, focus, and accessibility contract

- Pointer hover changes lift/scale; click activates the same action as keyboard activation.
- Tab uses Compose/native focus order. Login and profile routes request deterministic initial focus.
- Arrow keys have explicit login field, profile row, editor action, and dialog focus edges.
- Enter/Space activate Compose button/card semantics.
- Escape cancels the active dialog/editor or exits profile manage mode.
- The editor modal uses the browser top layer and inert background. Cancel initially owns focus; arrows transfer between Cancel/Delete; Escape closes
  without leaving the editor; close restores the same native Delete trigger. A 20-cycle open/Escape scenario verifies cleanup and listener identity.
- Focus is visible through geometry plus a high-contrast outer border, not color alone.
- Foundation lazy/scroll containers bring focused children into the visible viewport.
- Fields expose email/password platform content semantics through their keyboard types; errors, roles, disabled/loading state, dialog descriptions,
  selected avatar/maturity state, and profile labels are present in semantics. The native display-name error is visible, uses alert/live semantics,
  and is referenced by `aria-errormessage` through a real element ID. Profiles load-error gives Retry deterministic initial focus.

Login fields remain Compose canvas fields, so login password-manager/autofill behavior is **not claimed**. The profile display-name editor is a native
DOM text input with `autocomplete="nickname"`; only that input's native browser behavior is claimed. WEB-01's selector limitation remains authoritative
for canvas content. Playwright uses Compose accessibility roles only to resolve semantic bounds, then drives real viewport canvas input; stable DOM
test IDs are limited to the explicit native editor controls. `docs/kmp/web-testing.md` records the exact role/name and native test-ID allowlist proven
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
- A guarded live run proved the canonical browser/request credential payload and all four TMDB calls: token creation, login validation, session
  creation, and account details returned HTTP 200. The failure occurred in the following atomic preferences transaction because TMDB's account
  display name was absent. AndroidX Preferences 1.2.1 Wasm casts the `null` result of removing an absent key to non-null `T`; project-owned
  Wasm-reachable removals are now guarded with a presence check. The earlier status-code-30 result came from a zero-delay automation focus race,
  not rejected canonical credentials. Code-30 mapping remains hardened and displays deterministic “Sign-in failed” guidance.
- WEB-01 diagnostics remain available at `/diagnostic`; the previous Details/Player ID probes remain diagnostic-only.

## Browser screenshots and Android TV comparison

Deterministic, credential-free frames are committed under `webApp/e2e/screenshots/` for Chromium, Firefox, and WebKit at both `1280x720` and `1920x1080`:

- `<engine>-<width>-login.png`
- `<engine>-<width>-profiles.png`

All 12 frames were inspected after the final matrix. Each contains complete login copy/background or complete profile copy/artwork. Login capture
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
| `.\gradlew.bat :webApp:wasmJsBrowserTest` | Pass; 34 tests, zero failures/errors/skips across 8 suites |
| `npm ci` in `webApp/e2e` | Pass; 3 packages, 0 vulnerabilities |
| `npx playwright install` | Pass |
| `npx playwright test` | Pass; 84/84 in 2.2 minutes across Chromium, Firefox, and WebKit at both target viewports (14 scenarios per project) |
| `:client:tmdb:data:testAndroidHostTest` and `:client:tmdb:data:wasmJsNodeTest` | Pass; live-matching HTTP 401/status-code-30, malformed HTTP-401, and HTTP-200/code-30 payloads map to authentication |
| `npm run test:live-auth` with all live variables explicitly absent | One test skipped; valid-credential smoke was not run and is not counted as pass |
| Focused TMDB Android/Wasm plus `:webApp:wasmJsBrowserTest` after guarded-removal fix | Pass; null/blank display name, account-null, repeated clear, and absent selected-profile removal covered |
| Focused production persistence scenarios after guarded-removal fix | Pass; 4/4 across Chromium and WebKit for WebLocalStorage and forced WebSessionStorage fallback |
| Expanded 96-case Playwright matrix | Initial run 95/96; all WEB-02 product/persistence cases passed. The inherited WebKit-1920 external-popup test missed an immediate fixed-coordinate click. Its Playwright-only driver now resolves semantic bounds, hover-settles, and sends a real canvas click; the single corrected case passed 1/1. Per fast-feedback protocol, the full matrix was not rerun and no 96/96 claim is made. |
| `run-live-auth.ps1` local launcher | Added; PowerShell parser reports zero syntax errors. File-backed preflight reports booleans only and passes values to the isolated child process; no live invocation performed. |
| `.\gradlew.bat :app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin` | Pass; 355 actionable tasks |
| `.\gradlew.bat verifyDesignTokensLogFiles --console=plain` | Pass; 324 production files checked, zero violations |
| `.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --max-workers=1 --console=plain` | Pass; 1,985 actionable tasks |
| Static commonMain/provider/security scans plus `git diff --check` | Pass; no Android/TV/provider DTO boundary imports, credential URL/log use, or whitespace errors |

The root check retains the accepted WEB-01/KMP inventory. The three web modules intentionally contain no `commonTest` source sets; portable state,
action identity, and Compose semantics are exercised in the executable `:webApp` test target. `runComposeUiTest` does not provide the DOM interop
container required by `HtmlElementView`, so native input/action/dialog behavior is verified in real Playwright browsers rather than hidden behind a
production test-mode flag.

## Known limitations and follow-up

- Login password-manager/autofill integration remains unproved for Compose canvas fields and is not advertised. The native profile nickname input is
  covered for typing/submission, but browser-specific nickname autofill UI was not asserted.
- The valid canonical credentials were proven through session/account creation, and the temporary live session was deleted successfully. A final
  post-fix live login/persistence run still requires new explicit authorization. The opt-in smoke remains isolated from the mocked matrix and must
  remain unclaimed until that authorized run completes and cleans up its session.
- TMDB's current profile repository is process-local provider behavior; WEB-02 persists and validates selected-profile identity, while provider-backed cross-process profile mutation persistence remains dependent on a real provider implementation.
- WEB-01's WebKit synchronous image decoder remains in effect and retains its documented main-thread allocation cost; WEB-03 owns replacement/performance follow-up.
- Runtime performance measurement was intentionally skipped at the user's request. Production webpack still reports its existing large-bundle
  warnings; this ticket makes no performance-regression claim.
- `/authenticated` is intentionally temporary. WEB-03 replaces it with Home/browse surfaces without changing the frozen `StreamCoreWeb*` interaction vocabulary.
