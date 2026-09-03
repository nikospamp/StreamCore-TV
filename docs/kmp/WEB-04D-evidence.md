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
  root remains focusable without becoming a parent button, document Escape dispatches exactly one layer, and fullscreen exit restores the current
  projected control across the Compose open shadow root.
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

Focused tests do not constitute the production candidate or six-project release matrix. A production source change after a development run is not
silently promoted by an affected rerun.

## Review and release gates

- Capped architecture/backend/DI review: `PASS` — no P0/P1 or acceptance blocker.
- Capped lifecycle/input/accessibility review: initial `BLOCK` on hidden-controls key-handler modifier order; corrected regression passes 14/14 and
  delta re-review returned `PASS`.
- Capped security/release/E2E review: initial `BLOCK` on default-skipped player registrations, stale live placeholder behavior,
  malformed/sibling-prefix server paths, application-wide Shaka fallback scanning, and mixed observed/not-run wording. All bounded corrections plus
  production-video removal assertions were delta-reviewed `PASS`.
- Frozen integrated production commit: `NOT YET CREATED`.
- Production/Binaryen distribution and artifact validator: `NOT RUN` on a frozen WEB-04D commit.
- Complete Chromium/Firefox/WebKit matrix: `NOT RUN` on a frozen WEB-04D commit.
- Player screenshot inspection: `NOT RUN` on a frozen WEB-04D commit.
- Android/root compatibility gate: `NOT RUN` on a frozen WEB-04D commit.
- Manual current Safari/macOS: `NOT RUN`; Playwright WebKit cannot substitute.
- Final live TMDB/public-media journey and temporary-session cleanup: `NOT RUN`; it requires new action-time authorization immediately before
  transmission.
