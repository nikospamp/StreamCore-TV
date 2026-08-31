# KMP-00 — Freeze and Document the Android Baseline

## Goal

Create a reproducible, evidence-backed Android baseline before changing dependency injection, Gradle plugins, source sets, or production architecture.

## Context

The repository currently contains performance, benchmark, tracing, and baseline-profile work. This ticket starts only after that work has been
reviewed and committed. The baseline is the comparison point for every later Android regression assessment.

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

## Non-Goals

- No KMP plugins or source-set changes.
- No DI migration.
- No production fixes unless a previously unknown baseline blocker is separately approved.
- No regeneration of baseline profiles unless the existing project workflow already requires it.

## Implementation Tasks

1. Confirm `git status --short` is empty. If it is not, stop and report the exact paths; do not stash, restore, or commit user work.
2. Record the baseline commit hash, toolchain versions, Android SDK/JDK paths used, connected test devices, and date.
3. Inventory every included Gradle module and classify it as Android application, Android library, JVM library, Android test, benchmark, or
   baseline-profile module.
4. Record the existing project dependency graph and the current provider/flavor wiring.
5. Run the build/test matrix below and capture pass/fail plus the first actionable failure for each failed command.
6. Smoke-test the critical journeys using the currently supported device/emulator matrix:
    - Login and logout/session restoration.
    - Profile selection, creation, editing, and deletion.
    - Home loading/refresh and navigation.
    - Search discovery, query, recent searches, and result selection.
    - Library states and details mutations.
    - Details, trailer launch, recommendations, and back navigation.
    - Player start, seek, pause/resume, settings, exit, and progress restoration.
    - TV D-pad focus traversal and focus restoration.
7. Record existing controlled benchmark results without changing thresholds.
8. Create `docs/kmp/migration-baseline.md` with the evidence, known failures, and the exact commands required by KMP-07.

## Public API or Type Changes

None.

## Verification Commands

Run from the repository root:

```powershell
.\gradlew.bat :app:assembleTmdbDebug
.\gradlew.bat :app:assembleClientBDebug
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
.\gradlew.bat test
.\gradlew.bat :feature:home:ui-mobile:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:home:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:login:ui-tv:compileDebugKotlin
.\gradlew.bat :benchmark:assemble
.\gradlew.bat :baselineprofile:assemble
```

Run the repository's existing controlled performance campaign exactly as documented. Record results; do not make performance a pass/fail threshold in
this ticket.

## Acceptance Criteria

- The baseline commit and working environment are recorded.
- Both Android flavors either pass or have pre-existing failures documented with reproduction commands.
- Critical mobile/tablet/TV journeys have recorded outcomes.
- Benchmark and baseline-profile buildability is known.
- `docs/kmp/migration-baseline.md` is sufficient for KMP-07 to repeat the same gate.
- No production source or build configuration changed.

## Handoff Checklist

- [ ] Baseline commit hash included.
- [ ] Build/test command results included.
- [ ] Smoke-test matrix included.
- [ ] Performance evidence location included.
- [ ] Known baseline failures clearly separated from future regressions.
- [ ] No unrelated files changed.
- [ ] Final working tree is clean after committing this ticket.

