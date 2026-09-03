# WEB-04B Player UI Evidence

## Scope and base

- Ticket: `WEB-04B`
- Exact contract-freeze base: `28af56e2d3bf560cf9594c6ea724e3ed510ebf9b`
- Owned implementation: `feature/player/ui-web/**`
- Owned evidence: `docs/kmp/WEB-04B-evidence.md`
- Root-owned module/build-gate registration was used only as an uncommitted verification overlay in `settings.gradle.kts` and `build.gradle.kts`;
  neither file is part of the WEB-04B accepted commit.
- Target architecture: backend-agnostic

The module consumes `PlayerViewModel`, `PlayerAction`, `PlayerUiState`, `PlaybackRequestModel`, `PlaybackSession`, and `PlaybackVideoSurface`. It contains
no Shaka, provider, DTO, SDK, response, media-element, or browser playback-engine implementation.

## Implemented API

- `WebPlayerRoute(request: PlaybackRequestModel, onBack: () -> Unit, viewModel: PlayerViewModel = koinViewModel())`
  - dispatches the shared load action;
  - forwards lifecycle foreground/background changes;
  - collects the shared state, video surface, and route effects;
  - owns document-fullscreen setup/cleanup and exits fullscreen on route disposal.
- `WebPlayerScreen(state: PlayerUiState, videoSurface: PlaybackVideoSurface?, onAction: (PlayerAction) -> Unit, modifier: Modifier = Modifier)`
  - is stateless at the business-state boundary;
  - forwards only shared typed actions;
  - consumes a provider-neutral video surface.
- `WebPlayerTestTags`
  - exposes stable selectors for browser integration fixtures without exposing browser or engine types.

## UI behavior

- TV-like 1280 x 720 playback composition with large web controls, deterministic arrow focus, hover/focus affordances, time/timeline/buffer display,
  play/pause/replay, +/-10 second seek, settings, and document fullscreen.
- Settings pages cover quality, audio, subtitles, speed, and fit/fill resize; lazy rows use stable keys and content types.
- Explicit preparing, buffering, initial ready/paused user-activation, ended, recoverable error, fatal error, scrubbing, no-filmstrip, long-text, and
  controls-hidden fixtures/previews.
- Pointer enter/move forwards `UserInteraction` without consuming child actions. Root keyboard handling consumes only Escape and hidden-control reveal
  keys. Focused settings buttons and the timeline retain their own Enter/Space/arrow behavior.
- Escape/Back exits exactly one current layer: fullscreen, settings subpage/root, error/player route.
- Focus restores to the settings invoker after panel close and to the fullscreen invoker after fullscreen exit.

## Focused browser test inventory

`wasmJsBrowserTest` defines 13 focused tests:

1. route load and Back effect through a fake session;
2. typed primary-control actions;
3. deterministic control/timeline focus graph;
4. timeline arrow scrubbing without global seek interception;
5. one-layer settings Escape and invoker focus restoration;
6. focused settings Space activation only;
7. explicit autoplay/user-activation presentation;
8. explicit preparing, buffering, and ended presentation;
9. recoverable retry versus fatal Back behavior;
10. mouse movement revealing auto-hidden controls;
11. no-filmstrip fallback with scrub time/timeline retained;
12. fullscreen callback, Escape, and focus restoration;
13. backend-free fake-session command recording.

The fixtures use only `FakeWebPlaybackSession` and `PlaybackVideoSurface`; no real source URL or engine is present.

## Verification ledger

| Command | Result | Count |
|---|---|---:|
| `git rev-parse HEAD` before implementation | PASS | exact freeze SHA |
| `git diff --check -- feature/player/ui-web docs/kmp` | PASS | no whitespace errors at first implementation checkpoint |
| `./gradlew :feature:player:ui-web:compileKotlinWasmJs` | PASS — initial root-owned execution in 1m43s | 39 actionable: 38 executed, 1 from cache |
| `./gradlew :feature:player:ui-web:wasmJsBrowserTest` (original) | FAIL — 9 assertion/time-out failures, zero errors/skips | 4/13 passed |
| First corrected `wasmJsBrowserTest` rerun | FAIL — 7 assertion/time-out failures, zero errors/skips | 6/13 passed |
| Intermediate corrected `wasmJsBrowserTest` reruns | FAIL — bounded harness/semantics corrections retained in evidence | progressed through 10/13 and 11/13 passed |
| First full focused pass | PASS — root-owned execution in 57s | 13/13 passed; 180 actionable: 19 executed, 161 up-to-date |
| `./gradlew verifyDesignTokens` | PASS — module-local `Dimens.kt` registered in the exact-path allowlist | 5 actionable: 1 executed, 4 up-to-date |
| Compose-KMP convention compatibility attempt | FAIL at `compileAndroidMain`: missing Android actuals, then unavailable Android `onPointerEvent`; Wasm compilation passed | 65 actionable, then 61 actionable |
| Corrected Wasm + Android-KMP compile | PASS in 4s after adding Android compatibility actuals | 61 actionable: 13 executed, 48 up-to-date |
| Final post-review `wasmJsBrowserTest` | PASS in 1m10s | 13/13 passed, zero failures/errors/skips; 184 actionable: 19 executed, 165 up-to-date |
| Development Chromium 1280 x 720 fake-player journey | NOT RUN | — |
| Screenshot inspection | NOT RUN — no screenshots generated | — |

The original XML report is `feature/player/ui-web/build/test-results/wasmJsBrowserTest/TEST-wasmJsBrowserTest.com.pampoukidis.streamcoretv.feature.player.web.player.WebPlayerScreenTest.xml`.
The correction retains the behavioral assertions while applying actual Compose Wasm semantics: tagged/text descendants hidden by merged nodes are
queried in the unmerged tree, fixture selection is initialized consistently, state-changing actions wait for recomposition, fullscreen exposes a
stable semantic description, and Arrow traversal is explicit for the control row and timeline. The primary action test now uses the semantic click
contract after the original focused Enter injection produced the observed two-second browser-test timeout.

The first corrected rerun accepted the explicit directional focus graph and Auto-selection fixture, then exposed two remaining harness boundaries:
browser Compose Resources complete asynchronously outside `waitForIdle()`, and popup/page state changes must be driven separately from their action
assertions. The final correction uses bounded semantic-resource waits, test-controlled settings page transitions with exact one-action assertions,
the semantic `OnClick` contract for the four primary actions, and an ancestor timeline key handler so the UI-owned scrub sequence precedes the
Slider handler and consumes the paired key-up without a duplicate completion callback.

The focused suite proves the UI's stable state tags, typed actions, Back-layer policy, and fullscreen invocation contract. It does not claim that a
synthetic Compose test key event is equivalent to a native browser Escape event or that document fullscreen exit restores focus in an integrated
browser. Those browser-owned behaviors remain explicit WEB-04D Playwright acceptance work; no focused test was disabled to hide that boundary.

## Bounded review disposition

The capped P0/P1 acceptance review blocked on three concrete issues only: the on-screen Back control collapsed fullscreen and route layers; the
module bypassed the mandated `streamcore.kmp.compose.library` convention; and root/settings verification overlays were outside WEB-04B ownership.
The accepted delta now exits fullscreen without dispatching `BackSelected`, applies the convention while retaining the executable Wasm browser-test
target, supplies Android compatibility actuals, and limits the commit to `feature/player/ui-web/**` plus this evidence. The fullscreen regression is
covered inside the passing 13-test suite. No P2 or discretionary edge-case review was opened.

## Recomposition and allocation review

- `WebPlayerScreen` receives immutable shared UI state and remembers focus requesters, interaction sources, fullscreen controller state, scrim brushes,
  and derived filmstrip item identities.
- Lazy filmstrip/settings content uses stable keys and content types; duplicate boundary filmstrip timestamps remain safe because keys include their
  stable slot index.
- Pointer handlers use `rememberUpdatedState` for the action sink, avoiding stale callbacks while keeping the root event path stable.
- No flow/session is created during recomposition. `WebPlayerRoute` obtains the ViewModel at the route boundary and the ViewModel retains the frozen
  one-session ownership policy.
- Settings row model allocation is bounded to at most five navigation rows or the small track/speed/resize set; no per-frame work or polling is
  introduced.

## Known limits and deferred integration

- The frozen state contract has no separate autoplay-rejection flag. WEB-04B renders the explicit activation prompt for the observable equivalent:
  `Ready`, paused, and at position zero. WEB-04A/WEB-04D own engine mapping and integrated validation.
- Fullscreen targets the document element because the frozen `PlaybackVideoSurface` deliberately exposes no DOM node. WEB-04D owns app-shell
  integration and final browser behavior verification.
- Filmstrip UI renders supplied contract frames and an explicit absence fallback; it never captures cross-origin video frames.
- Real engine/media, app navigation, release artifact, Playwright matrix, Safari, Android/root gates, live credentials, and visual acceptance are
  outside WEB-04B and remain not run here.
