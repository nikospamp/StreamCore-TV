# WEB-04D — Integrate Playback and Accept the Web Release

## Dispatch and Merge Inputs

- **Depends on:** accepted WEB-04A engine, WEB-04B UI, and WEB-04C test/release commits based on the same reviewed contract freeze.
- **Branch:** `codex/web-04d-final-integration` created from exact accepted `WEB-03F_COMMIT` supplied by the root.
- The root records exact 40-character `WEB-04A_ACCEPTED_COMMIT`, `WEB-04B_ACCEPTED_COMMIT`, and `WEB-04C_ACCEPTED_COMMIT` inputs. Missing, dirty,
  unreviewed, rebased, or contract-divergent inputs block integration.
- **Mandatory order:** merge/cherry-pick WEB-04A → WEB-04B → WEB-04C. Feature owners do not resolve integration conflicts.

## Ownership and Integration Scope

The integration owner exclusively owns `webApp/**`, necessary `settings.gradle.kts`/root/catalog/convention changes, graph/navigation wiring,
integration-only conflict fixes, final evidence, and release documentation reconciliation. Return non-trivial feature/engine fixes to the owning
ticket. Provider/data, unrelated Android UI, credentials, generated config, node modules, browser binaries, and committed distributions are forbidden.

- Register `WebPlaybackSessionFactory`, never a session. Wire `WebPlayerRoute` into `/player/{contentId}` and replace WEB-03's placeholder.
- Direct reload resolves `PlaybackRequestModel` from backend-agnostic repositories/IDs or returns safely to Details. Back/Escape exits once and restores
  Details focus.
- Preserve profile-isolated progress/resume/completion through browser DataStore. Fullscreen exit restores control/focus state.
- Verify state/command mapping, autoplay activation, sanitized error/retry, track controls, no-filmstrip behavior, repeated cleanup, and no retained
  video DOM node, Shaka listener/instance, session, timer, job, or collector.
- Reconcile `/config.json`, MIME/CORS/CSP/cache/static fallback docs with the actual frozen artifact; package no real token/config.

## Review and Serialized Candidate Gate

Run focused tests for integration changes, freeze production, then perform read-only architecture, backend-boundary, Media3 compatibility,
lifecycle/leak, accessibility, security, and release review. Only after review run one candidate sequentially through the shared queue:

```powershell
.\gradlew.bat :playback:web:compileKotlinWasmJs
.\gradlew.bat :feature:player:ui-web:compileKotlinWasmJs
.\gradlew.bat :feature:player:ui-common:testAndroidHostTest
.\gradlew.bat :webApp:wasmJsBrowserTest
.\gradlew.bat :webApp:wasmJsBrowserDistribution
Set-Location webApp/e2e
npm ci
npx playwright test
Set-Location ../..
.\gradlew.bat :playback:media3:compileDebugKotlin :feature:player:ui-mobile:testDebugUnitTest :app:assembleTmdbDebug :app:assembleClientBDebug check -PverifyDesignTokensLogFiles=true --continue --max-workers=1
```

Record one frozen commit, build kind, exact output directory/contents, command/test counts, failures/skips, and complete Chromium/Firefox/WebKit
browser/viewport coverage. Inspect player screenshots; file existence is not visual acceptance. A production change creates a new candidate. A
test-only fix receives only affected reruns and cannot turn the prior matrix into an unexecuted full-pass claim.

## Final External Gates

After every non-live gate passes:

1. Coordinate and record a manual current Safari/macOS pass. If no qualified machine/operator evidence exists, release remains blocked; automated
   WebKit is not Safari evidence.
2. Run one final redacted TMDB journey through the ignored WEB-02 wrapper: Login → Profiles → Details/Home → Player → play/seek/fullscreen → Back →
   hard reload/resume → logout, then mandatory temporary-session cleanup. Never read, print, copy, commit, or screenshot secrets/session/account data.
   Obtain action-time approval when Codex would transmit; user-manual wrapper execution exposes only its redacted outcome.

## Acceptance Handoff

Accept only when aggregate WEB-04 criteria have observed evidence on the final commit: API/Media3 compatibility, engine/UI behaviors, cleanup,
candidate matrix, inspected visuals, artifact/config/MIME/CORS/CSP/cache/routing contract, Android/root gates, manual Safari, final live journey and
cleanup. Report deferred DRM/ClientB/PiP/casting/offline/mobile work, changed files/APIs, exact results/counts, and status. Otherwise label the gate
`blocked`, `failed`, or `not-run`; never infer passing from artifacts, screenshots, mocks, WebKit, or focused reruns.
