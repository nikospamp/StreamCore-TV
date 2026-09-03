# WEB-04 — Web Playback and Release Milestone Index

## Status

This is a non-executable milestone index. Do not assign it to an implementation owner. Execute
[WEB-04A](WEB-04A-contract-and-engine.md) through [WEB-04D](WEB-04D-final-integration.md) only after WEB-03F is accepted.

No immutable WEB-04 implementation base exists yet. The root records the accepted WEB-03F 40-character SHA before dispatch; a symbolic, candidate,
or unmerged commit blocks implementation.

## Aggregate Goal and Scope

Implement provider-neutral browser playback for the public Sintel DASH source, integrate TV-like web player controls, complete cross-browser and
manual Safari verification, and produce a deployable static production artifact. Android Media3 remains intact. The browser implementation consumes
the shared playback contracts through Shaka Player `5.2.3` and an `HTMLVideoElement` embedded through Compose Multiplatform `HtmlElementView`.

The milestone adds `:playback:web` and `:feature:player:ui-web`, replaces WEB-03's placeholder, exercises browser media/fullscreen APIs, and documents
the artifact, `/config.json`, MIME, CORS, CSP, caching, and static routing contracts.

Out of scope: DRM, ClientB web playback, PiP, casting, offline video, ads, live TV, mobile web, canvas frame extraction, hosted deployment, or a BFF.
Core, playback API, domain, and feature UI remain backend-agnostic and contain no browser/provider DTOs, SDKs, responses, or client-specific models.

## Executable Tickets and Integration Wave

| Ticket | Deliverable | Immutable-base rule |
|---|---|---|
| [WEB-04A](WEB-04A-contract-and-engine.md) | Contract-freeze checkpoint and Shaka/HTML engine | Accepted WEB-03F SHA; publishes contract-freeze SHA |
| [WEB-04B](WEB-04B-player-ui.md) | Player UI and fake-session tests | WEB-04A contract-freeze SHA |
| [WEB-04C](WEB-04C-release-and-test.md) | Browser fixtures, release checks and deployment docs | Same WEB-04A contract-freeze SHA |
| [WEB-04D](WEB-04D-final-integration.md) | A→B→C integration, candidate and release acceptance | Accepted WEB-03F SHA plus reviewed A/B/C commits |

WEB-04A must publish a reviewed contract-only checkpoint before engine work continues. B and C branch from that exact checkpoint and must not modify
the frozen playback contract. The integration owner merges A→B→C, owns `:webApp` and shared build/navigation files, and serializes all Gradle,
Playwright-server, webpack, and Binaryen work.

## Aggregate Acceptance

- The public Sintel DASH source prepares and plays end to end in automated Chromium, Firefox, and WebKit and in a separately recorded manual current
  Safari/macOS pass.
- `PlayerViewModel` controls the web implementation through provider-neutral `PlaybackSessionFactory`, `PlaybackSession`, and `PlaybackVideoSurface`
  contracts that remain Media3-implementable.
- Autoplay rejection is recoverable; state/commands, progress resume, tracks, fullscreen, error/retry, no-filmstrip fallback, and repeated cleanup are
  verified without leaking DOM nodes, listeners, sessions, jobs, timers, collectors, URLs, or configuration.
- Compose UI tests cover semantic node/state/focus behavior; browser tests follow the WEB-01 canvas-safe strategy.
- One production `wasmJsBrowserDistribution` yields a deployable static artifact with no real token/config. Deployment documentation covers the
  artifact, `/config.json`, MIME, CORS, CSP, caching, and fallback routing without selecting a host.
- Android TMDB/ClientB and Media3 regression gates remain green. Deferred capabilities are documented, not production stubs.

No live credential use is allowed during A–C. WEB-04D may use only the final-gate workflow after every non-live gate is ready. Automated browser
evidence is not a manual Safari pass, screenshot existence is not visual approval, and focused reruns are not complete-matrix passes.
