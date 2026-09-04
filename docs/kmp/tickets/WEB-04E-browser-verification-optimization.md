# WEB-04E — Optimize Browser Verification Timing

## Status

**QUEUED FOLLOW-UP.** Candidate 9 is accepted and closure-ready; this ticket does not reopen, modify, or qualify that acceptance. Dispatch only from
the final accepted WEB-04D integration SHA after closure. Candidate 9 production and evidence remain immutable inputs.

## Dispatch and Dependency

- **Depends on:** the accepted WEB-04D Candidate 9 integration and its committed evidence. Do not amend Candidate 9 or fold this optimization into
  its closure commits.
- **Branch:** `codex/web-04e-browser-verification-optimization` from the exact accepted WEB-04D integration SHA.
- **Owner:** one test-infrastructure owner. The root retains the serialized build queue, evidence classification, and merge decision.

## Scope and Path Reservation

Preserve all 33 unique browser scenarios while replacing repeated real-time player auto-hide waits with a deterministic Playwright diagnostic clock
or test-owned hidden-controls fixture. Keep exactly one release smoke on the real production auto-hide duration.

Owned: `webApp/e2e/**` and this ticket's evidence document. A narrowly scoped diagnostic fixture change requires a separate root decision and creates
a new production artifact; it is not implicitly authorized by this test-optimization ticket. Forbidden: production player behavior, playback APIs,
provider/data modules, dependency/catalog/build configuration, credentials, generated distributions, browser binaries, and committed test output.

The optimization must not exclude, merge, rename away, conditionally skip, or weaken any existing scenario. It must preserve browser/viewport
coverage, state assertions, input behavior, cleanup checks, diagnostic classification, and backend-agnostic boundaries.

## Implementation Contract

- Use deterministic time only inside the test harness. Advancing time must exercise the existing production state transition rather than assigning
  production state or bypassing the Player action path.
- Establish a reusable hidden-controls fixture for pointer- and keyboard-reveal cases, with explicit readiness before activation.
- Keep one real-clock auto-hide smoke that observes the production duration and proves the deterministic clock has not replaced release timing proof.
- Retain leak/disposal assertions and unknown-diagnostic failure behavior. Do not relax timeouts globally to conceal failures.
- Run all CPU-heavy commands serially through the root-owned queue.

## Risk-Tiered Verification

1. Inner loop: affected test-helper checks plus the targeted Chromium 1280×720 hidden-controls scenario, target about two minutes.
2. Browser-sensitive risk gate: the affected scenario in Chromium, Firefox, and WebKit at one viewport. Add the second viewport only if the helper
   changes geometry/responsive behavior.
3. Because the deterministic clock/fixture is shared harness behavior, freeze the test delta and run the complete matrix once before acceptance.
   Discovery must remain exactly 198 registrations: 33/33 in each of six browser/viewport projects, with Player 72, product 66, and runtime 60.
4. Test-only corrections after that run receive only the affected slice unless they alter global discovery, scheduling, global classifier defaults,
   or another shared fixture. Documentation-only corrections receive no executable rerun.

## Acceptance

- All 33 unique scenarios remain present with zero new skips or conditional exclusions.
- Deterministic hidden-controls cases no longer pay repeated 10-second waits, while one real-timing auto-hide smoke passes.
- The focused Chromium inner loop and one-viewport three-browser risk gate pass with exact counts recorded.
- The frozen shared-harness delta completes one serialized 198/198 matrix with 33/33 per project, Player 72/72, product 66/66, runtime 60/60,
  zero failures, zero skips, and zero retries.
- No production artifact, dependency, credential, provider boundary, or backend-agnostic contract changes unless the root explicitly reclassifies the
  work as a new production candidate.
