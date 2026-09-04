# WEB-04D Integration Evidence

## Immutable inputs and merge order

- Integration base / accepted WEB-03F merge: `f9e13558b3fc1c68db89ba9715932e8db80813ae`.
- `WEB-04A_ACCEPTED_COMMIT`: `631f4edbd4c7007c8cef3c19d60f2071168a0bd6`.
- `WEB-04B_ACCEPTED_COMMIT`: `718980793ef4eb4e89ad0c355b0ea2678e51adf6`.
- `WEB-04C_ACCEPTED_COMMIT`: `3e3dc3618af19b9a0276f20d6d4674607f87d72b`.
- Mandatory merge order completed without textual conflicts:
  1. A: `a467eeb856f5088fb3080717cdf4303b5a9d01f8`.
  2. B: `59424237368fdfdbc6d0f7d0a473f009bd34e730`.
  3. C: `f64522700a6d7ae5af0f45f4cefd8a7252c223ce`.

All three accepted input worktrees were clean before integration. The primary `codex/kmp-migration` checkout remained at the accepted WEB-03 merge
with only its pre-existing untracked `.kotlin/` directory; WEB-04D does not read, move, stage, or delete that directory. The target architecture
remains backend-agnostic.

## Integrated behavior

- The production graph contains exactly one `PlaybackSessionFactory`, supplied by `webPlaybackModule`; startup probes the factory rather than
  eagerly creating an unowned `PlayerViewModel`/session.
- `/player/{contentId}` renders `WebPlayerRoute`. In-app Details requests reuse the backend-neutral `PlaybackRequestModel`; direct reload resolves a
  `ContentModel` through `DetailsRepository`, maps it to the shared request, and safely replaces to Details on failure while rethrowing coroutine
  cancellation.
- Every production and diagnostic player destination owns a route-keyed `ViewModelStore`; disposal clears it, which synchronously reaches
  `PlayerViewModel.onCleared()` and closes its one session.
- The obsolete WEB-01 Shaka probe, global fallback, direct WebApp npm dependency, and adapter resource are removed. The WebApp packages the
  dependency-owned `shaka-playback-adapter.mjs` and `web-player-fullscreen.mjs` sources without duplicating their committed source of truth.
- The native video and its generated `HtmlElementView` host are pointer-transparent. Compose controls remain sibling semantics, the hidden-controls
  root remains focusable without becoming a parent button, document Escape dispatches exactly one layer, and Compose focus restoration is proved
  by Space reactivating the returned fullscreen/Details action; DOM `activeElement` projection is not treated as the focus oracle.
- Diagnostic fixtures use the real shared `PlayerViewModel` and real profile-isolated browser progress repository with a synthetic source and
  route-isolated fake session. No provider DTO, SDK type, account value, credential, or media URL enters the fixture graph.
- Listener cleanup evidence comes from an actual diagnostic-only `EventTarget` registration/removal balance for relevant document and tagged-video
  listeners. The repeated journey requires a nonzero mounted count and exact zero sessions/listeners/timers/video nodes after every disposal.

## Focused verification ledger

| Purpose | Result |
|---|---|
| `:webApp:compileKotlinWasmJs` after initial wiring | PASS in 1m04s; 156 actionable tasks (149 executed, 7 from cache) |
| First `:webApp:wasmJsBrowserTest` integration attempt | FAIL before tests: dependency-owned `shaka-playback-adapter.mjs` was not beside the consuming generated Kotlin module; 346 actionable tasks |
| Corrected WebApp browser suite | PASS 65/65, zero failures/errors/skips; latest run in 59s with 342 actionable tasks (66 executed, 276 up-to-date) |
| Engine browser suite on integrated source | PASS 19/19, zero failures/errors/skips; task completed before a later UI-suite failure stopped the combined invocation |
| Player UI browser suite after all-null/pointer/Escape/focus corrections | PASS 13/13, then PASS 14/14 after the hidden-controls keyboard regression was added; latest run in 1m05s with 180 actionable tasks (20 executed, 160 up-to-date) |
| `verifyDesignTokens` | PASS in the same serialized invocation; module-local `Dimens.kt` is registered in the exact-path allowlist |
| Development Wasm distribution | PASS; latest pre-hidden-root-focus correction build in 1m09s, 338 actionable tasks (65 executed, 273 up-to-date); approximately 12.3 MiB JS, 31.4 MiB app Wasm, 8.24 MiB Skiko Wasm |
| Locked E2E install | PASS; 3 packages installed, 4 audited, 0 vulnerabilities |
| Default Playwright discovery | PASS; plain `npx playwright test --list` reports 186 tests in 3 files, including all 60 player project-tests with no fixture skip guard |
| Live-smoke discovery | PASS; `npx playwright test --config playwright.live.config.ts --list` reports exactly 1 test without launching a browser or reading credentials |
| Release-script syntax | PASS; `node --check` accepts both `server.mjs` and `validate-release-artifact.mjs` |
| Local artifact server contract | PASS: `/` and `/details/603` returned 200, missing `.js` returned 404, malformed encoding plus encoded backslash/slash traversal returned 400 |
| Chromium 1280 player fixture, initial | FAIL 1/10: nine cases exposed the common native host pointer interception plus timeline selector mismatch |
| Chromium 1280 after pointer correction | 7/10; resume/base Escape and fullscreen projected focus remained failing |
| Affected Escape/resume rerun | 2/3; only shadow-root projected focus remained failing |
| Affected fullscreen focus rerun | PASS 1/1 |
| Final clean Chromium 1280 development fixture before the later hidden-root focusability correction | PASS 10/10 in 57.2s, zero skips |

## Candidate 1 and correction evidence

- Candidate 1: `ace71461c4914716509e1488a311110d7a20844d`; the worktree was clean before execution.
- Focused candidate steps passed: playback Wasm compile in 5s (12 actionable), player UI Wasm compile in 4s (39 actionable), shared player Android
  host tests 15/15 in 20s, and WebApp browser tests 65/65 in 1m10s (342 actionable).
- Production/Binaryen distribution passed in 3m56s with 344 actionable tasks (65 executed, 279 up-to-date): approximately 1.31 MiB JS, 6.14 MiB
  app Wasm, and 8.24 MiB Skiko Wasm.
- Locked install passed (3 packages installed, 4 audited, 0 vulnerabilities). Artifact validation passed with 1 HTML, 1 JS, 2 Wasm, 51 Compose
  assets, placeholder config only, and no real config.
- The first complete production matrix executed all 186 registrations with zero skips in 6.6m and **failed**: 144 passed / 42 failed.
  Classification retained from the full output/artifacts: 29 player cases completed behavior and failed only terminal diagnostics, 6 fullscreen
  cases used unreliable cross-engine DOM-focus projection, 6 product cases queried a not-yet-projected Details node despite visible focus, and 1
  WebKit legacy reload case exceeded its existing warning count under eight-worker contention.
- Correction scope is explicit: default browser workers are now 1; focus is asserted behaviorally; the fixture waits for its history-seed Wasm
  route before navigating; only exact project/source/phase/count-bounded WebKit teardown and Firefox fallback diagnostics are accepted. Unknown,
  standalone network, out-of-phase, or over-count diagnostics still fail.
- Focused correction observations, not a replacement full-matrix claim: fullscreen behavior 6/6; Firefox/WebKit 1280 player sample 18/20 followed
  by passing affected WebKit resume and Escape cases; Details return behavior 2/2 in Chromium/WebKit; isolated WebKit 1920 legacy reload 1/1.
- Latest focused module results remain engine 19/19, player UI 14/14, and WebApp 65/65. Behavioral focus corrections removed the projected-DOM
  focus oracle: Space re-entered fullscreen in all six browser/viewport projects, and Space re-entered Player from returned Details focus in the
  targeted product cases. The listener probe, server confinement, hidden-controls keyboard path, fullscreen focus, and Details return-focus deltas
  each received a bounded P0/P1 re-review PASS.
- Failure screenshots were inspected: the fullscreen control and Details Play action visibly retained the Compose focus ring where DOM focus
  assertions failed. Those correction images were not the final visual gate; Candidate 4's production visual result is recorded below.

Focused tests do not constitute the production candidate or six-project release matrix. A production source change after a development run is not
silently promoted by an affected rerun.

## Candidate 2 and bounded test-only correction

- Candidate 2: `d9a05ce7e4594f42efccc8c02d2c9b0ec6003f60`; the production candidate was frozen before execution.
- Production/Binaryen distribution **PASS** in 3m49s with 340 actionable tasks (64 executed, 276 up-to-date).
- Locked install **PASS**: 3 packages installed, 4 audited, 0 vulnerabilities.
- Artifact validator **PASS**: 1 HTML, 1 JavaScript, 2 Wasm, 51 Compose assets, 1 placeholder config example, and 0 real configs.
- The complete six-project matrix executed all 186 registrations with zero skips in 21.7m and **failed**: 183 passed / 3 failed / 0 skipped.
  All behavioral assertions passed. The three failed project-tests were terminal WebKit diagnostics comprising two exact hard-reload coroutine
  teardown occurrences across the two viewports, one same-epoch player-exit blob-access plus I/O pair, and one diagnostic-seed `web-probe.png`
  fetch abort.
- The post-candidate correction is test-only and bounded to those observed diagnostics: exact project/source/phase/epoch/count matching remains
  mandatory, and the diagnostic history seed must finish its existing image probe before navigating. It does not broaden suppression to unknown,
  product-media, general configuration/network, out-of-phase, or over-count failures.
- The first affected four-case rerun passed 3/4 after all four behaviors completed; the remaining failure exposed one exact WebKit hard-reload
  Compose-resource access teardown diagnostic. The second affected four-case rerun again passed 3/4 after behavior completed and exposed one exact
  WebKit hard-reload I/O teardown diagnostic.
- The classifier was then consolidated to exact WebKit + `pageerror` + `hard-reload` signatures, per-signature uniqueness, and a total cap of two
  accepted diagnostics per phase epoch. The already bounded same-epoch player-exit teardown pair and diagnostic-seed image readiness remain
  separate exact contracts; unknown, duplicate-signature, over-cap, wrong-source, wrong-phase, and other-browser diagnostics still fail.
- The final affected rerun **PASS**: 4/4 in 30.6s. This is focused pre-freeze correction evidence only; it does not replace Candidate 2's failed
  183/3/0 matrix and is not a Candidate 3 complete-matrix result.
- Candidate 2 remains failed; the bounded correction is not a replacement full-matrix pass. Candidate 3 subsequently froze and executed the
  production gates recorded below.

## Candidate 3 and bounded expiry-only correction

- Candidate 3: `9021e94eb2810463c0f9e9066cb0ec9a9e65f9c1`; the candidate was frozen before execution.
- Production/Binaryen distribution re-invocation **PASS** in 1s with 340 actionable tasks (59 executed, 281 up-to-date).
- Artifact validator **PASS**: 1 HTML, 1 JavaScript, 2 Wasm, 51 Compose assets, 1 placeholder config example, and 0 real configs.
- The complete six-project matrix executed all 186 registrations with zero skips in 21.8m and **failed**: 185 passed / 1 failed / 0 skipped.
  All 60 player registrations passed. The sole failure was the legacy product journey in WebKit 1280 retaining one terminal exact
  `JsException: The I/O read operation failed.` during its explicitly tagged `expiry` phase after every behavioral assertion passed.
- The correction is strict and test-only: product diagnostics may consume this exact WebKit + `JsException` + I/O-read signature once in the
  `expiry` phase only. Non-WebKit, restoration/null/other phases, different source/name/message, and a second expiry occurrence remain fatal.
- The affected legacy product journey then passed 2/2 across WebKit 1280 and 1920 in 34.5s; bounded test-integrity and release delta reviews both
  returned `PASS`. This focused result is only pre-freeze evidence.
- Candidate 3 remains failed; 185/186 is not a pass and the one-case correction is not a replacement full matrix. Candidate 4 subsequently froze
  and executed the final non-live gates recorded below.

## Candidate 4 final non-live evidence

- Candidate 4: `dc066a26effde71eabfc66ca590f37ea976805f7`; the candidate was frozen before final non-live execution.
- Production/Binaryen distribution re-invocation **PASS** in 1s with 340 actionable tasks (59 executed, 281 up-to-date).
- Artifact validator **PASS**: 1 HTML, 1 JavaScript, 2 Wasm, 51 Compose assets, 1 placeholder config example, and 0 real configs.
- Complete six-project matrix **PASS** in 21m41.111s: 186 passed / 0 failed / 0 skipped / 0 retried. Every project executed 31/31:
  Chromium 1280, Chromium 1920, Firefox 1280, Firefox 1920, WebKit 1280, and WebKit 1920. By suite, player passed 60/60, product passed 66/66,
  and runtime passed 60/60.
- Final production visual capture and human inspection **PASS** at 1280×720 and 1920×1080 for player controls and settings. Readability, clipping,
  overlap, focus visibility, responsive placement, and scrim/panel hierarchy had no P0/P1 finding.
- Combined Android/root compatibility gate **PASS** in 7m10s with 2,350 actionable tasks (1,858 executed, 164 from cache, 328 up-to-date), including
  Media3 compilation, player-mobile tests, TMDB and ClientB APK assembly, root `check`, KMP, lint, host-test, and Wasm coverage.
- Player-mobile unit results were 12/12 with zero failures/errors/skips. The generated TMDB and ClientB debug APKs were 23,177,040 bytes and
  23,012,604 bytes respectively.
- Post-run cleanup restored the WEB-04D worktree to its committed state. The primary checkout remains at
  `f9e13558b3fc1c68db89ba9715932e8db80813ae` with exactly the pre-existing untracked `.kotlin/`; the build-generated untracked `.android/` was
  removed without touching `.kotlin/`.
- Candidate 4 is green for every recorded non-live gate. On 2026-09-04 the user explicitly waived the manual current Safari/macOS gate; it remains
  **WAIVED / NOT RUN**, is not a pass, and no Safari execution evidence is claimed. WEB-04 acceptance is now blocked only by a successful rerun of
  the authorized live TMDB/public-media journey with mandatory temporary-session cleanup.

## External-gate status and live attempt 1

- Manual current Safari/macOS: **WAIVED / NOT RUN** by explicit user decision on 2026-09-04. Playwright WebKit is not substituted for Safari, the
  checklist was not executed, and the waiver removes this gate as a current blocker without converting it to `PASS`.
- Candidate 4 live attempt 1 used the boolean-clean wrapper and **FAILED** at 0/1 in 49.7s. The journey reached real login/session, search,
  Details, and public Sintel, then failed while waiting for projected `Play` to appear after an attempted `Pause`.
- Cleanup evidence is limited to the live smoke's control flow: fallback `DELETE` cleanup was confirmed because no cleanup exception replaced the
  original failure, and browser local/session-storage cleanup was awaited. This is not recorded as a successful live journey.
- A bounded test-only correction now re-resolves stable projected semantic bounds after hover/recomposition and uses native
  `HTMLVideoElement.paused` state as the pause/play oracle. Its capped delta review returned **PASS**; the corrected live rerun is **NOT RUN** and
  requires fresh action-time authorization immediately before transmission.
- Candidate 4's production, complete non-live matrix, visual, and Android/root evidence remains valid. WEB-04 acceptance is blocked only by the
  authorized live rerun.

## Review and release gates

- Current capped P0/P1 review status: **PASS across architecture/backend/DI, lifecycle/input/accessibility, and security/release/E2E** after the
  bounded corrections recorded below; no review blocker remains open before Candidate 4 freeze.
- Capped architecture/backend/DI review: `PASS` — no P0/P1 or acceptance blocker.
- Capped lifecycle/input/accessibility review: initial `BLOCK` on hidden-controls key-handler modifier order; corrected regression passes 14/14 and
  delta re-review returned `PASS`.
- Capped security/release/E2E review: initial `BLOCK` on default-skipped player registrations, stale live placeholder behavior,
  malformed/sibling-prefix server paths, application-wide Shaka fallback scanning, and mixed observed/not-run wording. All bounded corrections plus
  production-video removal assertions were delta-reviewed `PASS`.
- Failed frozen candidate: `ace71461c4914716509e1488a311110d7a20844d`; it is not eligible for acceptance.
- Failed Candidate 2: `d9a05ce7e4594f42efccc8c02d2c9b0ec6003f60`; 183/186 passed with 3 terminal-diagnostic failures and zero skips.
- Failed Candidate 3: `9021e94eb2810463c0f9e9066cb0ec9a9e65f9c1`; 185/186 passed with 1 terminal expiry diagnostic and zero skips.
- Candidate 4: `dc066a26effde71eabfc66ca590f37ea976805f7`; final non-live candidate.
- Production/Binaryen distribution and artifact validator: candidates 1, 2, 3, and 4 `PASS`.
- Complete Chromium/Firefox/WebKit matrix: Candidate 1 `FAIL` (144/186), Candidate 2 `FAIL` (183/186), Candidate 3 `FAIL` (185/186), Candidate 4
  `PASS` (186/186, zero failures/skips/retries).
- Player screenshot inspection: Candidate 1 correction screenshots inspected for focus evidence; Candidate 4 final production controls/settings
  captures at both required viewports `PASS` with no P0/P1 finding.
- Android/root compatibility gate: Candidate 4 `PASS` in 7m10s, 2,350 actionable tasks (1,858 executed, 164 from cache, 328 up-to-date).
- Manual current Safari/macOS: `WAIVED / NOT RUN` by explicit user decision on 2026-09-04; Playwright WebKit cannot substitute, and no pass is
  claimed.
- Final live TMDB/public-media journey and temporary-session cleanup: attempt 1 `FAIL` at 0/1 in 49.7s after reaching public Sintel; corrected rerun
  `NOT RUN` pending new action-time authorization immediately before transmission.
