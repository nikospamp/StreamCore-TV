# WEB-03A — Freeze Browse Contracts and Integration Shell

## Dispatch Gate and Immutable Base

- **Depends on:** accepted, merged WEB-02.
- **Branch:** `codex/web-03a-contract-shell`.
- **Immutable base:** `eb5467a661fed227f80f70bd3f8a2e91c52bf102`, the accepted WEB-02 merge. The root must verify `HEAD` equals this exact SHA before edits.
- `2267306e5df827deded96addd91b6ec947e9f655` remains the older accepted WEB-01 rollback point, not an authorized implementation base for this ticket.

Stop if the base is missing, status contains anything except explicitly preserved user-owned files, or WEB-02's shared web API is not reviewed.

## Goal

Create the browse module shells and freeze the shared integration contract before parallel feature work: top-level chrome, browser-safe routes,
history/deep-link behavior, focus-restoration token, backend-free fixtures, feature slots, and a clearly labelled non-playing Player placeholder.

## Path Reservation

Owned exclusively by the integration owner:

- `settings.gradle.kts`, `gradle/libs.versions.toml`, and root/convention build files only when the module shells prove a change necessary.
- `core/ui-web/**`.
- `webApp/build.gradle.kts` and `webApp/src/wasmJsMain/**`.
- `webApp/src/wasmJsTest/**` only for route/shell/history contract tests.
- `feature/home/ui-web/build.gradle.kts`, `feature/search/ui-web/build.gradle.kts`, `feature/library/ui-web/build.gradle.kts`, and
  `feature/details/ui-web/build.gradle.kts` as empty source-set-capable module shells.
- This ticket's evidence document if the root reserves one.

Forbidden: feature production/test sources below the four `ui-web/src/**` trees, `webApp/e2e/**`, provider/data implementations, shared feature
ViewModels/contracts, Android UI, playback implementation, credentials, generated config, screenshots, and build outputs.

## Contract to Freeze

- Extend `WebRoute` with exact address-bar shapes `/home`, `/search`, `/library`, `/details/{contentId}`, and temporary
  `/player/{contentId}` while retaining WEB-02's login/profile routes. IDs use the existing browser-safe validation; invalid/protected routes resolve
  through the authenticated coordinator without exposing stale content.
- `WebProductShell` (or its reviewed replacement) owns top chrome, profile/logout actions, protected-route routing, and feature slots. Feature modules
  do not import `:webApp`.
- Freeze a backend-agnostic `WebBrowseFocusKey(destination, sectionKey, itemKey)` value contract and shell callbacks to capture/consume it across
  Details return. Do not put `ContentModel`, provider objects, DOM nodes, or `FocusRequester` in browser history.
- Freeze feature entry points named `WebHomeRoute`, `WebSearchRoute`, `WebLibraryRoute`, and `WebDetailsRoute`, mirroring existing shared ViewModel
  inputs/actions. Navigation callbacks may receive `ContentModel`/`PlaybackRequestModel` at composition boundaries, while address-bar state stores
  IDs only. Screens remain stateless and preview-friendly.
- Freeze app-owned web chrome/card/action/input primitives in `:core:ui-web`. Search's native text adapter is owned by WEB-03C and consumes the
  frozen value/change/submit/focus contract; no provider or `:webApp` dependency enters `:core:ui-web`.
- Freeze deterministic backend-free fixtures/test tags for loading, content, empty, offline, error, and long-text states. Fixture APIs are test or
  showcase only and never select a provider.

Any contract change after acceptance reopens WEB-03A review and invalidates parallel branches until the root publishes a new common base.

## Focused Verification

Run through the serialized build queue, in order:

```powershell
.\gradlew.bat :core:ui-web:compileKotlinWasmJs
.\gradlew.bat :webApp:compileKotlinWasmJs
.\gradlew.bat :webApp:wasmJsBrowserTest --tests "*WebRouteTest*" --tests "*WebProductCoordinatorTest*"
```

Tier 1 may use one development Chromium 1280×720 protected-route/history smoke. No Binaryen, full browser matrix, Android/root gate, visual
approval, or live credential run belongs here.

## Acceptance and Handoff

- Read-only architecture/boundary/accessibility review accepts the frozen contracts and exact route/focus shapes.
- The four feature module shells resolve without production feature implementation; temporary Player explicitly announces playback is unavailable.
- Publish the accepted 40-character `WEB-03A_CONTRACT_COMMIT`. WEB-03B–E all branch from that identical commit.
- Report changed files, API signatures, command/count evidence, review findings, and status. Do not claim complete browse, visual parity, cross-browser
  passing, or live TMDB behavior.
