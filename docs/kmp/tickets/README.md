# StreamCoreTV Kotlin Multiplatform Migration Backlog

## Purpose

This directory contains the implementation backlog for migrating StreamCoreTV to Kotlin Multiplatform without interrupting Android delivery, then
adding a TMDB-first web client whose visual language closely matches the Android TV client.

Every ticket is intended to be executable in a fresh agent/task with no hidden decisions. Read this file and the assigned ticket before changing code.

## Target Architecture

- `:app`, mobile/tablet/TV UI modules, `:playback:media3`, Android tracing, benchmarks, and baseline-profile modules remain Android-specific.
- Models, domain logic, repositories, persistence contracts, feature ViewModels/state, shared Compose components, provider-neutral playback contracts,
  and eligible provider implementations move to KMP.
- Shared modules use `commonMain` first. Android APIs belong in `androidMain`; browser APIs belong in `wasmJsMain`.
- Backend-agnostic boundaries remain mandatory. Shared/core/feature UI code must not import provider DTOs, SDKs, or client-specific models.
- Android remains the release platform throughout Phase 1. No `wasmJs` target is added before KMP-07 passes.
- Phase 2 adds Compose Multiplatform/Wasm for desktop evergreen browsers. The web UI uses TV-like composition with mouse and keyboard input rather
  than `androidx.tv.material3`.

## Locked Technical Decisions

- Kotlin `2.3.21`, AGP `9.1.1`, Gradle `9.3.1`, JVM target `11`.
- Compose Multiplatform `1.12.0`.
- Official `com.android.kotlin.multiplatform.library` plugin for KMP Android library targets.
- Koin `4.2.2`, classic constructor DSL (`singleOf`, `factoryOf`, `viewModelOf`); no Koin compiler plugin or service-locator calls in business code.
- `kotlinx-datetime` `0.8.0`, DataStore `1.2.1`, Ktor `3.5.0`, Coil `3.6.0` with Ktor networking.
- Shaka Player `5.2.3` for the first web DASH player.
- TMDB is the first web provider. ClientB must remain fully operational on Android but is out of scope for the first web release.
- Web configuration is deployment-supplied and browser-visible through `/config.json`; no real token is committed or packaged.
- Android performance results are recorded but are not a numerical release gate.

## Required Merge Order

```text
KMP-00
  -> KMP-01
  -> KMP-02
  -> KMP-03 ─┐
             ├-> KMP-05 -> KMP-06 -> KMP-07
  -> KMP-04 ─┘
  -> WEB-01 -> WEB-02 -> WEB-03 -> WEB-04
```

Only KMP-03 and KMP-04 are safe to implement concurrently. Their agents must not independently change root build logic or the version catalog after
KMP-02 has frozen those contracts.

WEB-03 may use several agents internally after WEB-02 freezes the shared web component APIs. Each agent must own disjoint feature modules; one
integration owner owns `:webApp`, navigation, and shared web design-system changes.

## Ticket Index

| Ticket                                           | Outcome                                                       | Depends on             | Parallel-safe                    |
|--------------------------------------------------|---------------------------------------------------------------|------------------------|----------------------------------|
| [KMP-00](KMP-00-android-baseline.md)             | Reproducible green Android baseline                           | Current work committed | No                               |
| [KMP-01](KMP-01-hilt-to-koin.md)                 | Android graph migrated from Hilt to Koin                      | KMP-00                 | No                               |
| [KMP-02](KMP-02-kmp-foundation-and-core.md)      | KMP build conventions and core modules                        | KMP-01                 | No                               |
| [KMP-03](KMP-03-feature-logic-group-a.md)        | Login/profiles/details/home logic migrated                    | KMP-02                 | With KMP-04                      |
| [KMP-04](KMP-04-feature-logic-group-b.md)        | Search/library/player/persistence/playback contracts migrated | KMP-02                 | With KMP-03                      |
| [KMP-05](KMP-05-provider-migration.md)           | TMDB/ClientB provider implementations migrated                | KMP-03, KMP-04         | No                               |
| [KMP-06](KMP-06-shared-compose-and-resources.md) | Shared presentation, UI, and resources migrated               | KMP-05                 | No                               |
| [KMP-07](KMP-07-android-parity-gate.md)          | Android parity and Phase 1 release gate                       | KMP-06                 | No                               |
| [WEB-01](WEB-01-wasm-runtime.md)                 | Wasm runtime and platform adapters                            | KMP-07                 | No                               |
| [WEB-02](WEB-02-login-and-profiles.md)           | Web login and profile flows                                   | WEB-01                 | No                               |
| [WEB-03](WEB-03-browse-surfaces.md)              | Web browse milestone                                          | WEB-02                 | Per feature, with one integrator |
| [WEB-04](WEB-04-playback-and-release.md)         | Web playback and production artifact                          | WEB-03                 | No                               |

## Execution Protocol

1. Start from the merged commit of every prerequisite ticket.
2. Confirm `git status --short` is empty. Stop rather than overwrite unrelated/user changes.
3. Work only in the modules listed by the ticket. If a prerequisite contract is missing, report it instead of redesigning another ticket's scope.
4. Preserve public behavior unless the ticket explicitly authorizes an API change.
5. Run the ticket's targeted verification before broader verification.
6. Do not hide failures with exclusions, blanket suppressions, disabled tests, or relaxed lint rules.
7. Finish with a clean working tree after the ticket commit and provide the handoff information below.

## Shared Definition of Done

- Production and test sources are in the source set specified by the ticket.
- `commonMain` contains no `android.*`, `java.*`, Hilt/Dagger, Android resource IDs, provider DTOs, or platform engine implementations.
- New collections exposed in UI state are immutable and lazy layouts retain stable keys/content types.
- ViewModels continue to expose one immutable `StateFlow<UiState>` and effects remain explicit.
- New/changed screen-level or reusable composables retain backend-free previews.
- Relevant common/unit/device tests pass.
- Both TMDB and ClientB Android graphs remain resolvable until the web phase explicitly narrows a web-only acceptance test to TMDB.
- No credentials, generated production config, build outputs, or local paths are committed.

## Required Agent Handoff

Every ticket response must include:

- Changed files grouped by module.
- Public API/type changes.
- Exact verification commands and their results.
- Known limitations or deferred work, mapped to a later ticket.
- Confirmation that no unrelated files were modified.
- Final `git status --short` output, or an explicit explanation of remaining user-owned changes.

