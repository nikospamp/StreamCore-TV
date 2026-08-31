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
- Mobile/tablet/TV smoke flows and recorded performance comparison.
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
5. Repeat the exact KMP-00 build/test/smoke matrix and classify any delta as fixed, accepted/documented, or blocking.
6. Test persistence migration using an app installation containing pre-migration auth, library, search, and playback preference data.
7. Verify mobile/tablet/TV previews and device tests, especially TV D-pad focus and player cleanup.
8. Assemble debug and R8 variants for both clients.
9. Build benchmark/baseline-profile modules and confirm existing generated profiles are still consumed by the intended variants.
10. Run the existing controlled benchmark workflow and record before/after evidence. There is no numerical failure threshold, but repeatable
    regressions must be documented with likely cause and follow-up recommendation.
11. Update module dependency documentation and add permanent KMP rules to the project documentation:
    - Shared dependencies require KMP support.
    - Platform APIs live in target source sets.
    - Provider types never cross client boundaries.
    - New feature state/ViewModels start in `ui-common`.
    - Web support is not implied until a module gains and verifies `wasmJs` in WEB-01.
12. Record the Phase 1 green commit in `docs/kmp/migration-baseline.md` and mark the Android baseline comparison complete.

## Public API or Type Changes

No new API changes are authorized. This ticket integrates and validates the explicit changes from KMP-01 through KMP-06.

## Verification Commands

```powershell
.\gradlew.bat :app:assembleTmdbDebug
.\gradlew.bat :app:assembleClientBDebug
.\gradlew.bat :app:assembleTmdbReleaseR8
.\gradlew.bat :app:assembleClientBReleaseR8
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat :feature:home:ui-mobile:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:home:ui-tablet:compileDebugAndroidTestKotlin
.\gradlew.bat :feature:login:ui-tv:compileDebugKotlin
.\gradlew.bat :feature:search:ui-tv:compileDebugAndroidTestKotlin
.\gradlew.bat :benchmark:assemble
.\gradlew.bat :baselineprofile:assemble
rg -n "dagger\.|hilt|javax\.inject" app core client feature playback -g "*.kt" -g "*.kts"
```

Run all connected device tests available for the test device matrix, followed by the same controlled performance campaign recorded in KMP-00.

## Required Smoke Journeys

- Cold start and restored-session start for both providers.
- Login failure/success/logout/session expiry.
- Profile select/create/edit/delete and profile switch.
- Home loading/content/error/offline/refresh and progress decoration.
- Search discovery/recent/query/results/error.
- Library load/mutations/empty/error/profile isolation.
- Details initial content, refresh, recommendations, like/list mutation, trailers, and back.
- Player prepare/play/pause/seek/settings/filmstrip/exit/PiP/progress resume.
- TV drawer navigation, deterministic D-pad order, focused styling, and return-focus restoration.
- Phone/tablet adaptive routing and rotation/configuration changes.

## Acceptance Criteria

- Both debug and R8 provider variants assemble.
- All previously green tests remain green or have an explicitly approved baseline update.
- Pre-migration persisted data is readable.
- Every baseline smoke journey passes on its relevant platform.
- Benchmark/baseline-profile infrastructure builds and the non-blocking comparison is recorded.
- No Hilt/Dagger production dependency remains.
- No wasm target exists.
- The accepted Phase 1 commit is clean and ready for WEB-01.

## Handoff Checklist

- [ ] Baseline-vs-current matrix attached.
- [ ] Both provider/debug/R8 results included.
- [ ] Unit/common/device/lint results included.
- [ ] Persistence migration evidence included.
- [ ] Performance comparison location included.
- [ ] Remaining known issues and owners documented.
- [ ] Phase 1 green commit recorded.
- [ ] Final working tree is clean after committing this ticket.

