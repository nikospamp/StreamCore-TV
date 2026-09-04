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
| Production host-pointer compile correction | Initial FAIL in 10s, then PASS in 3s; 8 actionable tasks (6 executed, 2 up-to-date) |
| Corrected WebApp browser suite | PASS 65/65, zero failures/errors/skips; latest run in 43s with 342 actionable tasks (66 executed, 276 up-to-date) |
| Engine browser suite on integrated source | PASS 21/21, zero failures/errors/skips; latest run in 54s with 148 actionable tasks (12 executed, 136 up-to-date) |
| Player UI browser suite after all-null/pointer/Escape/focus corrections | PASS 13/13, then PASS 14/14 after the hidden-controls keyboard regression was added; latest up-to-date run in 2s with 180 actionable tasks (15 executed, 165 up-to-date) |
| `verifyDesignTokens` | PASS in the same serialized invocation; module-local `Dimens.kt` is registered in the exact-path allowlist |
| Development Wasm distribution | PASS; latest post-host-pointer-correction build in 35s, 338 actionable tasks (63 executed, 275 up-to-date) |
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
| Six-project physical Play/Pause regression after host-pointer correction | PASS 6/6 in 36.8s |

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
- At that correction stage, focused module results were engine 19/19, player UI 14/14, and WebApp 65/65. Behavioral focus corrections removed the projected-DOM
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

## Candidate 4 historical non-live evidence

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
- Candidate 4 passed every recorded non-live gate, but this is now historical evidence only. Two failed live attempts exposed a production
  HtmlElementView host hit-test blocker, and the subsequent production correction means Candidate 4 is not eligible for WEB-04 acceptance.

## External-gate status, Candidate 4 live attempts, and production correction

- Manual current Safari/macOS: **WAIVED / NOT RUN** by explicit user decision on 2026-09-04. Playwright WebKit is not substituted for Safari, the
  checklist was not executed, and the waiver removes this gate as a current blocker without converting it to `PASS`.
- Candidate 4 live attempt 1 used the boolean-clean wrapper and **FAILED** at 0/1 in 49.7s. The journey reached real login/session, search,
  Details, and public Sintel, then failed while waiting for projected `Play` to appear after an attempted `Pause`.
- Cleanup evidence is limited to the live smoke's control flow: fallback `DELETE` cleanup was confirmed because no cleanup exception replaced the
  original failure, and browser local/session-storage cleanup was awaited. This is not recorded as a successful live journey.
- A bounded test-only correction now re-resolves stable projected semantic bounds after hover/recomposition and uses native
  `HTMLVideoElement.paused` state as the pause/play oracle. Its capped delta review returned **PASS**.
- Candidate 4 live attempt 2 on harness revision `eb37969` also **FAILED** at 0/1 in 49.1s: the stable projected `Pause` bounds were clicked, but
  native `video.paused` remained `false`. Fallback `DELETE` and browser local/session-storage cleanup were again confirmed only by the live
  smoke's control flow; no cleanup exception replaced the original failure.
- Bounded production diagnosis identified the immediate HtmlElementView parent host as the hit-test interceptor above the Compose canvas. The
  production surface now sets the video and attached host to `pointer-events:none` immediately, retries host attachment for at most six animation
  frames, reapplies once on the frame after attachment, reapplies from HtmlElementView updates, and cancels pending work on update/release.
- Wasm surface tests cover an existing host `pointer-events:auto` override, delayed host attachment plus post-attachment reapplication, and
  cancellation/idempotent release. The successful physical-pointer fixture now checks computed video/host pass-through and performs
  Play→Pause→Play semantics; the live smoke checks the same production video/host invariant before activation.
- Focused correction results: production compile initially **FAIL** in 10s, then **PASS** in 3s with 8 actionable tasks (6 executed, 2
  up-to-date); engine browser suite **PASS** 21/21 in 54s with 148 actionable (12 executed, 136 up-to-date); player UI browser suite **PASS** 14/14
  up-to-date in 2s with 180 actionable (15 executed, 165 up-to-date); WebApp browser suite **PASS** 65/65 in 43s with 342 actionable (66 executed,
  276 up-to-date); development distribution **PASS** in 35s with 338 actionable (63 executed, 275 up-to-date); and the six-project physical
  Play/Pause regression **PASS** 6/6 in 36.8s.
- The production correction and focused results were pre-Candidate-5 evidence only. Candidate 5 subsequently froze and ran the complete matrix
  recorded below. Safari remains **WAIVED / NOT RUN**. Candidate 7 later supplied a green non-live candidate and a passing live journey with
  application cleanup, recorded below.

## Candidate 5 matrix and bounded classifier correction

- Frozen production correction `a6f4b64b2ab75ec3439623bfd56b31b759badfef` ran
  `.\gradlew.bat :webApp:wasmJsBrowserDistribution --max-workers=1`: **PASS** in 3m42s with 340 actionable tasks (64 executed,
  276 up-to-date). `webApp/build/dist/wasmJs/productionExecutable` contained approximately 1.31 MiB JavaScript, 6.14 MiB application Wasm,
  and 8.24 MiB Skiko Wasm.
- Candidate 5 dependency and artifact checks: `npm ci` **PASS** in 4m with 3 packages installed, 4 audited, and 0 vulnerabilities;
  `npm run validate:release` **PASS** with 1 HTML, 1 JavaScript, 2 Wasm, 51 Compose assets, 1 placeholder config example, and 0 real configs.
- Candidate 5 complete six-project matrix **FAIL** in 26.1m: 183 passed / 3 failed / 0 skipped. The failed result remains authoritative and is not
  replaced by the following test-only correction.
- Raw WebKit classification: the product expiry phase emitted one exact, adjacent localhost-UUID blob-access → `JsException` I/O-read pair in
  WebKit 1280 and again in WebKit 1920. The prior product classifier accepted the I/O event as its expiry singleton but retained the blob event.
- Raw WebKit classification: the WebKit-1920 player-exit epoch emitted one exact Compose-resource access event followed by one exact adjacent
  same-epoch coroutine-teardown pair. The prior player-exit classifier retained the Compose-resource event and counted the two coroutine events
  as unrelated singles. All three project failures reached their final diagnostics assertion after the behavioral checks completed.
- The product correction accepts one exact adjacent UUID blob→I/O pair in either non-null `restoration` or `expiry`, using one decoder allowance
  per phase. The established standalone expiry I/O event shares that expiry allowance, so pair plus singleton, a repeated pair, a standalone blob,
  wrong ordering, or cross-phase adjacency remains fatal.
- The player-exit correction uses a total cap of three events per epoch and a per-signature ledger. It accepts one exact Compose-resource singleton
  and either one exact coroutine singleton or one exact adjacent same-epoch coroutine pair, counting the pair as two and sharing one coroutine
  signature. The existing blob→I/O pair also counts as two and marks I/O consumed; exact config, I/O, response-class-cast, and Compose-resource
  singles remain unique. Unknown, extra, nonadjacent, wrong-source, wrong-phase,
  wrong-epoch, and non-WebKit diagnostics remain fatal.
- Final classifier refinement makes the coroutine singleton and exact adjacent pair two representations of the same per-epoch signature. Accepting
  either blocks a later singleton, pair, or repeat in that epoch. The affected WebKit focused rerun **PASS** 4/4 in 1.3m; this remains focused
  correction evidence and does not replace Candidate 5's failed matrix.
- Candidate 5 is not eligible for acceptance. Candidate 6 subsequently froze and ran the complete matrix recorded below; focused correction
  evidence cannot replace Candidate 5's failed matrix.

## Candidate 6 matrix and non-whitelist navigation correction

- Candidate 6 was test/docs-only relative to Candidate 5 production. Re-invoking
  `.\gradlew.bat :webApp:wasmJsBrowserDistribution --max-workers=1` **PASS** in 3s with 340 actionable tasks (59 executed, 281 up-to-date), and
  `npm run validate:release` **PASS** with the same 1 HTML / 1 JavaScript / 2 Wasm / 51 Compose assets / 1 placeholder config / 0 real config
  inventory in `webApp/build/dist/wasmJs/productionExecutable`.
- Frozen Candidate 6 `308742ad6ef3574a0819ba6424f7e2d8dd5b45d1` ran `npx playwright test`; its complete six-project matrix **FAIL**
  in 26.0m: 185 passed / 1 failed / 0 skipped. The sole failure was the WebKit-1280
  `Escape closes one layer and browser Back returns to details` player case retaining the unknown `Context is stopped` page error in
  `player-exit` epoch 2.
- Raw trace timing proves the failure occurred during the test's second cross-document `page.goBack()`: the player Compose-resource fetch began at
  `951926.710` and completed about `951942.240`; Back began at `952038.227`; the known Compose-resource teardown appeared at `952215.968`;
  the unknown `Cache API operation failed: Context is stopped` appeared at `952229.045`; the known coroutine teardown followed at
  `952244.273`; and Back completed at `952294.347`.
- The existing post-navigation settle could not eliminate the error because it runs against the new Details document after the old WebKit context
  has already stopped. No classifier signature or whitelist was added for `Context is stopped`.
- Focused correction attempt 1 **FAIL** at 0/2 across WebKit-1280/1920: Details → Player correctly reached exact
  `/diagnostic/player/603`, but the inherited assertion still required the query-bearing direct-entry URL. The exact-route assertion was corrected;
  no re-entry, final cleanup, or terminal-diagnostics result was established by that attempt.
- Focused correction attempt 2 **FAIL** at 0/2 across WebKit-1280/1920: initial projected-button entry, settings Escape, Player Escape/Back, zero
  active lifecycle counters, and close count 1 passed. The visible diagnostic `Player ID` action did not have a projected button role after that
  return, so the second role lookup stopped the case before re-entry and final cleanup. No production accessibility failure was inferred from this
  diagnostic-only projection observation.
- The final bounded test-only correction removes direct full-document Player `page.goto` entry. It enters through the projected `Player ID` action
  and production `history.pushState`, verifies Escape/Back disposal, reopens the same history entry with browser Forward/`popstate`, and exits with
  browser Back/`popstate`. It requires close count 1 after the first return, active session/video count 1 after Forward, and exact zero
  sessions/listeners/timers/video plus close count 2 after final Back before diagnostics must remain clean.
- Exact affected command `npx.cmd playwright test tests/player.spec.ts --project=webkit-1280 --project=webkit-1920 --grep "Escape closes one layer"
  --reporter=line` **PASS** with exit code 0 at 2/2 in 13.0s. The classifier is unchanged; unknown `Context is stopped` remains fatal. This focused
  result does not replace Candidate 6's failed matrix.
- Candidate 6 remains failed. Candidate 7 subsequently froze and completed the non-live gates recorded below.

## Candidate 7 frozen non-live acceptance evidence

- Candidate 7: `1cb7ca253182f5f61ed07e7c9905f18e1307c469`; the WEB-04D worktree was clean before execution. Its delta from Candidate 6 is limited to
  the reviewed Player navigation test and evidence/release documentation; production remains the backend-agnostic Candidate 5 base.
- `.\gradlew.bat :webApp:wasmJsBrowserDistribution --max-workers=1 --console=plain` **PASS** in 13s with 344 actionable tasks (59 executed,
  285 up-to-date). The production output contains `streamcore-web.js` at 1,377,368 bytes, application Wasm at 6,435,018 bytes, and Skiko Wasm at
  8,640,316 bytes.
- `npm.cmd run validate:release` **PASS** with 1 HTML, 1 JavaScript, 2 Wasm, 51 Compose assets, 1 placeholder config example, and 0 real configs.
  The frozen locked install `npm.cmd ci` also **PASS** in 5m with 3 packages added; the package and lock inputs are unchanged from Candidate 5.
- `npx.cmd playwright test --reporter=line` **PASS** with exit code 0 in 22.2m: 186 passed / 0 failed / 0 skipped / 0 retried. Chromium 1280,
  Chromium 1920, Firefox 1280, Firefox 1920, WebKit 1280, and WebKit 1920 each completed 31/31. By suite, Player passed 60/60, product passed 66/66,
  and runtime passed 60/60.
- Current production visual capture **PASS**: the temporary capture-only test passed 2/2 in 6.3s and produced controls/settings images at 1280×720
  and 1920×1080. Human inspection found no P0/P1 readability, clipping, overlap, focus-visibility, responsive-placement, timeline, or panel/scrim
  issue. The temporary test and tracked screenshot churn were restored exactly to Candidate 7 before the next gate.
- Combined Android/root compatibility gate **PASS** in 58s with 2,350 actionable tasks (103 executed, 2,247 up-to-date), covering Media3 compile,
  player-mobile unit tests, TMDB and ClientB debug APK assembly, and root `check` with design-token verification and one worker. Player-mobile reports
  12/12 with zero failures/errors/skips. APK sizes are 23,177,040 bytes (TMDB) and 23,012,604 bytes (ClientB).
- Post-run cleanup restored WEB-04D to exact Candidate 7 and removed the verified generated primary `.android/` directory. The primary checkout
  remains `f9e13558b3fc1c68db89ba9715932e8db80813ae` with exactly its pre-existing untracked `.kotlin/` directory.
- Every required non-live Candidate 7 gate is green. Current Safari/macOS remains explicitly **WAIVED / NOT RUN**, never `PASS`. The first
  user-authorized Candidate 7 live attempt subsequently failed and is recorded below; it does not invalidate the non-live gate.

## Candidate 7 live attempts and bounded harness corrections

- Candidate 7 live attempt 1 (overall live attempt 3) ran from clean evidence revision
  `7827a7cdcc6d29248e09ced6e9dd640dfbb4dc20`. The redacted wrapper reported both ignored inputs configured, all six required environment values
  configured, and `childEnvironmentMatched=True`; no secret value was printed.
- The live journey **FAIL** at 0/1 in 50.4s. It completed real login/session, profile selection, search, Details/My List/Library navigation, entered
  the production Player with public Sintel, found the production video, and verified computed video plus immediate-host `pointer-events:none`.
  It then clicked projected `Pause` while native `video.paused` remained `false` for 30s.
- Trace and credential-free instrumentation isolated the failure to the readiness oracle, not production playback. At the original click, the
  Player was buffering and the Play/Pause control rendered its loading spinner, while its projected semantic name remained `Pause` and Playwright
  reported it enabled. Canvas received the exact-center pointerdown/up/click, but the intentionally disabled loading control dispatched no command:
  `pauseCalls=0`, `playCalls=0`, `pauseEvents=0`, and native `paused=false`.
- A credential-free real Shaka/Sintel rerun gated the click on literal rendered `Pause` text. It passed both with an experimental z-order change and,
  decisively, after that experiment was fully reverted to the original Candidate 7 production code: **PASS** 1/1 in 17.8s with
  `pauseCalls=1`, `playCalls=0`, one pause event, and native `paused=true`. Temporary instrumentation, z-order source/tests, screenshots, and product
  test changes were removed; no production change remains.
- Test-only correction `acc13d034a4d05bb0b3f945d6463e5299b9d5e7f` filters the semantic Play/Pause locators by exact rendered `Play` or `Pause` text, requires exactly
  one actionable control, re-resolves stable bounds, and retains native paused/playing plus opposite-label assertions. Credential/session routing,
  endpoint assertions, and cleanup are unchanged. Live discovery remains exactly 1 test in 1 file.
- The failed attempt stopped before application logout. Because a session had been captured, the harness `finally` path required fallback authenticated
  `DELETE` success plus `{ success: true }` and would have replaced the original assertion with `Temporary-session cleanup was not confirmed` on
  cleanup failure. The original Pause assertion remained terminal, and nested local/session-storage clearing completed; cleanup is therefore
  confirmed by control flow, not claimed as application logout.
- Candidate 7 live attempt 2 (overall live attempt 4) ran from clean diagnosis revision
  `9ad17b16ca74568ef3f9b042597877722f52e860`; the same redacted boolean preflight passed with `childEnvironmentMatched=True`. It progressed through
  corrected production Pause→Play, seek beyond the resume threshold, fullscreen pointer and Space re-entry, Player exit/video removal, hard reload,
  and ready restored `/details/550`, then **FAIL** at 0/1 in 59.9s while resolving the reloaded Details `Play` bounds. The second Player entry,
  resume-position assertion, application logout, and application-driven deletion were not reached.
- The geometry helper required x, y, width, and height all to change by less than 0.5px. `StreamCoreWebButton` intentionally scales around its center
  for focus and hover, changing every edge while leaving the safe click center invariant. Test-only correction
  `09c346ea40d02da5fbd3e94ee8ba17b765879c31` now requires a positive-area rectangle whose center is stable within 0.5px, then preserves the
  existing pre-hover resolve, pointer move, post-hover re-resolve, and physical click.
- The exact credential-free product transition—login/profile → Details → production Player/video → Escape cleanup → Details hard reload →
  center-stable physical Play → Player/video—**PASS** 1/1 in 20.5s. Temporary test code and artifacts were removed. Live discovery remains exactly
  1 test in 1 file.
- Attempt 2 stopped before application logout, so the same mandatory fallback authenticated `DELETE` and nested browser-storage cleanup ran. The
  terminal result remained the bounds assertion rather than `Temporary-session cleanup was not confirmed`; cleanup is confirmed by control flow.
- Candidate 7 live attempt 3 (overall live attempt 5) ran from clean evidence revision
  `365f823680060dbdbffb4ae16f7a81a8dc528ac2`; redacted preflight again passed. It repeated the corrected playback, seek/fullscreen, Player exit,
  hard reload, and ready restored Details checkpoints, but the center-stability poll still did not obtain consecutive projected bounds and the run
  **FAIL** at 0/1 in 58.4s before the second Player entry.
- Cross-frame stability is not required for safe physical activation because the helper already resolves once, moves to that center, waits for
  hover/recomposition, and resolves again immediately before clicking. Test-only correction
  `b70eabf328c000dc27b85374e57a601d6dee0a68` therefore requires one unique positive-area current rectangle at each stage and clicks the latest
  center. The exact credential-free Player→Back→reload→Play journey **PASS** 1/1 in 21.3s; temporary code/artifacts were removed and live discovery
  remains exactly 1 test in 1 file.
- Attempt 3 also stopped before logout; the original bounds assertion remained terminal rather than the cleanup-specific error, confirming fallback
  session deletion and nested browser-storage cleanup by control flow.
- Candidate 7 live attempt 4 (overall live attempt 6) ran from clean evidence revision
  `f38f1e254bb54aa9704d4d40c7adfdacfd44de14`; redacted preflight passed and all prior checkpoints repeated. It then **FAIL** at 0/1 in 56.2s
  because restored Details projected no `Play` button. This is expected product state: progress had been persisted beyond the resume threshold, so
  `DetailsViewModel` sets resumable progress and the Details CTA renders `Resume`. The harness was querying the wrong accessible name.
- One-word test correction `1a7f2c3f57489c737a2c6c5de9db0773cb79a4cb` activates exact `Resume` after hard reload and retains physical bounds, Player/video, restored-position,
  logout, endpoint, and cleanup assertions. Live discovery remains exactly 1 test in 1 file. Attempt 4 stopped before logout; fallback session and
  browser-storage cleanup were again confirmed by control flow.
- Candidate 7 live attempt 5 (overall live attempt 7) ran from clean evidence revision
  `fcb36c1b5f7938afe34a7ebca492a7ef04109731` under standing authorization. Redacted preflight passed, and the complete journey **PASS** 1/1 in
  35.7s (test body 34.7s): credential field separation; real login/session and profile selection; Search → Details → My List → Library → Details;
  public Sintel production Player; pointer pass-through; Pause→Play; seek past the resume threshold; fullscreen pointer and Space re-entry; Player
  exit/video removal; hard reload; exact `Resume`; restored-position assertion; second Player exit/video removal; logout; and browser Back remaining
  on Login.
- All 16 required TMDB endpoint classes were observed at least once and every observed response was 2xx, including application-driven session
  `DELETE`. The captured session was non-null, response JSON confirmed cleanup success, browser local/session storage was cleared in `finally`, and
  no secret value was printed or retained.
- Candidate 7 production and its complete non-live pass remain unchanged. The live external gate and mandatory cleanup are now **PASS**.

## Review and release gates

- Current capped P0/P1 review status: **PASS across architecture/backend/DI, lifecycle/input/accessibility, security/release/E2E, the bounded
  production HtmlElementView host-pointer correction, and the Candidate 7 test-only navigation delta**. Candidate 7 non-live execution is green;
  the live journey and cleanup are also green. WEB-04D is accepted for primary fast-forward; Safari/macOS remains explicitly waived/not run.
- Capped architecture/backend/DI review: `PASS` — no P0/P1 or acceptance blocker.
- Capped lifecycle/input/accessibility review: initial `BLOCK` on hidden-controls key-handler modifier order; corrected regression passes 14/14 and
  delta re-review returned `PASS`.
- Capped security/release/E2E review: initial `BLOCK` on default-skipped player registrations, stale live placeholder behavior,
  malformed/sibling-prefix server paths, application-wide Shaka fallback scanning, and mixed observed/not-run wording. All bounded corrections plus
  production-video removal assertions were delta-reviewed `PASS`.
- Failed frozen candidate: `ace71461c4914716509e1488a311110d7a20844d`; it is not eligible for acceptance.
- Failed Candidate 2: `d9a05ce7e4594f42efccc8c02d2c9b0ec6003f60`; 183/186 passed with 3 terminal-diagnostic failures and zero skips.
- Failed Candidate 3: `9021e94eb2810463c0f9e9066cb0ec9a9e65f9c1`; 185/186 passed with 1 terminal expiry diagnostic and zero skips.
- Candidate 4: `dc066a26effde71eabfc66ca590f37ea976805f7`; historical non-live pass, not acceptance-eligible after two failed live attempts exposed the
  production host-pointer blocker.
- Candidate 5 production base: `a6f4b64b2ab75ec3439623bfd56b31b759badfef`; complete matrix `FAIL` in 26.1m at 183 passed / 3 failed /
  0 skipped; not acceptance-eligible.
- Candidate 6: `308742ad6ef3574a0819ba6424f7e2d8dd5b45d1`; complete matrix `FAIL` in 26.0m at 185 passed / 1 failed / 0 skipped;
  not acceptance-eligible.
- Candidate 7: `1cb7ca253182f5f61ed07e7c9905f18e1307c469`; complete non-live gate `PASS`; live attempts 1–4 `FAIL` at 0/1 in 50.4s, 59.9s,
  58.4s, and 56.2s; live attempt 5 `PASS` at 1/1 in 35.7s with application cleanup.
- Production/Binaryen distribution and artifact validator: candidates 1, 2, 3, 4, 5, 6, and 7 `PASS`; Candidates 6 and 7 reused Candidate 5's
  unchanged production artifact.
- Complete Chromium/Firefox/WebKit matrix: Candidate 1 `FAIL` (144/186), Candidate 2 `FAIL` (183/186), Candidate 3 `FAIL` (185/186), Candidate 4
  historical `PASS` (186/186, zero failures/skips/retries), Candidate 5 `FAIL` (183/186, zero skips), and Candidate 6 `FAIL`
  (185/186, zero skips); Candidate 7 `PASS` (186/186, zero failures/skips/retries).
- Player screenshot inspection: Candidate 1 correction screenshots inspected for focus evidence; Candidate 4 historical and Candidate 7 current
  production controls/settings captures at both required viewports `PASS` with no P0/P1 finding.
- Android/root compatibility gate: Candidate 7 `PASS` in 58s, 2,350 actionable tasks (103 executed, 2,247 up-to-date), with player-mobile 12/12 and
  both requested APKs assembled.
- Manual current Safari/macOS: `WAIVED / NOT RUN` by explicit user decision on 2026-09-04; Playwright WebKit cannot substitute, and no pass is
  claimed.
- Final live TMDB/public-media journey and temporary-session cleanup: Candidate 4 attempt 1 `FAIL` at 0/1 in 49.7s; attempt 2 on `eb37969` `FAIL`
  at 0/1 in 49.1s with native video still unpaused after the stable projected Pause click. Candidate 7 attempt 1 `FAIL` at 0/1 in 50.4s because the
  harness clicked the buffering spinner under a false-enabled semantic projection; fallback session and storage cleanup were confirmed by control
  flow. Correction `acc13d034a4d05bb0b3f945d6463e5299b9d5e7f` is committed. Candidate 7 attempt 2 `FAIL` at 0/1 in 59.9s after corrected playback,
  seek/fullscreen, exit, and hard reload because centered focus/hover scaling never satisfied the all-edge bounds oracle; fallback cleanup was again
  confirmed. Center-stability correction `09c346ea40d02da5fbd3e94ee8ba17b765879c31` was exercised by Candidate 7 attempt 3, which `FAIL` at 0/1
  in 58.4s at the same restored-Details bounds checkpoint. Fresh-bounds correction
  `b70eabf328c000dc27b85374e57a601d6dee0a68` is committed, and credential-free exact transition evidence is 1/1 in 21.3s. Candidate 7 attempt 4
  `FAIL` at 0/1 in 56.2s because the restored CTA correctly rendered `Resume`, not the queried `Play`. Correction
  `1a7f2c3f57489c737a2c6c5de9db0773cb79a4cb` is committed. Candidate 7 attempt 5 `PASS` at 1/1 in 35.7s: the complete journey, all required 2xx
  endpoint classes, application logout/session deletion, and browser-storage cleanup passed.

## Primary integration closure

- `codex/kmp-migration` fast-forwarded from `f9e13558b3fc1c68db89ba9715932e8db80813ae` to accepted WEB-04D evidence tip
  `47141011f5d965bdf36bb9a646387ae3b35336f7` with `git merge --ff-only codex/web-04d-final-integration`.
- The integration worktree was clean, the accepted branch tracked no `.kotlin/**`, and the primary checkout retained exactly its pre-existing
  untracked `.kotlin/`. The generated `.android/` directory remained absent.
