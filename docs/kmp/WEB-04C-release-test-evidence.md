# WEB-04C release/test evidence

## Status

- Ticket: `WEB-04C`
- Immutable contract-freeze base: `28af56e2d3bf560cf9594c6ea724e3ed510ebf9b`
- Implementation status: structurally prepared, syntax-checked, and accepted as the WEB-04C test/release input
- A/B production integration: **NOT PRESENT** on this branch
- Production artifact validation: **NOT RUN**; the expected production output was absent during read-only inspection
- Playwright browser scenarios: **NOT RUN**
- Current Safari/macOS: **NOT RUN**
- Live TMDB or public-media journey: **NOT RUN**
- WEB-04C accepted commit: published by the integration-owner handoff after this evidence commit

The target architecture remains backend-agnostic. No production Kotlin/resource, playback, feature, core, build-logic, credential, real-config,
generated distribution, browser binary, node-module, or screenshot file was changed.

## Observed facts

- `git rev-parse HEAD` returned the exact required 40-character contract-freeze base above before edits.
- The frozen branch contains the WEB-03 player placeholder, not the WEB-04A engine or WEB-04B web player UI.
- Source resources define `index.html`, `config.example.json`, and the module-local `shaka-adapter.mjs`.
- Existing browser testing documents prove that projected Compose semantics are geometry/discovery contracts: pointer interaction must target their
  resolved bounds, while native `HtmlElementView` controls may use explicit DOM selectors.
- No build, npm, Playwright, browser, Gradle, media, or credential command was run for this slice.
- `node --check webApp/e2e/server.mjs` and `node --check webApp/e2e/validate-release-artifact.mjs` passed; `git diff --check` passed.

These observations are not runtime passes.

## Prepared deliverables

### Player E2E plan

`webApp/e2e/tests/player.spec.ts` defines 10 scenarios across the existing six browser/viewport projects (60 planned project-test invocations):

1. successful fake playback and user play activation;
2. autoplay rejection followed by explicit activation;
3. sanitized recoverable network failure and one retry;
4. seek/scrub, speed, quality, audio, and subtitle selection;
5. changed progress persisted across close/reopen and hard reload for one profile, with a second-profile isolation check;
6. fullscreen exit and focus restoration;
7. one-layer-at-a-time Escape behavior and browser Back return;
8. explicit diagnostic invalid direct-ID fixture with zero session and zero prepare count;
9. no-filmstrip seek fallback and zero canvas frame capture;
10. five enter/play/close cycles with zero retained session, video node, listener, or timer.

The describe block is skipped unless `STREAMCORE_WEB_PLAYER_FIXTURES=enabled`. On the current placeholder branch these cases therefore cannot be
reported as passing. WEB-04D may enable the flag only after integrating all fixture selectors/state below and proving their exact projection.

The planned interactions use exact role/name projection plus settled semantic bounds for Compose controls; they never call DOM `click()` on a
projected Compose node and never use fixed coordinates. The video uses the native `[data-testid="player:video"]` contract. Readiness and assertions
use explicit, non-sensitive `body[data-player-*]` state. Browser error attachments redact URLs, bearer values, and session query values.

### Fixture integration assumptions

WEB-04D must reconcile these assumptions against accepted WEB-04A/B rather than silently weakening assertions:

- Existing diagnostic route `/diagnostic/player/603?fixture={scenario}` hosts a backend-free fake session for `success`, `autoplay-blocked`,
  `recoverable-error`, `tracks`, `resume`, and `no-filmstrip`.
- `/diagnostic/player/invalid?fixture=invalid-direct-id&requestedId=%24invalid` treats the requested ID as rejected input, sets
  `data-player-invalid-request="true"`, and exposes zero `data-player-active-sessions` and zero `data-player-prepare-count` without creating a video.
- `body[data-player-fixture]` and `body[data-player-fixture-ready="true"]` are set only after the fixture and UI are stable.
- The hosted video is the one native `HtmlElementView` node with `data-testid="player:video"`.
- Exact projected actions are unique on their active layer: `Play`, `Retry`, `Playback settings`, `Speed`, `1.5x`, `Quality`, `1080p`, `Audio`,
  `Greek`, `Subtitles`, `English`, and `Fullscreen`; the timeline projects one `slider` named `Playback position`.
- Non-sensitive fixture state uses `data-player-phase`, `data-player-playing`, `data-player-activation-required`, `data-player-error-code`,
  `data-player-error-message`, `data-player-prepare-count`, `data-player-position-ms`, `data-player-speed`, `data-player-video-track`,
  `data-player-audio-track`, `data-player-text-track`, `data-player-fullscreen`, `data-player-focused-action`, `data-player-layer`,
  `data-player-filmstrip-count`, and `data-player-canvas-capture-count`.
- Cleanup counters remain inspectable on the diagnostic details route through `data-player-active-sessions`, `data-player-active-listeners`,
  `data-player-active-timers`, and `data-player-close-count`; they contain counts only and reset at browser-context start.
- Escape from settings closes only settings; Escape from the base player returns to diagnostic details. Product-route Back/Escape behavior still
  requires WEB-04D integration coverage.
- The resume fixture accepts synthetic `profile=profile-a` and `profile=profile-b`, exposes the active synthetic ID through
  `data-player-profile-id`, starts without progress, persists a scrubbed position on close, restores it for the same profile across reopen/reload,
  and exposes no inherited position for the other profile. It uses the real profile-isolated progress repository with no provider DTO or account
  value.

If the accepted A/B semantics differ, update this planned test and `docs/kmp/web-testing.md` together, prove the replacement selector contract in
all six projects, and record it as an integration-only reconciliation. Do not enable the fixture flag with missing attributes or placeholder UI.

### Artifact validator

`webApp/e2e/validate-release-artifact.mjs` checks the configured production output without changing it. It requires HTML, JavaScript, content-hashed
Wasm, non-empty Compose assets, placeholder config example, and the module-local Shaka adapter; decodes and confines every script reference to the
distribution; rejects a packaged real config; scans HTML/JavaScript/modules/JSON/source maps and Wasm string content for bounded JWT/read tokens,
bearer/session values, and concrete token/account config values; and reports only repository-relative counts/paths. Required artifacts are not
excluded from scanning. Generated output remains ignored and uncommitted.

Planned serialized commands after A/B integration and candidate freeze:

```powershell
.\gradlew.bat :webApp:wasmJsBrowserDistribution
Set-Location webApp/e2e
npm ci
npm run validate:release
$env:STREAMCORE_WEB_PLAYER_FIXTURES='enabled'
npx playwright test
Remove-Item Env:STREAMCORE_WEB_PLAYER_FIXTURES
```

All build/install/validation/test commands are **NOT RUN** for WEB-04C. The environment flag must be removed in `finally` by an automated runner if the commands are wrapped.
The final matrix must report exact passed/failed/skipped counts; any fixture skip prevents a full WEB-04 player acceptance claim.

### Deployment contract

`docs/kmp/web-release.md` documents the required runtime config boundary, MIME metadata, API/image/media CORS, CSP including local Shaka/Wasm worker
requirements, cache split, static fallback/deep links, atomic publication, rollback, and a current Safari/macOS checklist. It recommends no hosting
vendor and contains no deploy credential.

## Planned review and outstanding gates

- Read-only security/release review: **PASS after one bounded correction**. The initial review blocked raw-token scanning, script-root confinement,
  invalid-ID proof, and profile-isolated resume; a delta-only re-review confirmed all four findings closed and introduced no new scope.
- TypeScript/Playwright discovery: **NOT RUN**
- Release validator against a frozen production artifact: **NOT RUN**
- Chromium 1280 development fixture journey: **NOT RUN**
- Six-project production matrix: **NOT RUN**
- Screenshot inspection: **NOT RUN**; no screenshots were created by WEB-04C
- Current Safari/macOS checklist: **NOT RUN**; Playwright WebKit must not substitute for it
- Final redacted live TMDB/public-media journey and cleanup: **NOT RUN**

WEB-04C is accepted as a reviewed test/docs input. Browser scenarios, artifact validation against the frozen integrated distribution, screenshots,
current Safari/macOS, and live provider/media gates remain explicitly NOT RUN here and are owned by WEB-04D. Integration must reconcile A/B fixture
semantics without weakening assertions before enabling the 60 project-test registrations.
