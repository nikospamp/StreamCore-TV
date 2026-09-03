# WEB-04A — Freeze Playback Contract and Implement the Browser Engine

## Dispatch and Immutable Base

- **Depends on/base:** accepted `WEB-03F_COMMIT`; the root records its exact 40-character SHA and verifies `HEAD`. No implementation may begin from a
  candidate or symbolic base.
- **Branch:** `codex/web-04a-contract-engine`.
- **Blocks:** WEB-04B/C until the contract-freeze checkpoint is reviewed.

## Ownership

Owned: `playback/web/**`; `playback/api/**` and its Android regression tests only when a missing provider-neutral capability is demonstrated;
`settings.gradle.kts`/necessary build catalog or convention files only for registering `:playback:web`; and WEB-04A engine tests/evidence.

Forbidden: `feature/player/ui-web/**`, `webApp/**`, WEB-04C fixtures/release docs, provider/data implementations, Android Media3 production code except a
minimal reviewed adaptation to an approved shared contract, credentials, generated config, screenshots, and build outputs. The playback API stays
backend-agnostic and contains no Shaka, DOM, browser, TMDB, DTO, SDK, response, or client-specific type.

## Mandatory Contract-Freeze Checkpoint

Before engine implementation, audit the existing `PlaybackSessionFactory`, `PlaybackSession`, `PlaybackVideoSurface`, `PlaybackEngineState`, media,
track, error, resize, and filmstrip contracts against browser requirements.

- Prefer no shared API change. If a capability is missing, add only a provider-neutral contract that Media3 can implement and add focused Media3/API
  regression tests in the same checkpoint.
- Create the `:playback:web` module shell and compile-time fake/stub sufficient for consumers; do not claim playback.
- Freeze the constructor/factory and ownership rule: the graph provides a `PlaybackSessionFactory`; each `PlayerViewModel` owns exactly one created
  session and closes it. Never register a session as a singleton.
- Read-only playback/API, Media3 compatibility, lifecycle/leak, and security review must accept this commit.
- The root publishes its exact 40-character `WEB-04_CONTRACT_FREEZE_COMMIT`. WEB-04B and WEB-04C branch from that exact checkpoint. Later API changes
  invalidate those branches and require a new freeze/rebase decision.

## Engine Scope and Expected API

- `WebPlaybackSessionFactory : PlaybackSessionFactory`, `WebPlaybackSession : PlaybackSession`, and
  `WebPlaybackVideoSurface : PlaybackVideoSurface`; public exposure should be no broader than composition requires.
- Consume Shaka Player `5.2.3` through the WEB-01-proven direct `@JsModule` shape or a committed module-local ESM adapter. No global script fallback.
- Own one `HTMLVideoElement` and one Shaka instance per session. Render through `HtmlElementView`; install listeners once and remove listeners, DOM
  element, Shaka instance, ticker/filmstrip jobs, and collectors idempotently on `close()`.
- Map idle/preparing/buffering/ready/ended/error, playing, position/duration/buffered position, aspect ratio, speed, resize, and video/audio/text tracks
  to existing shared state. Implement prepare/start, play/pause/replay, seek/scrub, `0.5x..2.0x`, track selection/disable, retry, and resize semantics.
- Autoplay rejection becomes ready/paused with explicit user activation, not a fatal error. Sanitize errors; no tokens, credential-bearing URLs, or
  raw exception dumps reach UI/log evidence.
- Filmstrips use manifest image tracks when available; otherwise emit no frames. Never capture cross-origin video frames with canvas.

## Focused Verification and Evidence

Run one command at a time through the serialized queue:

```powershell
.\gradlew.bat :playback:web:compileKotlinWasmJs
.\gradlew.bat :playback:web:wasmJsBrowserTest
.\gradlew.bat :playback:media3:compileDebugKotlin
.\gradlew.bat :feature:player:ui-common:testAndroidHostTest
```

Focused fake/interop tests cover state/commands, autoplay rejection, error sanitization, filmstrip absence, and repeated idempotent cleanup. Record exact
counts and leak observations. No production Binaryen, full browser matrix, Safari claim, app/root gate, visual approval, or live credentials.

Acceptance publishes both the earlier contract-freeze SHA and the reviewed `WEB-04A_ACCEPTED_COMMIT`, with API diff, Media3 impact, commands/counts,
known browser limits, and final status.
