# WEB-03 — Implement the TV-Like Browse Milestone

## Goal

Deliver a distributable TMDB web browsing client covering Home, Search, Library, and Details with Android-TV visual parity, browser history, mouse
input, and deterministic keyboard focus.

## Context

This is the independently usable browse milestone. Playback is deliberately isolated to WEB-04. The Play action may navigate to a clearly identified
temporary player placeholder, but every non-player product journey must be complete.

## Dependencies and Parallelization

- **Depends on:** WEB-02.
- **Blocks:** WEB-04.
- **Parallelization:** Feature work may be split across Home, Search, Library, and Details agents after WEB-02 freezes `:core:ui-web`. One integration
  owner exclusively owns `:webApp`, shared navigation, top-level chrome, and cross-feature focus restoration.

## In-Scope Modules

- New `:feature:home:ui-web`.
- New `:feature:search:ui-web`.
- New `:feature:library:ui-web`.
- New `:feature:details:ui-web`.
- Web top-level navigation/drawer and temporary Player destination.
- Cross-feature browser navigation, screenshots, and E2E tests.

## Non-Goals

- No media playback implementation.
- No ClientB web UI.
- No mobile/narrow layout, SEO, SSR, or DOM rewrite.
- No changes to Android TV visual behavior.
- No duplicated provider DTOs or web-specific repository implementations.

## Implementation Tasks

1. Implement the web top-level navigation shell using the Android TV information architecture:
    - Home, Search, and Library destinations.
    - TV-like drawer/rail visual treatment.
    - Browser-safe route parameters and address-bar state.
    - Profile switch and logout entry points.
2. Implement Home:
    - Hero/backdrop, metadata, primary actions, pager/indicator behavior.
    - Horizontal content rows and continue-watching progress.
    - Loading, content, empty, offline, and failure states.
3. Implement Search:
    - Search field with browser text input semantics.
    - Debounced results, discovery/trending, recent searches, removal/clear actions.
    - Loading, empty, offline, failure, and result-grid/row states.
4. Implement Library:
    - Continue Watching, Liked, and My List surfaces.
    - Profile isolation, empty categories, refresh, and failure states.
5. Implement Details:
    - Backdrop, metadata, description, cast/genres, recommendations.
    - Like/My List optimistic mutation behavior.
    - Trailer launch in a new protected browser tab.
    - Play action routed to the temporary player placeholder.
6. Use shared ViewModels/repositories and web route adapters. No UI module may call TMDB APIs directly.
7. Use Coil/Ktor images with placeholders/fallbacks. Bound requested image sizes and avoid decoding original-size artwork unnecessarily.
8. Implement large-screen interaction:
    - Arrow keys move spatially within rows and between sections.
    - Enter/Space activates.
    - Escape returns/closes the current layer.
    - Mouse hover updates focus visuals without stealing keyboard focus unpredictably.
    - Focused cards scroll into view.
    - Returning from Details restores the originating row/item where data still exists.
9. Bind browser Back/Forward and direct URL reload. Direct Details URLs must load content from repositories by IDs rather than requiring an in-memory
   `ContentModel` navigation payload.
10. Preserve stable item keys/content types and avoid per-recomposition list/resource allocation.
11. Add screen showcases and deterministic screenshots for loading/content/empty/offline/error/long-text states at 1280×720 and 1920×1080.
12. Add Compose UI Test v2 coverage for semantic node/state/focus behavior and Playwright browser-level feature journeys using only the selector/input
    strategy proven by WEB-01. Run the full browse journey across Chromium, Firefox, and WebKit.

## Public API or Type Changes

- Add platform-specific web Route/Screen composables for four features.
- Add internal browser-safe route argument types and focus-restoration state in the web shell.
- Shared feature actions, effects, UI states, repositories, and models remain unchanged.

## Verification Commands

Each feature owner runs its module compilation/tests. The integration owner runs:

```powershell
.\gradlew.bat :feature:home:ui-web:compileKotlinWasmJs
.\gradlew.bat :feature:search:ui-web:compileKotlinWasmJs
.\gradlew.bat :feature:library:ui-web:compileKotlinWasmJs
.\gradlew.bat :feature:details:ui-web:compileKotlinWasmJs
.\gradlew.bat :webApp:wasmJsBrowserDistribution
.\gradlew.bat :webApp:wasmJsBrowserTest
Set-Location webApp/e2e
npm ci
npx playwright test
```

Then run Android regression compilation from the repository root:

```powershell
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
```

## Test Scenarios

- Authenticated startup enters Home for the selected profile.
- Home rows, hero actions, progress, refresh, offline, and retry paths work.
- Keyboard and mouse traverse long rows without losing focus or leaking stale selection.
- Search debounce/cancellation, recent-search persistence, empty results, and failure mapping work.
- Library categories reflect profile-specific persisted mutations and progress.
- Details direct URL, recommendation navigation, mutation rollback, and trailer launch work.
- Browser Back returns to the originating browse state/focus when possible.
- Reload reconstructs current destination from IDs and persisted state.
- Logout clears protected route state and prevents Back from exposing authenticated content.
- Temporary Player destination is explicit and cannot be mistaken for functional playback.

## Acceptance Criteria

- Login → Profiles → Home → Search/Library → Details works against TMDB.
- Browser Back/Forward/direct URL/reload behave correctly.
- Mouse and keyboard journeys pass on Chromium, Firefox, and WebKit.
- Node-level assertions pass through Compose UI Test; Playwright does not assume canvas-rendered children are ordinary DOM nodes.
- Visual hierarchy is approved against Android TV references at both resolutions.
- Browse distribution builds independently before player work.
- No provider DTO/network call appears in web UI modules.
- Android TMDB and ClientB compilations remain green.

## Handoff Checklist

- [ ] Feature modules and owners listed.
- [ ] Navigation/deep-link route shapes documented.
- [ ] Focus restoration behavior documented.
- [ ] State/screenshot coverage listed.
- [ ] Per-feature and integrated E2E results included.
- [ ] Selector usage conforms to `docs/kmp/web-testing.md`.
- [ ] Temporary player behavior explicitly documented for WEB-04.
- [ ] Final working tree is clean after committing this ticket.
