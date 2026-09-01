# KMP-07 — Android Parity and Phase 1 Release Gate

## Goal

Prove that the Android product is functionally equivalent to the frozen baseline after Koin and KMP migration, and establish the green commit from
which web work begins.

## Context

No wasm target may be introduced before this ticket passes. This ticket owns integration stabilization, full Android verification, documentation, and
the final Phase 1 commit/tag.

## Dependencies and Parallelization

- **Depends on:** KMP-06.
- **Blocks:** WEB-01.
- **Parallelization:** One integration owner. Test execution may be delegated by device/platform, but fixes merge through the owner.

## In Scope

- Root/app/platform integration fixes caused by completed migration tickets.
- Both Android providers and relevant build types.
- Common/unit/device tests, lint, benchmark/baseline-profile buildability.
- Mobile/tablet/TV smoke flows and optional informational performance comparison.
- KMP architecture and contribution documentation.

## Non-Goals

- No wasm/web target.
- No new feature or visual redesign.
- No opportunistic dependency upgrades.
- No performance optimization unless required to fix a functional regression.

## Implementation Tasks

1. Rebase/merge every prerequisite in the required order and confirm no temporary migration compatibility layer remains.
2. Resolve Android integration failures without moving platform-only code back into common source sets.
3. Verify every KMP module uses the Android-KMP plugin and source-set conventions; Android-only modules retain Android plugins.
4. Verify Koin graphs for TMDB and ClientB with strict duplicate/override checks.
5. Run the accepted KMP-00-equivalent build/test/smoke matrix. Map migrated JVM/Android unit-test tasks to `testAndroidHostTest`, then compare tests by
   owning module and executed test-case count rather than task name. Include design-token checked-file counts and classify any delta as explicitly
   accepted or blocking. A green task with fewer/zero expected tests is blocking.
6. Test persistence migration using an app installation containing pre-migration auth, library, search, and playback preference data.
7. Verify mobile/tablet/TV previews and device tests, especially TV D-pad focus and player cleanup.
8. Assemble debug and R8 variants for both clients.
9. Build benchmark/baseline-profile modules and confirm existing generated profiles are still consumed by the intended variants.
10. Optionally run the existing controlled benchmark workflow and record before/after evidence when a suitable physical device is available. The
    campaign is informational and non-blocking for Phase 1 acceptance; do not substitute emulator timing or fabricate a same-commit result. If run,
    document repeatable regressions with likely cause and follow-up recommendation.
11. Update `AGENTS.md`, `MODULE_DEPENDENCY_GRAPH.md`, and `DESIGN.md` with the final DI, source-set, preview, resource, testing, module, and web-readiness
    rules:
    - Shared dependencies require KMP support.
    - Platform APIs live in target source sets.
    - Provider types never cross client boundaries.
    - New feature state/ViewModels start in `ui-common`.
    - Web support is not implied until a module gains and verifies `wasmJs` in WEB-01.
12. Record the Phase 1 green commit in `docs/kmp/migration-baseline.md` and mark the Android baseline comparison complete.
13. Treat any unresolved functional, persistence, test-count, design-verifier, graph, or build-variant regression as a stop condition. Do not merge
    KMP-07 or begin WEB-01; return the fix to the owning Phase 1 ticket and retain the last green `codex/kmp-migration` commit as rollback.

## Public API or Type Changes

No new API changes are authorized. This ticket integrates and validates the explicit changes from KMP-01 through KMP-06.

## Verification Commands

```powershell
.\gradlew.bat :app:assembleTmdbDebug
.\gradlew.bat :app:assembleClientBDebug
.\gradlew.bat :app:assembleTmdbReleaseR8
.\gradlew.bat :app:assembleClientBReleaseR8
.\gradlew.bat check -PverifyDesignTokensLogFiles=true
.\gradlew.bat testAndroidHostTest
.\gradlew.bat lint
.\gradlew.bat :feature:home:ui-mobile:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:home:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:login:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:search:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :benchmark:assemble
.\gradlew.bat :baselineprofile:assemble
rg -n "dagger\.|hilt|javax\.inject|dagger-hilt|hilt-android" app core client feature playback build.gradle.kts settings.gradle.kts gradle/libs.versions.toml -g "*.kt" -g "*.kts" -g "*.toml"
```

Run all connected device tests available for the test device matrix. A same-protocol physical performance campaign may follow as optional
informational evidence; its absence does not block Phase 1 acceptance.

## Performance governance decision

On 2026-09-01 the project owner explicitly chose to skip the new physical before/after campaign and proceed. This amends KMP-07 acceptance only:

- benchmark and Baseline Profile infrastructure must still build;
- existing generated profiles must still be consumed and must not be regenerated merely for this gate;
- the accepted historical physical-device campaign remains informational context;
- no new performance result is claimed, inferred from emulator timing, or fabricated;
- a future campaign may use the documented protocol with a new run identity, but it is not a Phase 1 blocker.

## Required Smoke Journeys

- Cold start and restored-session start for both providers.
- Login failure/success/logout/session expiry.
- Profile select/create/edit/delete and profile switch.
- Home loading/content/error/offline/refresh and progress decoration.
- Search discovery/recent/query/results/error.
- Library load/mutations/empty/error/profile isolation.
- Details initial content, refresh, recommendations, like/list mutation, trailers, and back.
- Player prepare/play/pause/seek/settings/filmstrip/exit/PiP/progress resume.
- Cold and warm artwork loading, placeholder/error handling, and cache-hit behavior after the Coil Ktor3 migration.
- TV drawer navigation, deterministic D-pad order, focused styling, and return-focus restoration.
- Phone/tablet adaptive routing and rotation/configuration changes.

## Acceptance Criteria

- Both debug and R8 provider variants assemble.
- All previously green tests remain green or have an explicitly approved baseline update.
- Every migrated common test executes through `testAndroidHostTest`; per-module counts match or exceed KMP-00.
- `verifyDesignTokens` checks migrated `commonMain`/`androidMain` UI sources and does not pass with zero checked files.
- Pre-migration persisted data is readable.
- Every baseline smoke journey passes on its relevant platform.
- Benchmark/baseline-profile infrastructure builds. Any new controlled comparison is optional, informational, and non-blocking.
- No Hilt/Dagger production dependency remains.
- No wasm target exists.
- The accepted Phase 1 commit is clean and ready for WEB-01.

## Handoff Checklist

- [x] Baseline-vs-current matrix attached.
- [x] Both provider/debug/R8 results included.
- [x] Unit/common/device/lint results included.
- [x] Executed test counts and design-token checked-file counts compared with KMP-00.
- [x] Persistence migration evidence included.
- [x] Historical performance context and owner-approved optional campaign decision included.
- [x] Remaining known issues and owners documented.
- [x] Phase 1 green production commit recorded.
- [x] `AGENTS.md`, `MODULE_DEPENDENCY_GRAPH.md`, and `DESIGN.md` updated.
- [x] Final working tree is clean after committing this ticket.
