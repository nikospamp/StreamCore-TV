# WEB-04B — Implement Web Player UI

## Dispatch

- **Depends on/base:** exact reviewed `WEB-04_CONTRACT_FREEZE_COMMIT` published by WEB-04A; verify `HEAD`. Do not branch from WEB-04A's moving engine
  head or from WEB-03F.
- **Branch:** `codex/web-04b-player-ui`.
- **Concurrency:** may run with WEB-04A engine work and WEB-04C while the root remains active; all executions use the serialized queue.

## Ownership and Boundary

Owned: `feature/player/ui-web/**`, including backend-free fake-session fixtures, showcases, and local UI tests. Forbidden: `playback/api/**`,
`playback/web/**`, `webApp/**`, root/settings/catalog/convention files, shared `ui-common` production contracts, providers/data, E2E/release docs,
credentials, unreserved screenshots, and outputs.

Consume `PlayerViewModel`, `PlayerAction`, `PlayerUiState`, `PlaybackRequestModel`, the frozen playback interfaces, and app-owned web components. The UI
remains backend-agnostic; no Shaka, DOM element, TMDB, DTO, SDK, response, or client-specific type crosses into screen/state APIs.

## Expected API and Scope

- `WebPlayerRoute(request: PlaybackRequestModel, onBack: () -> Unit, viewModel: PlayerViewModel)` owns web lifecycle/fullscreen callbacks and closes
  through the shared ViewModel/session policy.
- Stateless `WebPlayerScreen(state: PlayerUiState, videoSurface: PlaybackVideoSurface?, onAction: (PlayerAction) -> Unit, modifier)` is the first
  public screen composable and has backend-free previews/showcases.
- TV-like video surface and large play/pause, seek, timeline/time, settings, quality/audio/subtitle, speed, resize, retry, and fullscreen controls.
- Arrow/Enter/Space/Escape and mouse movement/hover must not interfere with focused text/select controls. Auto-hide uses shared ViewModel policy;
  focus restores after settings/fullscreen, and Escape/Back exits exactly one layer.
- Render explicit autoplay-activation, preparing/buffering/ended, recoverable/fatal error, no-filmstrip, tracks, long-text, and controls-hidden states.
  Stable keys/content types and remembered resources prevent avoidable recomposition/allocation.

Use a fake `PlaybackSession`/surface only. Do not integrate the real engine or app route.

## Focused Verification and Evidence

```powershell
.\gradlew.bat :feature:player:ui-web:compileKotlinWasmJs
.\gradlew.bat :feature:player:ui-web:wasmJsBrowserTest
```

Through the build queue, cover semantic actions, focus graph, settings return, autoplay activation, error/retry, controls auto-hide, no-filmstrip, and
fullscreen callback using fakes. Tier 1 permits one development Chromium 1280×720 fake-player journey. Record test counts, recomposition/allocation
review, and inspected screenshots if generated. No Binaryen, complete matrix, real media/live credentials, Safari, Android/root, or visual overclaim.

Acceptance is a reviewed commit limited to reserved paths and the frozen API. WEB-04D owns real engine/app/release claims.
