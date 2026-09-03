# WEB-04A Contract-Freeze Evidence

## Checkpoint status

- Ticket: `WEB-04A`
- Immutable audit base: `f9e13558b3fc1c68db89ba9715932e8db80813ae`
- Freeze document status: accepted after focused verification, capped review, and delta-only re-review
- Reviewed freeze SHA: `28af56e2d3bf560cf9594c6ea724e3ed510ebf9b`
- WEB-04B / WEB-04C status: **released to branch from exactly `28af56e2d3bf560cf9594c6ea724e3ed510ebf9b`**

This checkpoint freezes the browser playback contract and module boundary before implementation continues. The target architecture remains backend-agnostic.

## Frozen conclusions

### Playback API

The existing playback API is unchanged and sufficient for WEB-04A. No contract expansion is authorized by this checkpoint. Provider SDK types, Shaka types, browser primitives, DTOs, and client-specific models must not cross into playback API, core, domain, or feature UI contracts.

The audit conclusion is therefore:

- keep the existing playback API unchanged;
- implement the browser adapter behind that API;
- preserve the backend-agnostic boundary;
- treat any newly discovered API insufficiency as a reason to stop and review the freeze, not as implicit permission to change the contract.

### Playback-session ownership

`PlayerViewModel` owns exactly one playback session for its lifetime.

The frozen lifecycle is:

1. Create one session when the `PlayerViewModel` is constructed or first requires playback, according to the existing ViewModel construction path.
2. Reuse that same session for all playback work handled by the ViewModel.
3. Do not create a new session for recomposition, repeated media selection, route re-entry handled by the same ViewModel instance, or state updates.
4. Close the owned session synchronously from the ViewModel's terminal lifecycle callback.
5. Do not launch asynchronous cleanup from the terminal callback and do not leave cleanup to garbage collection.

Koin must expose only the playback-session factory at the module boundary. It must not register a playback session as `single`, inject a live session directly, or use Koin as a service locator inside the ViewModel.

### Source-error sanitization

Errors originating from a media source, manifest, browser engine, provider, URL, or exception must be mapped to fixed application-owned copy before entering UI-visible state or effects.

The frozen rule is:

- UI-visible source failures use fixed copy defined by the application contract;
- raw exception messages, URLs, manifests, query parameters, headers, engine payloads, and provider diagnostics are never surfaced to users;
- variable technical detail may be retained only in an appropriate non-UI diagnostic channel where existing privacy and logging rules permit it;
- sanitization occurs at the boundary that maps implementation failures into the common playback error contract.

`PlayerViewModel` now maps every engine error to fixed `PLAYBACK_FAILED` / `Playback failed.` values while preserving only recoverability. Media3
also emits fixed copy at its implementation boundary. Neither raw engine code nor message reaches `PlayerUiState`.

This avoids leaking credentials or backend/engine-specific detail while keeping feature UI backend-agnostic.

### Browser module topology

WEB-04A uses a browser-only `:playback:web` module.

The module must:

- own browser and Shaka-specific implementation details;
- expose the existing playback contract through a Koin factory only;
- avoid exporting Shaka, JavaScript, DOM, or browser-specific types;
- remain outside Android application/provider composition except where the browser composition root explicitly selects it;
- avoid moving browser implementation detail into common playback API, core, domain, or feature modules.

The project Compose convention removes Wasm test compilations. Consequently, `:playback:web` uses a browser-only direct Kotlin Multiplatform/
Compose target rather than claiming convention-provided coverage. Compose Multiplatform requires `binaries.executable()` to webpack the Skiko
runtime for a browser test (`CMP-4906`); this binary exists only to make the module's test/runtime bundle executable and is not an application or
release-distribution claim.

### WEB-01 Shaka proof boundary

The WEB-01 Shaka proof is narrow. It demonstrates only the previously recorded integration/loading capability and is not evidence that the production playback engine, lifecycle, event mapping, source handling, recovery, or disposal work is complete.

For WEB-04A:

- the prior narrow Shaka proof may be cited only for the capability it actually established;
- playback-engine implementation remains **NOT RUN** at this checkpoint;
- engine behavior, lifecycle wiring, state/event mapping, media-source integration, and browser runtime validation must not be reported as completed by this freeze document.

## Module and dependency boundary

The frozen dependency direction is:

```text
feature/player UI -> playback API <- :playback:web implementation
                                      |
                                      +-> browser/Shaka internals
```

The implementation module supplies the existing interface through the browser composition root. Common playback API and feature UI do not depend on `:playback:web` or its implementation types.

## Owned paths

WEB-04A implementation work is restricted to the paths explicitly assigned by the ticket and reviewed freeze. Record the final reviewed list here before B/C begins.

- `docs/kmp/WEB-04A-contract-freeze-evidence.md` — this evidence document
- `playback/web/**` — browser-only module shell and focused browser tests
- `settings.gradle.kts` — registration of `:playback:web` only
- `feature/player/ui-common/src/commonMain/**/PlayerViewModel.kt` — root-reserved one-session/sanitization prerequisite
- `feature/player/ui-common/src/commonTest/**/PlayerViewModelTest.kt` — focused lifecycle/security regression tests

## Forbidden paths and changes

Unless a separate reviewed change reopens the contract freeze, WEB-04A must not modify:

- the existing playback API contract;
- core, domain, or feature UI contracts to accommodate Shaka/browser implementation details; the narrow `PlayerViewModel` implementation/test
  prerequisite above changes no public type or browser-specific contract;
- provider/client DTOs, SDK models, API responses, or client-specific models outside their owning modules;
- Android playback implementations or Android composition roots;
- unrelated modules, build logic, convention plugins, dependency versions, or repository-wide configuration;
- WEB-04B or WEB-04C implementation paths before the reviewed freeze SHA exists;
- generated outputs, credential files, or ignored local configuration;
- any path not explicitly listed as owned by the reviewed WEB-04A freeze.

## Required review assertions

The reviewer must confirm all of the following before recording the freeze SHA:

- [x] Audit base is exactly `f9e13558b3fc1c68db89ba9715932e8db80813ae`.
- [x] Existing playback API is unchanged and sufficient.
- [x] Common contracts remain backend-agnostic.
- [x] One session is owned per `PlayerViewModel` lifetime.
- [x] The session is reused and synchronously closed.
- [x] UI source and engine failures use fixed-copy sanitization.
- [x] `:playback:web` is browser-only and exposes an actually executed browser-test task.
- [x] The module exposes a Koin factory only, not a live session singleton.
- [x] WEB-01 Shaka evidence is described narrowly.
- [x] Playback-engine implementation and runtime proof remain marked not run.
- [x] Owned and forbidden paths are complete and non-overlapping.
- [x] WEB-04B and WEB-04C branch only from the reviewed freeze SHA recorded above.

## Command and result ledger

| Purpose | Command | Result | Evidence SHA |
|---|---|---|---|
| Confirm immutable audit base | `git rev-parse HEAD`; `git status --short` before edits | PASS at the exact base; clean worktree | `f9e13558b3fc1c68db89ba9715932e8db80813ae` |
| Playback API/boundary audit | Read-only API, Media3, lifecycle and Wasm topology audits | PASS: no API capability gap or backend-boundary violation; one-session wording and source-error sanitization were corrected before freeze | working tree |
| Module compile | `./gradlew :playback:web:compileKotlinWasmJs --max-workers=1 --console=plain` | PASS in 1m41s; 12 actionable tasks (11 executed, 1 from cache) | working tree |
| Browser shell tests, original | `./gradlew :playback:web:wasmJsBrowserTest --max-workers=1 --console=plain` | FAIL before tests: Compose UI check required a Wasm executable binary for Skiko; 124 actionable tasks (118 executed, 6 up-to-date) | working tree |
| Browser shell tests, corrected | Same command after adding the required test executable binary | PASS in 37s; 5/5, zero failures/errors/skips; 144 actionable tasks (24 executed, 120 up-to-date) | working tree |
| One-session ownership and fixed-copy sanitization | `./gradlew :feature:player:ui-common:testAndroidHostTest --max-workers=1 --console=plain` | PASS in 44s; 13/13, zero failures/errors/skips; 27 actionable tasks (23 executed, 4 up-to-date) | working tree |
| Player Wasm compatibility | `./gradlew :feature:player:ui-common:compileKotlinWasmJs --max-workers=1 --console=plain` | PASS in 8s; 14 actionable tasks (8 executed, 6 up-to-date) | working tree |
| Media3 compatibility | `./gradlew :playback:media3:compileDebugKotlin --max-workers=1 --console=plain` | PASS in 11s; 16 actionable tasks (8 executed, 8 up-to-date) | working tree |
| Verify playback API has no WEB-04A diff | `git diff -- playback/api` | PASS: empty | working tree |
| Browser/Shaka runtime verification | `NOT RUN — root to fill` | `NOT RUN — engine work remains not run` | `NOT RUN — root to fill` |

First-review corrections are recorded separately:

| Purpose | Command | Result | Evidence SHA |
|---|---|---|---|
| Capped freeze review | Three read-only reviews | BLOCK: raw engine error propagation and retained content-A Preparing state were reproducible P1s; all other assertions passed | `6d787a909f0e6a7763677bc489e25aabf06b9242` |
| Corrected ViewModel regressions | `./gradlew :feature:player:ui-common:testAndroidHostTest --max-workers=1 --console=plain` | PASS in 11s; 15/15, zero failures/errors/skips; 27 actionable tasks (9 executed, 18 up-to-date) | corrected working tree |
| Corrected Player Wasm compile | `./gradlew :feature:player:ui-common:compileKotlinWasmJs --max-workers=1 --console=plain` | PASS in 8s; 14 actionable tasks (7 executed, 7 up-to-date) | corrected working tree |
| Corrected Media3 compile | `./gradlew :playback:media3:compileDebugKotlin --max-workers=1 --console=plain` | PASS in 8s; 16 actionable tasks (3 executed, 13 up-to-date) | corrected working tree |

The corrected Preparing path clears prior media timing, playing state, buffer, aspect ratio, tracks, selections, and error before UI/progress logic;
only speed and resize preference may carry across media. A regression emits content A's retained playing state during content B preparation and proves
zero content-B progress writes. A separate regression emits a token-bearing engine code/message and proves neither reaches UI state.

## Checkpoint decision

Decision: `ACCEPTED`.

The first capped review blocked on two P1s and no other freeze criteria. Both are closed by `28af56e2d3bf560cf9594c6ea724e3ed510ebf9b`:
delta-only lifecycle and security re-reviews returned PASS. WEB-04A engine work continues on its owning branch; WEB-04B and WEB-04C may now
start from that exact immutable checkpoint. No engine, player UI, release, production/Binaryen, full-matrix, Safari, or live-playback claim is made.

## WEB-04A engine implementation checkpoint

- Implementation base: accepted contract freeze `28af56e2d3bf560cf9594c6ea724e3ed510ebf9b`, plus the docs-only acceptance commit
  `e92cced8aa8c9f2573a4db4e25c1fa9c80db6ddf`.
- Playback API diff: empty. Media3, `PlayerViewModel`, settings, web application/UI, providers, credentials, generated output, and WEB-04B/C paths are
  unchanged by the engine implementation.
- `WebPlaybackSessionFactory` creates a distinct session-owned `HTMLVideoElement`, `ShakaWebPlaybackBridge`, `WebPlaybackSession`, and
  `WebPlaybackVideoSurface` for every `create()` call. Koin continues to register only `PlaybackSessionFactory` as a process-scoped object.
- A committed module-local `shaka-playback-adapter.mjs` imports `shaka-player/dist/shaka-player.compiled.js` statically. It has no CDN, page-script,
  or global-object fallback. Shaka objects, promises, and raw errors remain inside this adapter; the fakeable Kotlin bridge exposes only commands,
  primitive-backed snapshots, and callbacks. Filmstrip image bytes cross only as base64-encoded PNG data before decoding to Compose
  `ImageBitmap`.
- Prepare/load uses both Kotlin and adapter generation/closed guards. State mapping covers idle, preparing, buffering, ready, ended, and error;
  autoplay rejection resolves to ready/paused. Commands cover play, pause, ended replay, bounded seek, `0.5x..2.0x` speed, fit/fill resize, retry,
  video/audio selection and text selection. Quality Auto (`selectVideoTrack(null)`) re-enables ABR independently of an explicit audio override and
  reports no selected video ID while Auto is active; manual video and audio selections continue to report active-variant IDs. Text selection
  explicitly restores visibility while null selection unloads the Shaka text track and disables native text-track visibility.
- UI-visible failures use only fixed `SOURCE_MISSING` / `No playable source is available.` or `PLAYBACK_FAILED` / `Playback failed.` copy. Raw
  exceptions, manifests, source URLs, query parameters, and Shaka payloads do not cross the adapter/session boundary.
- The playback surface returns the exact session-owned video element from `HtmlElementView`. Close synchronously invalidates the generation,
  detaches the callback, clears the ticker, removes every stored DOM/Shaka listener, pauses/clears/removes the element, clears track snapshots, and
  starts exactly one caught destroy for its successfully constructed Shaka instance. Adapter lifecycle probes expose only primitive cleanup
  observations and verify both destroy resolution and caught rejection. Repeated close and late callbacks are no-ops. No internal Flow collectors
  or object URLs are created.
- Browser filmstrips query only Shaka manifest image tracks. The adapter fetches the selected thumbnail sprite, crops that image through a temporary
  canvas, releases its fetched bitmap/controller/callback state, and emits a real Compose `ImageBitmap`; absent/failed image tracks complete with
  no frames. The video element is never a canvas source.
- Focused browser tests authored: 19 tests covering factory ownership, state/commands and clamping, autoplay rejection, fixed-copy sanitization,
  missing source, active track mapping/text visibility assertions, Quality Auto with a retained audio override, generation rejection, ended replay,
  retry generation, available and absent manifest filmstrips, filmstrip cancellation, same-element release, repeated close, and adapter destroy
  resolution/rejection cleanup.

### Engine verification ledger

| Purpose | Command | Result |
|---|---|---|
| Static scope/API check | `git diff --check`; `git diff -- playback/api`; owned-path inspection | PASS before handoff; API diff empty and implementation changes restricted to `playback/web/**` plus this appended engine evidence |
| Wasm compile | `.\gradlew.bat :playback:web:compileKotlinWasmJs --max-workers=1 --console=plain` | PASS in 3s; 8 actionable tasks (6 executed, 2 up-to-date) |
| Focused browser tests, pre-review | `.\gradlew.bat :playback:web:wasmJsBrowserTest --max-workers=1 --console=plain` | PASS in 35s; 17/17, zero failures/errors/skips; 140 actionable tasks (13 executed, 127 up-to-date) |
| Post-review Quality Auto delta | Same serialized command after two focused adapter/session regressions | PASS in 30s; 19/19, zero failures/errors/skips; 140 actionable tasks (13 executed, 127 up-to-date) |
| Media3 compatibility | `.\gradlew.bat :playback:media3:compileDebugKotlin` | NOT RERUN after browser-only engine delta; accepted freeze compatibility pass remains recorded above |
| Player regression | `.\gradlew.bat :feature:player:ui-common:testAndroidHostTest` | NOT RERUN after browser-only engine delta; accepted freeze 15/15 pass remains recorded above |

Known boundary: this checkpoint makes no production Binaryen, full-browser-matrix, Safari, application/root-gate, visual, or live-playback claim.
The target architecture remains backend-agnostic.
