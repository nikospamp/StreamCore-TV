# WEB-04C release/test evidence

## Status

- Ticket: `WEB-04C`
- Immutable contract-freeze base: `28af56e2d3bf560cf9594c6ea724e3ed510ebf9b`
- Implementation status: WEB-04C test/release input merged into WEB-04D; isolated WEB-04C execution remains **NOT RUN**
- A/B production integration: merged inputs present; integrated Candidates 1 `ace71461c4914716509e1488a311110d7a20844d`, 2
  `d9a05ce7e4594f42efccc8c02d2c9b0ec6003f60`, and 3 `9021e94eb2810463c0f9e9066cb0ec9a9e65f9c1` executed and **FAILED** their
  complete matrices; integrated Candidate 4 `dc066a26effde71eabfc66ca590f37ea976805f7` **PASSED** its complete matrix, but is historical and not
  acceptance-eligible after the subsequent live production blocker and production correction
- Production artifact validation: isolated WEB-04C **NOT RUN**; integrated Candidates 1 through 7 production/Binaryen artifacts and validators
  **PASS**
- WEB-04C standalone Playwright scenarios: **NOT RUN**; integrated Candidate 1 **FAILED** at 144 passed / 42 failed / 0 skipped and Candidate 2
  **FAILED** at 183 passed / 3 failed / 0 skipped; Candidate 3 **FAILED** at 185 passed / 1 failed / 0 skipped; Candidate 4 **PASS** at
  186 passed / 0 failed / 0 skipped / 0 retried; Candidate 5 **FAILED** at 183 passed / 3 failed / 0 skipped; Candidate 6 **FAILED** at
  185 passed / 1 failed / 0 skipped; Candidate 7 **PASS** at 186 passed / 0 failed / 0 skipped / 0 retried
- Current Safari/macOS: **WAIVED / NOT RUN** by explicit user decision on 2026-09-04; no pass is claimed
- Live TMDB or public-media journey: Candidate 4 attempts 1 and 2 **FAIL** at 0/1 in 49.7s and 49.1s; Candidate 7 attempt 1 **FAIL** at 0/1 in
  50.4s, attempt 2 **FAIL** at 0/1 in 59.9s, attempt 3 **FAIL** at 0/1 in 58.4s, and attempt 4 **FAIL** at 0/1 in 56.2s; further Candidate 7
  attempt 5 **PASS** at 1/1 in 35.7s with application cleanup
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
- Integrated Candidate 3 `9021e94eb2810463c0f9e9066cb0ec9a9e65f9c1` passed production distribution re-invocation in 1s with 340 actionable
  tasks (59 executed, 281 up-to-date) and artifact validation with 1 HTML / 1 JavaScript / 2 Wasm / 51 Compose assets / 1 placeholder config /
  0 real configs. Its complete 21.8m matrix failed at 185 passed / 1 failed / 0 skipped. All 60 player registrations passed; the only failure was
  the legacy product WebKit-1280 journey retaining one terminal exact `JsException` I/O read during its tagged expiry phase after behavior passed.
- The post-Candidate-3 correction is test-only and accepts that exact WebKit + `JsException` + I/O-read signature once during the tagged expiry
  phase only. Unknown, wrong-browser, wrong-name/message, null/restoration/other-phase, and repeated expiry diagnostics remain fatal. Candidate 4
  must execute the complete matrix before any pass claim.
- Integrated Candidate 4 `dc066a26effde71eabfc66ca590f37ea976805f7` passed production distribution re-invocation in 1s with 340 actionable
  tasks (59 executed, 281 up-to-date), artifact validation at 1 HTML / 1 JavaScript / 2 Wasm / 51 Compose assets / 1 placeholder config /
  0 real configs, and the complete matrix in 21m41.111s at 186 passed / 0 failed / 0 skipped / 0 retried. Each browser/viewport project passed
  31/31; player, product, and runtime passed 60/60, 66/66, and 60/60 respectively.
- Candidate 4 final production controls/settings captures at 1280×720 and 1920×1080 passed human inspection with no P0/P1 finding. Its combined
  Android/root gate passed in 7m10s with 2,350 actionable tasks (1,858 executed, 164 from cache, 328 up-to-date), including Media3 compilation,
  player-mobile tests, both provider APKs, root `check`, KMP, lint, host-test, and Wasm coverage.
- Candidate 4 live attempt 1 failed 0/1 in 49.7s after reaching real login/session, search, Details, and public Sintel. Attempt 2 on harness revision
  `eb37969` failed 0/1 in 49.1s because a stable projected `Pause` click left native `video.paused=false`; fallback `DELETE` and browser
  local/session-storage cleanup were confirmed only by live-smoke control flow.
- Production diagnosis identified HtmlElementView immediate-parent-host pointer interception. The bounded correction applies immediate video/host
  `pointer-events:none`, a six-frame attachment retry, post-attachment and update reapplication, and update/release cancellation. Focused compile,
  engine 21/21, player UI 14/14, WebApp 65/65, development-distribution, and six-project Play/Pause 6/6 evidence is green; those focused results
  were pre-Candidate-5 evidence only.
- Candidate 5 complete matrix **FAIL** in 26.1m at 183 passed / 3 failed / 0 skipped. Raw terminal diagnostics were the exact adjacent UUID
  blob→I/O pair during product expiry in both WebKit viewports, plus one exact Compose-resource singleton followed by an exact adjacent same-epoch
  coroutine pair during WebKit-1920 player exit. The bounded test-only classifier consolidation uses shared phase/epoch totals, adjacency, and
  per-signature uniqueness. Its final refinement treats one coroutine singleton or one exact adjacent pair as the same signature and blocks pair
  plus singleton or any repeat. The affected WebKit focused rerun passed 4/4 in 1.3m; it does not replace Candidate 5's failure. Candidate 6 is
  the subsequently executed candidate.
- Candidate 6 complete matrix **FAIL** in 26.0m at 185 passed / 1 failed / 0 skipped. The sole WebKit-1280 player failure was unknown
  `Cache API operation failed: Context is stopped` during the second cross-document Back, between known Compose-resource and coroutine teardown
  events after behavior passed. The correction adds no whitelist: it uses projected Player navigation, Escape/Back disposal, browser Forward
  re-entry, and final Back disposal with exact route-scoped lifecycle counters.
- Candidate 7 `1cb7ca253182f5f61ed07e7c9905f18e1307c469` complete non-live gate **PASS**: production/Binaryen distribution in 13s, artifact validator,
  locked install, complete matrix 186/186 in 22.2m with every project 31/31, current controls/settings visual inspection at both required
  viewports, and combined Android/root gate in 58s with 2,350 actionable tasks and player-mobile 12/12.
- Candidate 7 live attempt 1 **FAIL** at 0/1 in 50.4s after the real production Player/public Sintel was reached. The test clicked semantic `Pause`
  while the control still rendered its disabled buffering spinner because the projected node falsely appeared enabled. Credential-free real-media
  evidence changed from `pauseCalls=0`/`playCalls=0` before actionable readiness to `pauseCalls=1`/`playCalls=0`/native `paused=true` after literal
  rendered-text readiness on unchanged Candidate 7 production. The z-order hypothesis was reverted. Test-only correction
  `acc13d034a4d05bb0b3f945d6463e5299b9d5e7f` preserves physical clicks and native state assertions.
- Candidate 7 live attempt 2 **FAIL** at 0/1 in 59.9s after corrected playback, seek/fullscreen, Player exit, and hard reload. Centered focus/hover
  scale animation never satisfied the helper's all-edge bounds rule for restored Details `Play`. Correction
  `09c346ea40d02da5fbd3e94ee8ba17b765879c31` retains positive-area and post-hover checks but stabilizes the invariant click center; the exact
  credential-free Player→Back→reload→Play transition passed 1/1 in 20.5s.
- Candidate 7 live attempt 3 **FAIL** at 0/1 in 58.4s after repeating the same reached checkpoints because consecutive projected centers still did
  not stabilize after reload. Correction `b70eabf328c000dc27b85374e57a601d6dee0a68` retains two-stage hover remeasurement but uses the latest
  unique positive-area bounds without a cross-frame stability oracle; the exact credential-free transition passed 1/1 in 21.3s. Further corrected
  live execution followed.
- Candidate 7 live attempt 4 **FAIL** at 0/1 in 56.2s after all prior checkpoints because restored Details correctly rendered `Resume` once progress
  crossed the resume threshold, while the harness still queried `Play`. One-word correction `1a7f2c3f57489c737a2c6c5de9db0773cb79a4cb` targets exact `Resume`;
  fallback cleanup was confirmed. Live attempt 5 **PASS** at 1/1 in 35.7s with the full product/player/resume/logout journey, all 16 required
  endpoint classes observed only at 2xx, application session deletion confirmed, and browser storage cleared.

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
Candidate 3 later failed 185/1/0, and Candidate 4 later passed 186/0/0 with zero retries, as recorded above.

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

All build/install/validation/test commands are **NOT RUN** for the isolated WEB-04C input. Integrated Candidates 1, 2, and 3 separately passed their
production distributions and artifact validators, then failed their complete matrices at 144/42/0, 183/3/0, and 185/1/0 respectively. Integrated
Candidate 4 passed its production distribution, artifact validator, and complete matrix at 186/0/0 with zero retries.
Candidate 2's behavioral assertions all passed; its three failed project-tests contained only the bounded terminal WebKit diagnostics recorded in
`WEB-04D-evidence.md`. Candidate 3 also passed every player registration and failed only the exact terminal expiry diagnostic recorded there.
Candidate 4's complete pass is recorded independently; test-only corrections never replaced a failed result.

### Deployment contract

`docs/kmp/web-release.md` documents the required runtime config boundary, MIME metadata, API/image/media CORS, CSP including local Shaka/Wasm worker
requirements, cache split, static fallback/deep links, atomic publication, rollback, and a current Safari/macOS checklist. It recommends no hosting
vendor and contains no deploy credential.

## Planned review and outstanding gates

- Read-only security/release review: **PASS after one bounded correction**. The initial review blocked raw-token scanning, script-root confinement,
  invalid-ID proof, and profile-isolated resume; a delta-only re-review confirmed all four findings closed and introduced no new scope.
- TypeScript/Playwright discovery: isolated WEB-04C **NOT RUN**; integrated default discovery **PASS**, 186 registrations in three files
- Release validator against a frozen production artifact: isolated WEB-04C **NOT RUN**; integrated Candidates 1 through 7 **PASS**
- Chromium 1280 development fixture journey: **PASS on the integrated development artifact** — final clean run 10/10 in 57.2s, zero skips;
  earlier bounded runs exposed and retained evidence for module-resource packaging, `HtmlElementView` pointer interception, document Escape, and
  shadow-root focus restoration before the clean rerun.
- Six-project production matrix: isolated WEB-04C **NOT RUN**; integrated Candidate 1 **FAIL** at 144/42/0, Candidate 2 **FAIL** at 183/3/0, and
  Candidate 3 **FAIL** at 185/1/0; Candidate 4 historical **PASS** at 186/0/0 with zero retries, not a current acceptance candidate; Candidate 5
  **FAIL** at 183/3/0; Candidate 6 **FAIL** at 185/1/0; Candidate 7 current **PASS** at 186/0/0 with zero retries
- Screenshot inspection: isolated WEB-04C **NOT RUN**; Candidate 1 failure screenshots were inspected during WEB-04D correction triage, while the
  Candidate 4 historical and Candidate 7 current production controls/settings sets **PASS** at 1280×720 and 1920×1080 with no P0/P1 finding
- Current Safari/macOS checklist: **WAIVED / NOT RUN** by explicit user decision on 2026-09-04; Playwright WebKit does not substitute and no pass
  is claimed
- Final redacted live TMDB/public-media journey and cleanup: Candidate 4 attempt 1 **FAIL** at 0/1 in 49.7s; attempt 2 on `eb37969` **FAIL** at 0/1
  in 49.1s because native video remained unpaused after the stable projected Pause click. Cleanup was confirmed only by live-smoke control flow.
  Candidate 7 attempt 1 **FAIL** at 0/1 in 50.4s because the harness clicked the buffering spinner under a false-enabled semantic projection;
  Candidate 7 attempt 2 **FAIL** at 0/1 in 59.9s because centered control scaling never satisfied the all-edge bounds oracle after hard reload.
  Candidate 7 attempt 3 **FAIL** at 0/1 in 58.4s because the remaining center-stability oracle also did not settle after hard reload. Fallback
  session and browser-storage cleanup were confirmed after all three failures by the same control-flow contract. Candidate 7 attempt 4 **FAIL** at
  0/1 in 56.2s because restored resumable content exposed `Resume`, not `Play`; fallback cleanup was again confirmed. Candidate 7 attempt 5 **PASS**
  at 1/1 in 35.7s with application-driven cleanup.

WEB-04C remains a reviewed test/docs input now merged into WEB-04D. Its isolated execution remains NOT RUN. Integrated Candidates 1, 2, and 3 passed
artifact validation but failed their complete matrices at 144/42/0, 183/3/0, and 185/1/0; bounded corrections did not replace those failures.
Candidate 4 passed distribution, artifact validation, the complete 186-test matrix, and final production visual inspection, but its evidence is
now historical and it is not acceptance-eligible after the live production blocker and subsequent production correction. Current Safari/macOS is
explicitly **WAIVED / NOT RUN**, not a pass. Candidates 5 and 6 failed their complete matrices at 183/3/0 and 185/1/0; Candidate 7 passed its
complete non-live gate at 186/0/0. After four failed, cleanup-confirmed harness attempts, live attempt 5 passed 1/1 with application cleanup;
Candidate 7 is accepted for primary fast-forward.
WEB-04D reconciled A/B fixture semantics without weakening assertions and made all 60 player project-test registrations mandatory; no merged-input
or focused-rerun status is a release pass claim.
