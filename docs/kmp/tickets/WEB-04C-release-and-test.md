# WEB-04C — Prepare Release Tests and Deployment Contract

## Dispatch

- **Depends on/base:** exact reviewed `WEB-04_CONTRACT_FREEZE_COMMIT` published by WEB-04A; verify `HEAD`. No fallback base.
- **Branch:** `codex/web-04c-release-test`.
- **Concurrency:** test/docs-only work may run with WEB-04A engine and WEB-04B UI while the root stays active. Executions remain serialized.

## Path Reservation

Owned: `webApp/e2e/**`, `webApp/src/wasmJsTest/**` only for player/release integration fixtures, `docs/kmp/web-release.md` (new), and a dedicated
WEB-04C evidence document. Forbidden: all production feature/engine/app Kotlin and resources, `playback/**`, `feature/**`, `core/**`, root/settings/
catalog/convention files, real config, credentials, generated distribution, node modules, browser binaries, and unreserved screenshots.

Tests consume the frozen playback contract through deterministic fakes/fixtures. Test code and docs must not import provider DTOs or embed token,
account, session, credential, signed media URL, or machine-local path data.

## Expected Deliverables

- Canvas-safe Playwright scenarios for successful playback state, autoplay rejection/user activation, network error/retry, seek/scrub, speed,
  quality/audio/subtitle selection, resume, fullscreen/exit focus, Back/Escape, invalid direct player URL, no-filmstrip fallback, and repeated
  enter/play/close leak checks.
- Stable semantic/native selectors following `docs/kmp/web-testing.md`; no fixed-coordinate product interactions or assumptions that canvas children
  are DOM nodes. Fixture readiness is explicit and failures retain focused diagnostics without secrets.
- Artifact validation that records the production output directory and expected HTML/JS/Wasm/assets/config-example contents, rejects real config or
  tokens, and checks versioned asset references without committing generated files.
- `docs/kmp/web-release.md` documents `/config.json`, Wasm/JS/media MIME types, TMDB/image/media CORS, CSP for connect/img/media/worker/script sources
  including the chosen Shaka ESM adapter, immutable hashed-asset versus HTML/config cache policy, static fallback/deep-link routing, and rollback.
  It recommends no host and contains no deploy credential.
- Manual current Safari/macOS checklist with browser/OS/version, public Sintel playback, controls, fullscreen, resume/error and cleanup observations.
  A template or `not-run` row is not a passing manual result.

## Focused Verification and Evidence

Before A/B integration, run only fixture/parser/lint checks that do not require their production code. If a focused browser fixture is executable,
the build-queue owner may run one development Chromium 1280×720 scenario. Record exact command/counts and skipped/not-run cases.

Do not run `npm ci`, start Playwright servers, build Binaryen, execute a full matrix, use TMDB credentials, or claim an artifact/manual Safari pass in
this ticket. WEB-04D owns those serialized candidate/final gates.

Acceptance requires read-only security/release review, no production edits, a reviewed `WEB-04C_ACCEPTED_COMMIT`, exact fixture/deployment contract,
and evidence clearly separated into planned versus observed results.
