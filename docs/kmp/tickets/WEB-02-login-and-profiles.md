# WEB-02 — Implement TV-Like Login and Profile Flows

## Goal

Deliver the first functional web product slice: TMDB login, session restoration, profile selection, and profile management using Android-TV visual
language with browser-native input.

## Context

The web client must look close to Android TV but cannot use `androidx.tv.material3`. It renders through Compose Multiplatform and uses
Material3/Foundation focus, pointer, keyboard, and scroll APIs. The supported first-release viewport is desktop at 1280×720 or larger.

## Dependencies and Parallelization

- **Depends on:** WEB-01.
- **Blocks:** WEB-03.
- **Parallelization:** One owner freezes the shared web component and interaction contracts. Login/profile stateless screens may be delegated only
  after those contracts are committed.

## In-Scope Modules

- New `:core:ui-web`.
- New `:feature:login:ui-web`.
- New `:feature:profiles:ui-web`.
- Web app navigation/composition integration for login and profile routes.
- Playwright/web screenshot test infrastructure.

## Non-Goals

- No Home/Search/Library/Details UI.
- No player.
- No Android TV component replacement.
- No mobile/narrow browser layout.
- No ClientB web graph.
- No visual redesign of Android TV reference screens.

## Implementation Tasks

1. Create `:core:ui-web` as the web platform design-system module. Add reusable components for:
    - Large TV-like buttons/text buttons.
    - Focus/hover indication and scale/elevation treatment.
    - Content/profile cards.
    - Blocking loading/error surfaces.
    - Large-screen panel/background/scrim primitives.
2. Use shared theme colors, typography, shapes, dimensions, strings, and avatar resources. Do not duplicate design tokens in feature modules.
3. Implement deterministic web input behavior:
    - Mouse hover and click.
    - Tab plus arrow-key focus movement.
    - Enter/Space activation.
    - Escape as back/cancel.
    - Visible focus independent of color alone.
    - Scroll focused content into view.
4. Create stateless web login and profile screens with static previews/showcase states for loading, content, validation error, backend error, empty,
   delete confirmation, and long text.
5. Create web routes that use the existing shared ViewModels through `koinViewModel()` and lifecycle-aware StateFlow collection.
6. Wire login actions/effects to TMDB auth. Preserve identifier/password autofill semantics and ensure password content is never logged or placed in
   URLs.
7. Wire profiles select/create/edit/delete flows and provider avatar resolution.
8. Persist auth/profile state through the WEB-01 browser DataStore. Reload must restore a valid session and route appropriately.
9. Define startup routing:
    - Missing/invalid session → Login.
    - Valid session with no selected profile → Profiles.
    - Profile selected → temporary authenticated landing route until WEB-03 provides Home.
10. Add accessibility semantics for labels, roles, states, errors, dialogs, and focus order.
11. Add Playwright infrastructure under a dedicated web E2E directory. Pin the selected stable Playwright version in its lockfile and install
    Chromium, Firefox, and WebKit test engines.
12. Capture deterministic screenshots at 1280×720 and 1920×1080. Compare against Android TV references side by side; do not require renderer-level
    pixel equality.

## Public API or Type Changes

- Add reusable `StreamCoreWeb*` component APIs in `:core:ui-web`.
- Add platform-specific Login/Profile route composables; shared Screen/ViewModel contracts remain unchanged.
- No Android UI public API changes.

## Verification Commands

```powershell
.\gradlew.bat :core:ui-web:compileKotlinWasmJs
.\gradlew.bat :feature:login:ui-web:compileKotlinWasmJs
.\gradlew.bat :feature:profiles:ui-web:compileKotlinWasmJs
.\gradlew.bat :webApp:wasmJsBrowserDistribution
.\gradlew.bat :webApp:allTests
Set-Location webApp/e2e
npm ci
npx playwright install
npx playwright test
```

Return to the repository root before running Android regression compilation:

```powershell
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
```

## Test Scenarios

- Missing config is handled by WEB-01 before login renders.
- Logged-out startup displays Login with initial focus and correct keyboard traversal.
- Invalid local validation does not call the backend and exposes accessible errors.
- Authentication failure maps to provider-specific copy; retry succeeds.
- Successful login navigates to Profiles and survives reload.
- Profiles loading/content/empty/error paths render correctly.
- Select/create/edit/delete work with mouse and keyboard.
- Delete dialog traps/restores focus and Escape dismisses it.
- Unknown/missing avatar assets use the shared fallback.
- Session expiry returns to Login and clears stale selected-profile state.
- Browser Back/Forward never exposes credentials or resurrects an invalid session.

## Acceptance Criteria

- A user can authenticate, manage/select a profile, reload, and retain a valid session.
- The slice works in Playwright Chromium, Firefox, and WebKit at both target resolutions.
- Visual hierarchy/focus treatment is approved against Android TV references.
- Stateless screens remain preview/showcase friendly and do not resolve Koin directly.
- Android TMDB and ClientB compilations remain green.
- No password/token is logged, URL-encoded, screenshotted, or committed.

## Handoff Checklist

- [ ] New modules/components listed.
- [ ] Keyboard/pointer/focus contract documented.
- [ ] Startup/session routing documented.
- [ ] Screenshot locations and reference comparison included.
- [ ] Playwright and web test results included.
- [ ] Android regression compilation results included.
- [ ] Final working tree is clean after committing this ticket.

