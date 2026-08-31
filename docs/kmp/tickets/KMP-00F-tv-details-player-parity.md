# KMP-00F — Complete TV Trailer and D-pad Player Parity

## Goal

Make TV Details expose Trailer and navigate into a production TV player whose complete control flow works with D-pad input.

## Context

TV Details loads, refreshes, mutates library state, and opens recommendations. KMP-00 found Trailer absent and `TvDetailsRoute` passes
`onPlaySelected = {}`, making Play a no-op. The app currently routes every player destination to `MobilePlayerRoute`; no `:feature:player:ui-tv`
module exists.

## Dependencies and Parallelization

- **Depends on:** KMP-00B.
- **Blocks:** KMP-00G and final KMP-00 acceptance.
- **Parallelization:** Yes, with KMP-00C, KMP-00D, and KMP-00E. This ticket owns TV Details and the new TV player module; the integration owner owns
  settings, app dependencies, and app route integration.

## In Scope

- TV Trailer action parity.
- TV Play/Resume route wiring.
- New `:feature:player:ui-tv` module with 10-foot, D-pad-native controls.
- TV player focus, settings, filmstrip, seek, exit, cleanup, and progress restoration.

## Non-Goals

- No mobile/tablet player redesign or playback engine/provider change.
- No new media source, external dependency, KMP target, autoplay behavior, or visual redesign beyond TV parity.

## Implementation Tasks

1. Add `onPlaySelected: (PlaybackRequestModel) -> Unit` to `TvDetailsRoute` and forward it to `DetailsRouteEventEffect`.
2. Add a focusable TV Trailer action when trailers exist, dispatching `DetailsAction.TrailerSelected`; keep URI/error handling in the common effect.
3. Create `:feature:player:ui-tv` with `TvPlayerRoute.kt`, `TvPlayerScreen.kt`, and narrowly named reusable TV player composables.
4. Reuse `PlayerViewModel`, `PlayerUiState`, `PlayerAction`, `PlaybackVideoSurface`, and provider-neutral playback contracts. Do not duplicate player
   business logic or import provider types.
5. Route `AppRoute.Player` by platform: mobile/tablet touch player versus `TvPlayerRoute` for TV.
6. Define deterministic D-pad order for visible controls: Back -> replay/play-pause/forward -> timeline/filmstrip -> Settings; document any directional
   shortcuts and keep focus within active overlays.
7. Support play/pause, ±10-second seek, timeline scrubbing, settings root/subpages, quality/audio/subtitles/speed/resize, Back, exit, and progress resume.
8. Settings opens with its first actionable row focused; Back returns one settings level, then closes and restores Settings-button focus.
9. Player exit releases/cleans the engine, returns to the same Details item, restores Play focus, and shows Resume for saved progress.
10. Add backend-free `@PreviewTV` previews for preparing, playing controls, paused, buffering, settings, and error states.
11. Add TV Compose focus/action tests plus common state-transition regressions.

## Public API or Type Changes

- New `:feature:player:ui-tv` module and `TvPlayerRoute`/`TvPlayerScreen` APIs.
- `TvDetailsRoute` gains `onPlaySelected`.
- App player destination becomes platform-dispatched; domain/provider contracts remain unchanged.

## Verification Commands

```powershell
.\gradlew.bat :feature:details:ui-tv:compileDebugKotlin
.\gradlew.bat :feature:details:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:player:ui-tv:compileDebugKotlin
.\gradlew.bat :feature:player:ui-tv:testDebugUnitTest
.\gradlew.bat :feature:player:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:player:ui-common:testDebugUnitTest
.\gradlew.bat :feature:player:data:testDebugUnitTest
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
.\gradlew.bat verifyDesignTokensLogFiles --console=plain
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain
```

## Required Device Journeys

- TV Details -> Trailer -> external destination -> Back to same Details/Play focus.
- Details -> Play -> prepare/play -> pause/resume -> ±10 seek -> scrub/filmstrip -> settings/subpages -> exit.
- Re-enter from Details Resume and verify restored position; Back returns to the same Details item with Play focus.
- Repeat provider smoke for TMDB and ClientB.

## Acceptance Criteria

- TV Trailer and Play/Resume are functional, focusable, and no longer no-ops.
- Complete player journey works with D-pad only at 1920x1080/API 31.
- Settings and return focus are deterministic; engine resources are released on exit.
- New TV player has previews and focused UI tests; no mobile/tablet regression.
- Root `check`, lint, verifier, both providers, benchmark, and baseline-profile assembly remain green.

## Handoff Checklist

- [ ] New module/dependency edges documented.
- [ ] D-pad control order and settings focus contract included.
- [ ] Trailer/player/progress/cleanup evidence included.
- [ ] Test counts and verifier delta reported.
- [ ] No provider leakage or unrelated playback refactor.
