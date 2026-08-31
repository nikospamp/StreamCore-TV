# KMP-00 — Remediate, Freeze, and Document the Android Baseline

## Goal

Create a reproducible, evidence-backed Android baseline before changing dependency injection, Gradle plugins, source sets, or production architecture.

## Context

The first baseline pass identified two host blockers and no device was connected. This ticket is therefore reopened. It completes only after the
specific Login/default-state and design-token verifier defects below are fixed, the expanded host gate is green with counts, and device evidence is
recorded. The accepted commit becomes the comparison point for every later Android regression assessment.

## Dependencies and Parallelization

- **Prerequisite:** The current performance/baseline-profile work is committed by its owner.
- **Blocks:** KMP-01 and every later ticket.
- **Parallelization:** None. One owner records the canonical baseline.

## In Scope

- Current module/dependency inventory.
- Android build and test health for both providers.
- Representative mobile/tablet/TV smoke behavior.
- Benchmark/baseline-profile buildability and existing performance evidence.
- A checked-in baseline report at `docs/kmp/migration-baseline.md`.
- Narrow remediation of the two confirmed baseline blockers.

## Non-Goals

- No KMP plugins or source-set changes.
- No DI migration.
- No production fixes except clearing the confirmed hardcoded Login credentials/defaults described below.
- No build-logic changes except repairing `verifyDesignTokens` input isolation and source coverage described below.
- No regeneration of baseline profiles unless the existing project workflow already requires it.

## Implementation Tasks

1. Confirm only the KMP planning/baseline documentation is pending. Do not stash, restore, or overwrite unrelated/user work.
2. Fix the confirmed Login baseline defect: `LoginUiState` production defaults for identifier/password must be empty. Keep sample credentials only as
   explicit values inside previews/tests. The existing invalid-submit test must pass; do not weaken it.
3. Fix `verifyDesignTokens` for Gradle 9 by passing an explicit provider/set of individual Kotlin source files rather than broad `core`/`feature`/`app`
   FileTree roots that overlap generated build outputs. Cover `src/main/kotlin`, Kotlin files under `src/main/java`, `commonMain`, `androidMain`, and
   `wasmJsMain`; exclude build/generated/test trees. Retain incremental inputs and configuration-cache compatibility.
4. Run `verifyDesignTokensLogFiles` independently and then root `check --continue`. Both must execute; lint must remain green. Record the exact
   checked-file count and representative paths.
5. Record executed test counts per module from XML reports. The provisional inventory is 185 tests/1 failure; after the Login fix the accepted
   baseline must retain the same tests with zero failures. A later green task with fewer or zero expected tests is not equivalent.
6. Record the accepted baseline commit hash, toolchain versions, Android SDK/JDK paths, connected test devices, and date.
7. Retain/verify the included Gradle module classification, dependency graph, and provider/flavor wiring already captured in the report.
8. Run the build/test matrix below and capture pass/fail plus the first actionable failure for each failed command.
9. Smoke-test the critical journeys on at least one mobile target, one tablet target, and one Android TV target. Emulator evidence is acceptable;
   record API/device fingerprints. Run the full reference journey matrix with TMDB and at minimum boot/auth/profile/home/details/player provider smoke
   with ClientB:
    - Login and logout/session restoration.
    - Profile selection, creation, editing, and deletion.
    - Home loading/refresh and navigation.
    - Search discovery, query, recent searches, and result selection.
    - Library states and details mutations.
    - Details, trailer launch, recommendations, and back navigation.
    - Player start, seek, pause/resume, settings, exit, and progress restoration.
    - TV D-pad focus traversal and focus restoration.
10. Record existing controlled benchmark results without changing thresholds.
11. Record that CI is currently absent and identify the device/emulator owners used for the local release gate.
12. Update `docs/kmp/migration-baseline.md`: change status to `Accepted`, replace the provisional commit with the remediation commit, include host/device
    counts/results, and make its repeatable gate identical to KMP-07.
13. Use two commits to avoid a self-referential hash: first commit the two remediation changes and run the complete gate at that exact commit; then
    commit the documentation-only accepted report that records the tested remediation hash. Create `codex/kmp-migration` from the report commit.

## Public API or Type Changes

- `LoginUiState` retains its type/fields but identifier/password defaults become empty.
- `verifyDesignTokens` input construction changes without changing the design-token policy.

## Verification Commands

Run from the repository root:

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
.\gradlew.bat :benchmark:assemble
.\gradlew.bat :baselineprofile:assemble
```

Run the repository's existing controlled performance campaign exactly as documented. Record results; do not make performance a pass/fail threshold in
this ticket.

## Acceptance Criteria

- The baseline commit and working environment are recorded.
- Executed test counts and design-token checked-file counts are recorded.
- Root `check`, all tests, lint, and design-token verification pass with no expected-failure allow-list.
- Both Android flavors pass.
- Critical mobile/tablet/TV journeys have recorded pass/fail outcomes on identified targets; `Not run` is not accepted.
- Benchmark and baseline-profile buildability is known.
- `docs/kmp/migration-baseline.md` is sufficient for KMP-07 to repeat the same gate.
- Only the two explicitly authorized baseline remediations changed production/build configuration.

## Handoff Checklist

- [ ] Baseline commit hash included.
- [ ] Build/test command results included.
- [ ] Executed test counts included by module.
- [ ] Design-token checked-file count/sample paths included.
- [ ] Smoke-test matrix included.
- [ ] Performance evidence location included.
- [ ] Known baseline failures clearly separated from future regressions.
- [ ] Baseline report status is `Accepted`, not provisional.
- [ ] Tested remediation commit and subsequent report commit are both recorded; integration branch starts from the report commit.
- [ ] Mobile, tablet, and TV device/emulator fingerprints and results included.
- [ ] No unrelated files changed.
- [ ] Final working tree is clean after committing this ticket.
