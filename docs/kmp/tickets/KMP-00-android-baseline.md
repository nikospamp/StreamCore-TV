# KMP-00 — Remediate, Freeze, and Document the Android Baseline

## Goal

Create a reproducible, evidence-backed Android baseline before changing dependency injection, Gradle plugins, source sets, or production architecture.

## Context

Host remediation is complete at `8bc11dc4300e55822bbae689dfdad271ef3fe769`: root `check` is green, 185 tests pass, and `verifyDesignTokens`
checks 266 production files. Phone/tablet/TV execution then exposed functional baseline failures. This ticket remains reopened and completes only
after KMP-00B through KMP-00G fix those failures and KMP-00H reruns the complete host/device gate and commits an `Accepted` report.

## Dependencies and Parallelization

- **Prerequisite:** The current performance/baseline-profile work is committed by its owner.
- **Sub-ticket order:** KMP-00B -> [KMP-00C || KMP-00D || KMP-00E || KMP-00F] -> KMP-00G -> KMP-00H.
- **Blocks:** KMP-01 and every later ticket until KMP-00H passes.
- **Parallelization:** KMP-00C through KMP-00F are parallel-safe after KMP-00B is green and the integration owner freezes shared app/build ownership.
  KMP-00G and KMP-00H remain serial integration gates.

## In Scope

- Current module/dependency inventory.
- Android build and test health for both providers.
- Representative mobile/tablet/TV smoke behavior.
- Benchmark/baseline-profile buildability and existing performance evidence.
- A checked-in baseline report at `docs/kmp/migration-baseline.md`.
- Completed host/Login/design-token remediation plus the narrowly owned functional fixes defined by KMP-00B through KMP-00G.

## Non-Goals

- No KMP plugins, multiplatform targets, or source-set migration. New Android-only tablet/TV UI modules are authorized only by KMP-00C/KMP-00F.
- No DI migration.
- No production fix outside KMP-00B through KMP-00G.
- No build-logic change beyond the completed `verifyDesignTokens` input isolation/source coverage remediation.
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
5. Record executed test counts per module from XML reports. The remediated inventory is 185 tests/0 failures; final acceptance must retain those tests
   plus legitimate sub-ticket coverage. A later green task with fewer or zero expected tests is not equivalent.
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
12. Execute KMP-00B first, KMP-00C through KMP-00F in parallel with disjoint feature ownership, then KMP-00G; the integration owner alone edits shared
    app navigation, settings, and app dependency files.
13. KMP-00H updates `docs/kmp/migration-baseline.md` to `Accepted` only after every host command and every device journey passes, commits the
    documentation-only accepted report, and creates `codex/kmp-migration` from that report commit.

## Reopened Functional Sub-Tickets

| Ticket | Ownership | Blocking outcome |
|---|---|---|
| [KMP-00B](KMP-00B-auth-session-lifecycle.md) | App auth + platform profile Sign out | Login/logout/restored-session lifecycle passes |
| [KMP-00C](KMP-00C-tablet-top-level-navigation.md) | Tablet app shell + Search/Library tablet modules | Tablet Home/Search/Library are reachable and functional |
| [KMP-00D](KMP-00D-tablet-details-player-parity.md) | Tablet Details/trailer/player routing | Tablet trailer and complete player journey pass |
| [KMP-00E](KMP-00E-tv-profile-deletion.md) | TV profiles/editor deletion | Complete TV profile CRUD passes with D-pad |
| [KMP-00F](KMP-00F-tv-details-player-parity.md) | TV Details/trailer + new TV player | Complete TV trailer/player journey passes |
| [KMP-00G](KMP-00G-tv-return-focus-restoration.md) | App drawer/navigation + TV return focus | Exact originating content focus is restored |
| [KMP-00H](KMP-00H-android-baseline-reacceptance.md) | Integration evidence/report/branch only | Baseline becomes Accepted and `codex/kmp-migration` is created |

## Public API or Type Changes

- `LoginUiState` retains its type/fields but identifier/password defaults become empty.
- `verifyDesignTokens` input construction changes without changing the design-token policy.
- Functional/public API changes are limited to and documented by KMP-00B through KMP-00G.

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
- Every critical mobile/tablet/TV journey passes on identified targets; Fail, Blocked, and `Not run` are not accepted.
- Benchmark and baseline-profile buildability is known.
- `docs/kmp/migration-baseline.md` is sufficient for KMP-07 to repeat the same gate.
- Only the completed host remediation and explicitly authorized KMP-00B through KMP-00G functional fixes changed production/build behavior.

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
