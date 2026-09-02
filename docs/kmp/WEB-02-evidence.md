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
- Lazy profile content uses stable profile IDs and explicit content types.

Shared screen/ViewModel contracts were not changed. Android UI public APIs were not changed.

## Input, focus, and accessibility contract

- Pointer hover changes lift/scale; click activates the same action as keyboard activation.
- Tab uses Compose focus order. Login and profile routes request deterministic initial focus.
- Arrow keys have explicit login field, profile row, editor action, and dialog focus edges.
- Enter/Space activate Compose button/card semantics.
- Escape cancels the active dialog/editor or exits profile manage mode.
- Modal cancel/confirm controls form a focus loop; dismissal restores the originating profile card.
- Focus is visible through geometry plus a high-contrast outer border, not color alone.
- Foundation lazy/scroll containers bring focused children into the visible viewport.
- Fields expose email/password platform content semantics through their keyboard types; errors, roles, disabled/loading state, dialog descriptions, and profile labels are present in Compose semantics.

Compose canvas fields still do not project stable ordinary DOM form controls. Browser password-manager/autofill behavior is therefore **not claimed**. WEB-01's selector limitation remains authoritative.

## Startup, session, and history routing

- Missing/invalid session: replace browser history with `/login`.
- Valid session without a valid persisted profile: `/profiles`.
- Valid session with a persisted profile ID that still exists: `/authenticated`, the temporary landing surface for WEB-03.
- Create/edit routes are `/profiles/new` and `/profiles/{profileId}/edit`; route payloads contain IDs only.
- Selected profile ID is stored in the existing official TMDB browser DataStore. Reload validates the TMDB session and profile membership before restoring the landing route.
- Session expiry clears TMDB auth preferences (including the selected-profile key), replaces the route with Login, and prevents Back/Forward from resurrecting authenticated state.
- WEB-01 diagnostics remain available at `/diagnostic`; the previous Details/Player ID probes remain diagnostic-only.

## Browser screenshots and Android TV comparison

Deterministic, credential-free frames are committed under `webApp/e2e/screenshots/` for Chromium, Firefox, and WebKit at both `1280x720` and `1920x1080`:

- `<engine>-<width>-login.png`
- `<engine>-<width>-profiles.png`

The frames were visually compared with `TvLoginScreen`, `TvProfilesScreen`, `TvProfileTile`, and `TvProfileEditorScreen` plus the shared design reference. The web result retains the dark cinematic canvas, shared landscape artwork, left-side login panel, restrained ember accent, large profile imagery, 10-foot typography, and pronounced TV-like focus geometry. Browser layout density and Material3 text-field rendering differ intentionally from TV Material. This is a hierarchy/input-language comparison, not a renderer-level pixel-equality claim.

## Verification

All commands were run from the WEB-02 worktree unless a subdirectory is shown.

| Command | Result |
|---|---|
| `.\gradlew.bat :core:ui-web:compileKotlinWasmJs` | Pass |
| `.\gradlew.bat :feature:login:ui-web:compileKotlinWasmJs` | Pass |
| `.\gradlew.bat :feature:profiles:ui-web:compileKotlinWasmJs` | Pass |
| `.\gradlew.bat :webApp:wasmJsBrowserDistribution` | Pass; production assets remain approximately 1.29 MiB JS, 5.11 MiB app Wasm, and 8.24 MiB Skiko Wasm |
| `.\gradlew.bat :webApp:wasmJsBrowserTest` | Pass; 30 tests, zero failures/errors/skips (WEB-01 23 + WEB-02 7) |
| `npm ci` in `webApp/e2e` | Pass; 3 packages, 0 vulnerabilities |
| `npx playwright install` | Pass |
| `npx playwright test` | Pass; 66/66 across Chromium, Firefox, and WebKit at both target viewports |
| `.\gradlew.bat :app:compileTmdbDebugKotlin :app:compileClientBDebugKotlin` | Pass; 355 actionable tasks |
| `.\gradlew.bat verifyDesignTokensLogFiles --console=plain` | Pass; 314 production files checked, zero violations |
| `.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --max-workers=1 --console=plain` | Pass; 1,985 actionable tasks |
| Static commonMain/provider/security scans plus `git diff --check` | Pass; no Android/TV/provider DTO boundary imports, credential URL/log use, or whitespace errors |

The root check retains the accepted WEB-01/KMP inventory: 201 common Android host tests across 24 KMP test modules and 265 complete Android XML tests when app flavor and Android-only suites are included. The three new web modules intentionally contain no `commonTest` source sets; browser semantics live in the executable `:webApp` test target, so no zero-test regression is hidden.

## Known limitations and follow-up

- Browser password-manager/autofill integration is unproved for Compose canvas fields and is not advertised.
- TMDB's current profile repository is process-local provider behavior; WEB-02 persists and validates selected-profile identity, while provider-backed cross-process profile mutation persistence remains dependent on a real provider implementation.
- WEB-01's WebKit synchronous image decoder remains in effect and retains its documented main-thread allocation cost; WEB-03 owns replacement/performance follow-up.
- `/authenticated` is intentionally temporary. WEB-03 replaces it with Home/browse surfaces without changing the frozen `StreamCoreWeb*` interaction vocabulary.
