# KMP-00D — Complete Tablet Details, Trailer, and Player Parity

## Goal

Make tablet Details expose trailer playback and navigate into a fully functional touch player with progress restoration.

## Context

Tablet Details loads, refreshes, mutates Like/My List, and opens recommendations. KMP-00 found two blockers: the screen omits Trailer, and
`TabletDetailsRoute` passes `onPlaySelected = {}` to `DetailsRouteEventEffect`, making Play a no-op.

## Dependencies and Parallelization

- **Depends on:** KMP-00B.
- **Blocks:** KMP-00G and final KMP-00 acceptance.
- **Parallelization:** Yes, with KMP-00C, KMP-00E, and KMP-00F. This ticket owns tablet Details and touch-player changes; the integration owner owns
  app route integration.

## In Scope

- Tablet Trailer action parity with mobile.
- Tablet Play/Resume navigation wiring.
- Tablet touch-player start, seek, pause/resume, settings, exit, and progress restoration.
- Tablet Details/Player previews, focused tests, and device evidence.

## Non-Goals

- No TV player work; KMP-00F owns D-pad playback.
- No player domain/repository/provider contract redesign.
- No new external dependency, media source, KMP target, or visual redesign.

## Implementation Tasks

1. Add `onPlaySelected: (PlaybackRequestModel) -> Unit` to `TabletDetailsRoute` and forward it to `DetailsRouteEventEffect`.
2. Pass the existing app `DetailsDestination` callback into Tablet Details and navigate through the existing typed `AppRoute.Player` contract.
3. Add a tablet Trailer action when `content.trailers.isNotEmpty()`, dispatching `DetailsAction.TrailerSelected`; use app-owned design-system controls.
4. Preserve the current external URI/error behavior from `DetailsRouteEventEffect`; do not duplicate provider URL logic in tablet UI.
5. Explicitly route `Platform.Tablet` to the existing touch player only if its interaction/layout contract is unchanged and verified at tablet
   resolution. Otherwise add `:feature:player:ui-tablet` with a tablet route/screen using the same common state/actions and playback surface.
6. Preserve orientation, system-bar, PiP, player cleanup, and progress-store semantics.
7. On player exit, return to the same Details content with Play changed to Resume when progress is resumable.
8. Add Tablet Details tests for Trailer availability/dispatch and exactly-one Play/Resume dispatch.
9. Add tablet player tests for prepare/play/pause, seek, settings pages, Back/exit, and saved-progress restoration.

## Public API or Type Changes

- `TabletDetailsRoute` gains `onPlaySelected`.
- A tablet player route/screen is added only if the existing touch-player contract cannot remain platform-neutral at tablet size.
- No domain/provider API changes.

## Verification Commands

```powershell
.\gradlew.bat :feature:details:ui-tablet:compileDebugKotlin
.\gradlew.bat :feature:details:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:details:ui-common:testDebugUnitTest
.\gradlew.bat :feature:player:ui-common:testDebugUnitTest
.\gradlew.bat :feature:player:ui-mobile:testDebugUnitTest
.\gradlew.bat :feature:player:ui-mobile:compileDebugAndroidTestKotlin
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
.\gradlew.bat verifyDesignTokensLogFiles --console=plain
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain
```

If `:feature:player:ui-tablet` is required, run its production, unit-test, and Android-test compilation explicitly and record it in the handoff.

## Required Device Journeys

- Tablet Details -> Trailer -> external destination -> Back to the same Details item.
- Details -> Play -> prepare/play -> pause/resume -> seek -> settings -> exit -> Details Resume -> resume near saved position.
- Recommendation -> Details -> Back and refresh/mutation regression checks.
- Repeat the player smoke for TMDB and ClientB graphs.

## Acceptance Criteria

- Tablet Trailer is available only when data exists and opens through the existing common effect.
- Tablet Play/Resume produces exactly one typed player navigation event and is no longer a no-op.
- Complete tablet player and progress-restoration journeys pass.
- Details/library mutations and recommendations remain green.
- Root `check`, lint, verifier, both provider graphs, and baseline tests remain green.

## Handoff Checklist

- [ ] Route/public API delta documented.
- [ ] Decision to reuse or add tablet player UI justified.
- [ ] Trailer/player/progress device evidence included.
- [ ] Test counts and verifier delta reported.
- [ ] No TV scope or unrelated refactor included.
