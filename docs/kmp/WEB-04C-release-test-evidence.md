# WEB-04C release/test evidence

## Status

- Ticket: `WEB-04C`
- Immutable contract-freeze base: `28af56e2d3bf560cf9594c6ea724e3ed510ebf9b`
- Implementation status: WEB-04C test/release input merged into WEB-04D; isolated WEB-04C execution remains **NOT RUN**
- A/B production integration: merged inputs present; integrated Candidate 1 `ace71461c4914716509e1488a311110d7a20844d` and Candidate 2
  `d9a05ce7e4594f42efccc8c02d2c9b0ec6003f60` both executed and **FAILED** their complete matrices
- Production artifact validation: isolated WEB-04C **NOT RUN**; integrated Candidates 1 and 2 production/Binaryen artifacts and validators **PASS**
- WEB-04C standalone Playwright scenarios: **NOT RUN**; integrated Candidate 1 **FAILED** at 144 passed / 42 failed / 0 skipped and Candidate 2
  **FAILED** at 183 passed / 3 failed / 0 skipped
- Current Safari/macOS: **NOT RUN**
- Live TMDB or public-media journey: **NOT RUN**
- WEB-04C accepted commit: published by the integration-owner handoff after this evidence commit

The target architecture remains backend-agnostic. No production Kotlin/resource, playback, feature, core, build-logic, credential, real-config,
generated distribution, browser binary, node-module, or screenshot file was changed.

## Observed facts

- `git rev-parse HEAD` returned the exact required 40-character contract-freeze base above before edits.
- The original WEB-04C input branched from the frozen WEB-03 player placeholder; WEB-04A/B/C inputs are now merged for WEB-04D integration work,
  with no integrated pass claimed here.
- The merged artifact contract requires `index.html`, `config.example.json`, and the module-local `shaka-playback-adapter.mjs`; the obsolete WEB-01
  `shaka-adapter.mjs` and all global/CDN Shaka fallbacks must be absent.
- Existing browser testing documents prove that projected Compose semantics are geometry/discovery contracts: pointer interaction must target their
  resolved bounds, while native `HtmlElementView` controls may use explicit DOM selectors.
- No build, npm, Playwright, browser, Gradle, media, or credential command was run on the isolated WEB-04C branch; integrated WEB-04D observations
  are explicitly separated below and in `WEB-04D-evidence.md`.
- `node --check webApp/e2e/server.mjs` and `node --check webApp/e2e/validate-release-artifact.mjs` passed; `git diff --check` passed.
- On integrated Candidate 1, production/Binaryen distribution and artifact validation passed. The subsequent complete six-project matrix executed
  186 registrations and failed with 144 passed / 42 failed / 0 skipped, so Candidate 1 is not an accepted release candidate.
- Integrated Candidate 2 `d9a05ce7e4594f42efccc8c02d2c9b0ec6003f60` passed production/Binaryen distribution in 3m49s with 340 actionable
  tasks (64 executed, 276 up-to-date), locked install with 3 packages installed / 4 audited / 0 vulnerabilities, and artifact validation with
  1 HTML / 1 JavaScript / 2 Wasm / 51 Compose assets / 1 placeholder config / 0 real configs. Its complete 21.7m matrix executed all 186
  registrations and failed with 183 passed / 3 failed / 0 skipped. All behavioral assertions passed; terminal WebKit diagnostics comprised two
  exact hard-reload coroutine teardown occurrences across viewports, one same-epoch player-exit blob-access plus I/O pair, and one diagnostic-seed
  `web-probe.png` fetch abort.

The isolated WEB-04C observations are not runtime passes; integrated candidate results are labeled explicitly.

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

WEB-04D removed the pre-integration skip guard after supplying the synthetic harness. All ten scenarios are now mandatory in the default suite,
so plain `npx playwright test` registers 60 player project-tests and cannot succeed by skipping them. The exact projection is proved in one
development Chromium project. The complete matrix remained **NOT RUN on isolated WEB-04C**; integrated Candidate 1 later executed the production
matrix and failed 144/42/0, while Candidate 2 later failed 183/3/0 as recorded above.

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
- Exact projected actions are unique on their active layer: `Play`, `Retry`, `Playback settings`, `Speed · 1×`, `1.5×`,
  `Back to playback settings`, `Quality · 1080p · 5.8 Mbps`, `1080p · 5.8 Mbps`, `Audio · English · Original 5.1`,
  `Ελληνικά · Stereo`, `Subtitles · Off`, `English (CC)`, and `Enter fullscreen`. The timeline is the single adjustable generic node named
  `Playback position <position> of 2:00`; it is not claimed as an HTML `slider` role.
- Non-sensitive fixture state uses `data-player-phase`, `data-player-playing`, `data-player-activation-required`, `data-player-error-code`,
  `data-player-error-message`, `data-player-prepare-count`, `data-player-position-ms`, `data-player-speed`, `data-player-video-track`,
  `data-player-audio-track`, `data-player-text-track`, `data-player-fullscreen`, `data-player-layer`,
  `data-player-filmstrip-count`, and `data-player-canvas-capture-count`.
- Cleanup counters remain inspectable on the diagnostic details route through `data-player-active-sessions`, `data-player-active-listeners`,
  `data-player-active-timers`, and `data-player-close-count`; they contain counts only and reset at browser-context start. Listener balance is
  measured from actual relevant document and tagged-video `EventTarget` registrations/removals rather than inferred from session count.
- Escape from settings closes only settings; Escape from the base player returns to diagnostic details. Product-route Back/Escape behavior still
  requires WEB-04D integration coverage.
- The resume fixture accepts synthetic `profile=profile-a` and `profile=profile-b`, exposes the active synthetic ID through
  `data-player-profile-id`, starts without progress, persists a scrubbed position on close, restores it for the same profile across reopen/reload,
  and exposes no inherited position for the other profile. It uses the real profile-isolated progress repository with no provider DTO or account
  value.

If the accepted A/B semantics differ, update this test and `docs/kmp/web-testing.md` together, prove the replacement selector contract in all six
projects, and record it as an integration-only reconciliation. Missing fixture attributes or placeholder UI must fail the mandatory default suite.

### Artifact validator

`webApp/e2e/validate-release-artifact.mjs` checks the configured production output without changing it. It requires HTML, JavaScript, content-hashed
Wasm, non-empty Compose assets, placeholder config example, and `shaka-playback-adapter.mjs`; verifies its static pinned-package import; rejects the
obsolete WEB-01 adapter plus remote/CDN or global-object Shaka fallbacks; decodes and confines every script reference to the distribution; rejects
a packaged real config; scans HTML/JavaScript/modules/JSON/source maps and Wasm string content for bounded JWT/read tokens, bearer/session values,
and concrete token/account config values; and reports only repository-relative counts/paths. Required artifacts are not excluded from scanning.
Generated output remains ignored and uncommitted.

Planned serialized commands after the merged input is frozen as an integrated candidate:

```powershell
.\gradlew.bat :webApp:wasmJsBrowserDistribution
Set-Location webApp/e2e
npm ci
npm run validate:release
npx playwright test
```

All build/install/validation/test commands are **NOT RUN** for the isolated WEB-04C input. Integrated Candidates 1 and 2 separately passed their
locked installs, production distributions, and artifact validators, then failed their complete matrices at 144/42/0 and 183/3/0 respectively.
Candidate 2's behavioral assertions all passed; its three failed project-tests contained only the bounded terminal WebKit diagnostics recorded in
`WEB-04D-evidence.md`. Candidate 3 must rerun the complete matrix; test-only corrections cannot replace either failed result.

### Deployment contract

`docs/kmp/web-release.md` documents the required runtime config boundary, MIME metadata, API/image/media CORS, CSP including local Shaka/Wasm worker
requirements, cache split, static fallback/deep links, atomic publication, rollback, and a current Safari/macOS checklist. It recommends no hosting
vendor and contains no deploy credential.

## Planned review and outstanding gates

- Read-only security/release review: **PASS after one bounded correction**. The initial review blocked raw-token scanning, script-root confinement,
  invalid-ID proof, and profile-isolated resume; a delta-only re-review confirmed all four findings closed and introduced no new scope.
- TypeScript/Playwright discovery: isolated WEB-04C **NOT RUN**; integrated default discovery **PASS**, 186 registrations in three files
- Release validator against a frozen production artifact: isolated WEB-04C **NOT RUN**; integrated Candidates 1 and 2 **PASS**
- Chromium 1280 development fixture journey: **PASS on the integrated development artifact** — final clean run 10/10 in 57.2s, zero skips;
  earlier bounded runs exposed and retained evidence for module-resource packaging, `HtmlElementView` pointer interception, document Escape, and
  shadow-root focus restoration before the clean rerun.
- Six-project production matrix: isolated WEB-04C **NOT RUN**; integrated Candidate 1 **FAIL** at 144/42/0 and Candidate 2 **FAIL** at 183/3/0
- Screenshot inspection: isolated WEB-04C **NOT RUN**; Candidate 1 failure screenshots were inspected during WEB-04D correction triage, while the
  Candidate 3 visual set remains **NOT RUN**
- Current Safari/macOS checklist: **NOT RUN**; Playwright WebKit must not substitute for it
- Final redacted live TMDB/public-media journey and cleanup: **NOT RUN**

WEB-04C remains a reviewed test/docs input now merged into WEB-04D. Its isolated execution remains NOT RUN. Integrated Candidates 1 and 2 passed
artifact validation but failed their complete matrices at 144/42/0 and 183/3/0; bounded corrections do not replace either failure. Candidate 3
distribution, artifact validation, complete matrix, visual set, current Safari/macOS, and live provider/media gates remain explicitly NOT RUN.
WEB-04D reconciled A/B fixture semantics without weakening assertions and made all 60 player project-test registrations mandatory; no merged-input
or focused-rerun status is a release pass claim.
