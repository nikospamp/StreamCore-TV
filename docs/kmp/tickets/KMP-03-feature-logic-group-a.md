# KMP-03 — Migrate Feature Logic Group A

## Goal

Convert the portable login, profiles, details, and home data/domain layers to KMP while preserving their existing contracts and Android behavior.

## Context

These modules are mostly JVM libraries with clean core dependencies and no required Android runtime services. They form one independent branch above
the migrated core modules and can be implemented concurrently with KMP-04.

## Dependencies and Parallelization

- **Depends on:** KMP-02.
- **Parallel with:** KMP-04.
- **Blocks:** KMP-05.
- **Ownership rule:** This ticket must not modify search, library, player, playback, provider, root convention, or version-catalog files.

## In-Scope Modules

- `:feature:login:data`
- `:feature:login:domain`
- `:feature:profiles:data`
- `:feature:profiles:domain`
- `:feature:details:data`
- `:feature:details:domain`
- `:feature:home:domain`

## Non-Goals

- No `ui-common` or platform UI changes.
- No provider repository implementation changes.
- No navigation changes.
- No new targets beyond common plus Android.
- No model consolidation or feature-boundary redesign.

## Implementation Tasks

1. Apply the plain KMP Android convention from KMP-02 to every in-scope module.
2. Move portable production sources from `src/main/kotlin` to `src/commonMain/kotlin`.
3. Move portable unit tests to `src/commonTest/kotlin` and replace JUnit-only APIs with `kotlin.test` where required for common execution.
4. Enable `androidHostTest` only for a test that genuinely requires an Android/JVM runtime; document why it cannot be common.
5. Re-declare project and library dependencies under the correct `commonMain`, `commonTest`, or `androidMain` source set.
6. Remove obsolete JVM toolchain/plugin configuration from the converted modules; inherit JVM 11 Android configuration from the convention.
7. Keep request/draft/validation models and use cases in their existing modules and packages. Do not combine files or change visibility without
   compilation requiring it.
8. Ensure Koin definitions created in KMP-01 compile from common code and use only Koin core APIs.
9. Preserve explicit block-bodied functions, immutable collections, repository interfaces, and current result/error semantics.
10. Add focused tests where source conversion exposes an untested common invariant:
    - Login credential normalization/validation.
    - Profile draft validation and editor-mode behavior.
    - Details request and repository delegation.
    - Home row loading and mapping policy.

## Public API or Type Changes

No intended API changes. Existing Kotlin packages, repository interfaces, model names, function signatures, and serialized shapes remain stable.

If compilation requires widening visibility for a cross-module KMP dependency, prefer an explicit narrow public contract over publishing
provider/internal implementation types. Document every such change.

## Verification Commands

```powershell
.\gradlew.bat :feature:login:data:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:login:domain:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:profiles:data:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:profiles:domain:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:details:data:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:details:domain:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:home:domain:compileCommonMainKotlinMetadata
.\gradlew.bat :feature:login:domain:allTests
.\gradlew.bat :feature:profiles:domain:allTests
.\gradlew.bat :feature:details:domain:allTests
.\gradlew.bat :feature:home:domain:allTests
.\gradlew.bat :app:compileTmdbDebugKotlin
.\gradlew.bat :app:compileClientBDebugKotlin
rg -n "^import (android|java)\." feature/login/data/src/commonMain feature/login/domain/src/commonMain feature/profiles/data/src/commonMain feature/profiles/domain/src/commonMain feature/details/data/src/commonMain feature/details/domain/src/commonMain feature/home/domain/src/commonMain -g "*.kt"
```

## Test Scenarios

- Login valid/invalid credentials retain current error and submit behavior.
- Profile create/edit/delete validation retains all parental/kids-profile invariants.
- Details request construction retains profile/content/source-row identity.
- Home use case propagates success/failure without provider leakage.
- Koin modules resolve use cases with fake repositories in common tests.

## Acceptance Criteria

- Every in-scope module compiles as common metadata and Android.
- All migrated tests pass as common tests unless a documented Android-only reason exists.
- Both Android flavors compile against the migrated contracts.
- No `android.*`, `java.*`, Hilt/Dagger, provider DTO, or Android resource usage exists in the new common source sets.
- KMP-04 can merge before or after this ticket without file conflicts outside Gradle lock/cache outputs.

## Handoff Checklist

- [ ] Source-set moves listed by module.
- [ ] Any non-common test justified.
- [ ] Any visibility/API changes documented.
- [ ] Common and Android compilation results included.
- [ ] Both app flavor compilation results included.
- [ ] No KMP-04/root files modified.
- [ ] Final working tree is clean after committing this ticket.

