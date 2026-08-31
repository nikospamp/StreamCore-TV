# WEB-04 — Implement Web Playback and Produce the Release Artifact

## Goal

Implement provider-neutral browser playback for the existing public DASH source, integrate TV-like web player controls, complete cross-browser
verification, and produce the final static production artifact.

## Context

Android Media3 remains untouched. The browser implementation satisfies the existing shared playback contracts through Shaka Player `5.2.3` and a real
`HTMLVideoElement` embedded with Compose Multiplatform `HtmlElementView`.

The current TMDB source is public Sintel DASH with no DRM metadata. DRM, ClientB web playback, casting, offline video, and mobile web are not part of
this ticket.

## Dependencies and Parallelization

- **Depends on:** WEB-03 accepted browse milestone.
- **Final gate:** First web release artifact.
- **Parallelization:** One playback/integration owner. Tests/documentation may be delegated after the playback contract is stable.

## In-Scope Modules

- New `:playback:web`.
- New `:feature:player:ui-web`.
- Web player destination and transition from Details/Home.
- Shaka JS interop, HTML video surface, browser media/fullscreen APIs.
- Cross-browser player tests and production distribution documentation.

## Non-Goals

- No DRM/Widevine/FairPlay/PlayReady.
- No ClientB web playback.
- No PiP, casting, offline download, ads, live TV, or mobile browser support.
- No canvas-based frame extraction for filmstrips.
- No hosted deployment or backend/BFF.

## Implementation Tasks

1. Consume the Shaka Player `5.2.3` ESM interop shape proven by WEB-01: direct `@JsModule` declarations when compatible, otherwise the committed ESM
   adapter imported through `@JsModule`. Do not add a global `<script>` fallback.
2. Create `:playback:web` implementing:
    - `PlaybackSessionFactory`.
    - `PlaybackSession`.
    - `PlaybackVideoSurface`.
3. Embed an `HTMLVideoElement` through `HtmlElementView`. The element must be owned by one session, sized by Compose, and removed on close.
4. Create/destroy one Shaka instance per playback session. Install listeners once and remove every listener during cleanup.
5. Map browser/Shaka state into `PlaybackEngineState`:
    - Idle, preparing, buffering, ready, ended, and error phases.
    - Playing state, position, duration, buffered position, aspect ratio, speed, resize mode.
    - Video/audio/text tracks and selected IDs.
6. Implement commands with the existing semantics:
    - Prepare and start position.
    - Play/pause/replay.
    - Seek and scrub.
    - Playback speed from `0.5x` through `2.0x`.
    - Video quality, audio, and subtitle selection/disable.
    - Retry and resize modes.
7. Handle autoplay rejection as a recoverable ready/paused state requiring an explicit user action. Do not classify expected browser policy as a fatal
   playback error.
8. Map Shaka/network/media failures to `PlaybackErrorModel` without exposing tokens, URLs containing credentials, or raw exception dumps in UI.
9. Implement filmstrips only when Shaka exposes manifest thumbnail/image tracks. Otherwise return no frames and render the existing no-preview scrub
   behavior. Do not capture cross-origin video frames with canvas.
10. Create `:feature:player:ui-web` with TV-like controls:
    - Large play/pause, seek, timeline, time, settings, track, speed, resize, retry, and fullscreen controls.
    - Mouse movement/hover and keyboard focus.
    - Arrow/Enter/Space/Escape shortcuts that do not interfere with focused text/select controls.
    - Auto-hide controls using the shared ViewModel policy.
11. Persist/resume progress through browser DataStore and preserve profile isolation/completion policy.
12. Support browser fullscreen and restore focus/control state when fullscreen exits.
13. Ensure cleanup closes the shared ViewModel session, Shaka instance, event listeners, DOM node, ticker/filmstrip jobs, and StateFlow collectors.
14. Replace WEB-03's placeholder with the functional player route. Direct player reload must resolve required content by IDs or return safely to
    Details when it cannot.
15. Add unit/fake-player tests and Compose UI Test v2 coverage for player state/control/focus behavior. Add Playwright browser-level journeys, using
    WEB-01's selector strategy, for successful playback, autoplay rejection, network error/retry, seek, track selection, resume, fullscreen, and
    repeated enter/exit.
16. Generate the final static distribution and document:
    - Output directory and artifact contents.
    - `/config.json` deployment contract.
    - Required MIME types for Wasm/JS/assets.
    - TMDB/media/image CORS expectations.
    - Suggested CSP directives for Fetch, images, media, workers, and the selected Shaka ESM/direct-adapter import.
    - Cache policy and versioned asset behavior.
    - Static-server fallback/routing requirements.
17. Run automated Chromium/Firefox/WebKit coverage. Require a manual pass on current Safari/macOS before declaring release acceptance.

## Public API or Type Changes

- Add web implementations of existing playback interfaces; shared interfaces should not change unless a missing provider-neutral capability is proven.
- Add web Player Route/Screen APIs.
- If a shared playback contract must change, it must remain implementable by Media3 without browser types and include Android regression tests.

## Verification Commands

```powershell
.\gradlew.bat :playback:web:compileKotlinWasmJs
.\gradlew.bat :feature:player:ui-web:compileKotlinWasmJs
.\gradlew.bat :webApp:wasmJsBrowserTest
.\gradlew.bat :feature:player:ui-common:testAndroidHostTest
.\gradlew.bat :webApp:wasmJsBrowserDistribution
Set-Location webApp/e2e
npm ci
npx playwright test
```

Then run Android regression verification from the repository root:

```powershell
.\gradlew.bat :playback:media3:compileDebugKotlin
.\gradlew.bat :feature:player:ui-mobile:testDebugUnitTest
.\gradlew.bat :app:assembleTmdbDebug
.\gradlew.bat :app:assembleClientBDebug
```

## Test Scenarios

- Public Sintel DASH prepares and plays on all automated browser engines.
- Autoplay denial exposes an explicit Play action and succeeds after activation.
- Buffering/ready/playing/ended/error phases map correctly.
- Seek, scrub, speed, resize, quality, audio, subtitles, retry, and fullscreen update shared state.
- Progress is persisted per profile and resumes after reload.
- Unsupported/no-thumbnail manifests retain usable scrubbing without filmstrip frames.
- Media/network failure is recoverable and does not leak configuration.
- Repeated open/play/close cycles do not retain video DOM nodes, Shaka listeners, sessions, jobs, or timers.
- Browser Back/Escape exits player once and returns to Details with focus restored.
- Direct invalid player URL returns safely rather than crashing.
- Android Media3 player behavior remains green after any shared-contract adjustment.

## Acceptance Criteria

- The TMDB public DASH source plays end to end in Chromium, Firefox, WebKit, and a manually verified current Safari.
- Shared PlayerViewModel controls the web implementation through the provider-neutral interfaces.
- Compose UI tests cover node/state/focus behavior, while Playwright follows the canvas-safe strategy documented by WEB-01.
- Progress resume, error/retry, cleanup, and keyboard/mouse control tests pass.
- `wasmJsBrowserDistribution` produces a deployable static artifact with no real config/token.
- Deployment documentation is complete without selecting or configuring a host.
- Android TMDB and ClientB player builds remain green.
- Deferred DRM/ClientB/PiP/casting/offline/mobile work is explicitly documented and not partially stubbed as production behavior.

## Handoff Checklist

- [ ] Shaka/HTML interop architecture documented.
- [ ] Direct `@JsModule` or ESM-adapter choice matches the WEB-01 spike and CSP documentation.
- [ ] Playback state/command mapping documented.
- [ ] Cleanup ownership and leak-test results included.
- [ ] Browser automated and Safari manual results included.
- [ ] Android player regression results included.
- [ ] Production artifact path and config/CORS/CSP requirements included.
- [ ] Deferred playback capabilities listed.
- [ ] Final working tree is clean after committing this ticket.
