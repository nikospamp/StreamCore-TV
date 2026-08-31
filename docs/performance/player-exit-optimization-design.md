# Player exit optimization design

## Decision

For the mobile Player Back path, restore the Activity orientation **before** `popBackStack()` and wait until Compose observes the resulting
configuration before navigating. Keep system-bar and keep-screen-on cleanup in disposal, with an idempotent orientation-restore fallback for every
non-Back removal path.

This is a sequencing change in `:feature:player:ui-mobile`. It does not pin a Details platform, alter NavHost/shared-element effects, move Media3 work
to another dispatcher, or cross the backend-agnostic data/domain/UI boundaries.

## Evidence and scope

The controlled Samsung campaign shows Player frame CPU P99 remaining roughly 57–92 ms across build/compilation cells. Representative traces locate
synchronous orientation restoration at approximately 43–62 ms and Media3 release separately at approximately 35–63 ms. Before the sequencing change,
width moved from roughly 823 to 384 dp and Details could select tablet and then mobile during the same exit.

Measured facts support separating orientation restoration from Details navigation. They do not support asynchronous Media3 release, removing shared
transitions, or freezing adaptive platform selection.

Source: [controlled Samsung final report](samsung-controlled-v2-final.md).

## Current control flow

1. [
   `MobilePlayerRoute`](../../feature/player/ui-mobile/src/main/kotlin/com/pampoukidis/streamcoretv/feature/player/mobile/player/MobilePlayerRoute.kt)
   captures `activity.requestedOrientation`, synchronously requests sensor landscape, configures transient system bars, and hides them. Separate
   effects observe lifecycle, maintain `FLAG_KEEP_SCREEN_ON`, configure PiP auto-enter, and register the PiP listener.
2. System Back dispatches `PlayerAction.BackSelected`. If settings are open, `PlayerViewModel` only closes settings. Otherwise its one-shot
   `isBackNavigationPending` guard is set, `saveProgressBestEffort()` completes, and `PlayerEffect.NavigateBack` is sent.
3. [
   `PlayerRouteEventEffect`](../../feature/player/ui-common/src/main/kotlin/com/pampoukidis/streamcoretv/feature/player/common/player/PlayerRouteEventEffect.kt),
   while STARTED, maps `NavigateBack` directly to the route `onBack`; `StreamCoreNavHost` immediately calls `navController.popBackStack()`.
4. Removing Player disposes the window effect. Inside `Compose:onForgotten`, one trace span synchronously assigns the previous requested orientation,
   shows system bars, and clears `FLAG_KEEP_SCREEN_ON`. PiP auto-enter is disabled by its own effect disposal, and the lifecycle/PiP listeners are
   removed.
5. The orientation configuration change updates [
   `rememberLoginPlatform()`](../../core/ui/src/main/kotlin/com/pampoukidis/streamcoretv/core/ui/utils/PlatformUtils.kt). At 600 dp, [
   `DetailsDestination`](../../app/src/main/java/com/pampoukidis/streamcoretv/navigation/StreamCoreNavHost.kt) switches from tablet to mobile. Today,
   Details can therefore enter as tablet during the pop transition and recompose as mobile after restoration.
6. When the Player back-stack entry is destroyed, `PlayerViewModel.onCleared()` cancels its jobs and synchronously calls `PlaybackSession.close()`. [
   `Media3PlaybackSession.close()`](../../playback/media3/src/main/kotlin/com/pampoukidis/streamcoretv/playback/media3/Media3PlaybackSession.kt) is
   idempotent, cancels extractors/scope, evicts the frame cache, then releases MediaSession and ExoPlayer. The session and its
   `Dispatchers.Main.immediate` scope are created for main/application-looper access; release must retain that thread affinity.
7. Explicit PiP is a different effect and does not navigate. On API 31+, auto-enter is enabled only while playing. `ON_STOP` treats the session as
   foreground only when the Activity is already in PiP; otherwise playback pauses. PiP changes also persist progress.

## Change shape

Introduce a small route-owned, activity-scoped window session (for example `MobilePlayerWindowSession`) or equivalent private state in
`MobilePlayerRoute`:

- Capture the prior requested orientation and the source `Configuration.orientation` before forcing landscape.
- Make `restoreOrientation()` idempotent and main-thread-only. Trace the request separately from bar/flag cleanup.
- On `NavigateBack`, disable PiP auto-enter for this explicit exit, call `restoreOrientation()`, and delay the route `onBack` until
  `LocalConfiguration` reflects the restored source orientation. If no configuration change is required, navigation may continue on the next frame.
  Use a bounded fallback so sensor/rotation-lock or multi-window behavior cannot strand Back.
- Keep the Player destination on top while restoration settles. Then invoke the existing `onBack`, preserving the current NavHost transition and
  shared effects. `DetailsDestination` still derives the live window platform; no mobile/tablet value is cached or forced.
- In `onDispose`, call `restoreOrientation()` as a no-op when the Back path already restored it, then show bars and clear `FLAG_KEEP_SCREEN_ON`. This
  fallback covers external navigation, Activity teardown, and route replacement.
- Do not change `PlayerViewModel.back()`: progress persistence remains before its navigation effect and the duplicate-Back guard remains
  authoritative.
- Do not change `PlayerViewModel.onCleared()` or `Media3PlaybackSession.close()`. Release remains exactly once on the Media3 application looper and
  remains a separately measured cost.

The timeout is a correctness fallback, not a fixed delay in the normal path. The wait should react to configuration/frame state and be cancelled with
the route. A temporary blank/portrait Details placeholder is unnecessary: Player remains the visible destination until the pop begins.

## Rejected alternatives

| Alternative                                                      | Reason not chosen                                                                                                                                               |
|------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Keep pop-first and post orientation restore after the transition | It moves Binder work but leaves Details exposed in the wrong tablet branch longer and delays correct sizing/bars.                                               |
| Cache or force `Platform.Mobile` for returned Details            | It hides this trace symptom by breaking adaptive sizing for tablets, foldables, desktop/multi-window, and future platform Players.                              |
| Move `requestedOrientation` or window APIs off Main              | Activity/window mutation is main-thread work; a dispatcher hop does not remove the synchronous platform transaction and risks lifecycle races.                  |
| Release Media3 on `Dispatchers.IO`                               | The traces separate release from orientation. ExoPlayer/MediaSession access must stay on their application looper; changing it is unsupported by this evidence. |
| Release before navigation or before progress save                | It risks losing the last valid position/surface state and does not address the measured orientation/Details overlap.                                            |
| Remove NavHost/shared transitions                                | No trace isolates shared effects as the Player exit root cause; this would broaden behavior and visual regressions.                                             |

## Focused verification

Unit/state tests:

- Repeated Back still persists once and emits one navigation effect; persistence failure still navigates once.
- Settings Back closes settings without requesting orientation restore or navigation.
- Window-session restore is idempotent; normal Back performs request-before-navigation, while disposal-only removal restores once.
- No-change configuration navigates on the next frame; matching configuration change releases the gate; timeout/cancellation cannot double-navigate.

Device/instrumentation checks:

- Phone portrait Details → Player landscape → Back under sensor rotation, rotation lock, API 30 explicit PiP, and API 31+ auto-enter. Back must not
  enter PiP; explicit PiP and home auto-enter behavior remain unchanged.
- Tablet/foldable and multi-window: returned Details uses the live window size class, including resize while Player is open. No platform value is
  retained from entry.
- System bars and `FLAG_KEEP_SCREEN_ON` restore on normal Back, external route removal, and Activity teardown.
- Progress position is present on Details and resume; playback pauses outside PiP; session/frame extractors release once and no collector count
  accumulates.
- Existing shared/navigation animation and shared-bound continuity remain visually unchanged.

## Trace acceptance

Any future focused Player→Details capture must record thermal status immediately before and after the measured action and must not pool controlled
evidence with old diagnostics.

The change is confirmed structurally when:

- the orientation-request span completes while Player is still the current destination and before `popBackStack`/NavHost Details work;
- orientation Binder work no longer overlaps `Compose:onForgotten`, `DetailsDestination`, or the selected Details transition frame;
- width may still change (expected adaptive behavior), but the first post-Player `DetailsDestination` on the phone is at the settled mobile width and
  no `TabletDetailsRoute` slice occurs;
- bar/flag disposal is separately visible and remains after the navigation gate;
- `SC.Player.persist` precedes navigation, `SC.Player.release` occurs once on Main/application looper, and `SC.Player.frameCacheBytesAfterRelease`
  remains zero;
- PiP, progress, system bars, shared effects, and platform behavior pass the checks above.

Only after those ordering assertions pass should before/after frame-duration, deadline-overrun, and readiness distributions be compared. A lower tail
is the performance confirmation; the source change and a successful build alone are not.
