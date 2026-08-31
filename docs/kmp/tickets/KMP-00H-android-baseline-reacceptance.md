# KMP-00H — Rerun and Accept the Android Baseline

## Goal

Run the complete KMP-00 host/device gate after KMP-00B through KMP-00G, commit the canonical Accepted report, and create `codex/kmp-migration`.

## Context

Host remediation commit `8bc11dc4300e55822bbae689dfdad271ef3fe769` is green with 185 tests and 266 checked files, but the report remains provisional because
device acceptance exposed functional blockers. This ticket is documentation/integration verification only; production fixes belong to B-G.

## Dependencies and Parallelization

- **Depends on:** KMP-00B, the integrated KMP-00C through KMP-00F parallel wave, and KMP-00G on `codex/kmp-00-android-baseline`.
- **Blocks:** KMP-01 and all migration/web tickets.
- **Parallelization:** One integration owner. Host commands and device execution may be delegated, but evidence/report/commit ownership is singular.

## In Scope

- Full KMP-00 host gate at one exact candidate commit.
- TMDB reference matrix on phone/tablet/TV and ClientB provider smoke.
- Final test/verifier/device counts, evidence, report status, commits, and integration branch.

## Non-Goals

- No production fix, KMP/Koin/Wasm work, dependency upgrade, performance optimization, or Baseline Profile regeneration.
- Do not accept expected failures or `Not run`/Blocked journeys.

## Implementation Tasks

1. Confirm the worktree is clean and record the exact candidate commit, toolchain, `adb devices -l`, AVD fingerprints, resolutions, and orientations.
2. Run every host command below independently. Stop on any failure; do not amend production code in this ticket.
3. Read executed test counts from current XML reports. Require at least the 185-test baseline plus all legitimate B-G tests, with zero failures/errors and
   no expected nonzero task silently becoming zero.
4. Run `verifyDesignTokensLogFiles`; require at least 266 checked files and explain the exact increase from new tablet/TV production files.
5. Verify lint and both provider debug/R8 graphs, Android-test compilation, benchmark, and baseline-profile assembly. Do not regenerate profiles.
6. Run the full KMP-00 TMDB journey matrix on `Medium_Phone_API_36.1`, `Medium_Tablet`, and `Television_1080p`.
7. Credential handling remains: direct TMDB credential submission on phone; opaque session restoration may cover tablet/TV when needed, paired with
   ClientB generated-input login submission. Never decode/print/commit the session value.
8. Run ClientB boot/auth/profile/home/details/player smoke and the complete TV D-pad/drawer/player/focus flow.
9. Every journey must be Pass; Fail, Blocked, and `Not run` prevent acceptance.
10. Update `docs/kmp/migration-baseline.md` from `PROVISIONAL` to `Accepted`, refresh the module classification/dependency graph for the new tablet/TV
    modules, and record the tested candidate commit, command results, per-module counts, verifier count/paths, device matrix, journeys, evidence
    locations, and remaining non-blocking limitations.
11. Ensure the report's KMP-07 command block exactly matches `KMP-07-android-parity-gate.md`.
12. Commit only the final documentation as `docs: accept KMP Android migration baseline`.
13. Create `codex/kmp-migration` from that report commit. Do not start KMP-01.

## Public API or Type Changes

None. Any required production change returns to the owning B-G sub-ticket.

## Verification Commands

```powershell
.\gradlew.bat :app:assembleTmdbDebug
.\gradlew.bat :app:assembleClientBDebug
.\gradlew.bat :app:assembleTmdbReleaseR8
.\gradlew.bat :app:assembleClientBReleaseR8
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
.\gradlew.bat :feature:login:ui-common:testDebugUnitTest --tests "*LoginViewModelTest"
.\gradlew.bat verifyDesignTokensLogFiles --console=plain
.\gradlew.bat check -PverifyDesignTokensLogFiles=true --continue --console=plain
.\gradlew.bat :feature:home:ui-mobile:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:home:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:login:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:search:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:search:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:library:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:player:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :benchmark:assemble
.\gradlew.bat :baselineprofile:assemble
```

## Required Device Matrix

- Phone: login/logout/session, profile CRUD, Home, Search, Library, Details/trailer/recommendations/Back, complete touch player/progress restoration.
- Tablet: combined auth/session coverage, profile CRUD, adaptive Home/Search/Library navigation, Details/trailer/recommendations/Back, complete touch
  player/progress restoration.
- TV: combined auth/session coverage, profile CRUD including deletion, Home/Search/Library drawer navigation, Details/trailer/recommendations, complete
  D-pad player, Back, exact-item and drawer focus restoration.
- ClientB: at minimum boot/auth/profile/home/details/player; both graphs must be smokeable on every form factor touched by shared fixes.

## Acceptance Criteria

- Every host command passes at the recorded candidate commit.
- Tests are at least baseline 185 plus added B-G coverage, with zero failures/errors and reported zero-test tasks.
- Verifier checks at least 266 production files and the increase is attributable to expected new files.
- Every phone/tablet/TV reference journey passes; no expected-failure list remains.
- `migration-baseline.md` is `Accepted`, command-identical to KMP-07 where required, and sufficient for later parity comparison.
- Documentation commit is clean and `codex/kmp-migration` points to it.
- KMP-01 has not started.

## Handoff Checklist

- [ ] Candidate and report commit hashes included.
- [ ] Exact host commands/results included.
- [ ] Per-module test counts and verifier count/paths included.
- [ ] Device fingerprints and all journey outcomes included.
- [ ] Evidence locations included.
- [ ] Baseline Profile non-regeneration confirmed.
- [ ] `codex/kmp-migration` created from the report commit.
- [ ] Final worktree clean; no unrelated changes.
