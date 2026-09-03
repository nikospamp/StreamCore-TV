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
- `kotlinx-datetime` `0.8.0`, DataStore `1.2.1` for common/Android, Ktor `3.5.0`, Coil `3.4.0` with `coil-network-ktor3`.
  WEB-01 has one scoped Wasm exception: `datastore-core-okio:1.3.0-alpha08` in `wasmJsMain`, because official
  `WebLocalStorage` / `WebSessionStorage` are not published by `1.2.1`; common and Android resolution stays on `1.2.1`.
- Shaka Player `5.2.3` for the first web DASH player.
- TMDB is the first web provider. ClientB must remain fully operational on Android but is out of scope for the first web release.
- Web configuration is deployment-supplied and browser-visible through `/config.json`; no real token is committed or packaged.
- Android performance results are recorded but are not a numerical release gate.

## Required Merge Order

```text
KMP-00A(done) -> KMP-00B -> [KMP-00C || KMP-00D || KMP-00E || KMP-00F] -> KMP-00G -> KMP-00H -> KMP-01 -> KMP-02 -> [KMP-03 || KMP-04] -> KMP-05 -> KMP-06 -> KMP-07 -> WEB-01 -> WEB-02
    -> WEB-03A -> [WEB-03B || WEB-03C || WEB-03D || WEB-03E] -> WEB-03F
    -> WEB-04A(contract freeze) -> [WEB-04A(engine) || WEB-04B || WEB-04C] -> WEB-04D
```

Only KMP-03 and KMP-04 are safe to implement concurrently. Their agents must not independently change root build logic or the version catalog after
KMP-02 has frozen those contracts.

**Current gate (2026-09-04): WEB-04D Candidate 3 pending freeze.** WEB-03 remains accepted at
`f9e13558b3fc1c68db89ba9715932e8db80813ae`. Reviewed WEB-04 inputs are A
`631f4edbd4c7007c8cef3c19d60f2071168a0bd6`, B `718980793ef4eb4e89ad0c355b0ea2678e51adf6`, and C
`3e3dc3618af19b9a0276f20d6d4674607f87d72b`; WEB-04D merged them in mandatory A→B→C order. Candidate 1
`ace71461c4914716509e1488a311110d7a20844d` passed production/Binaryen distribution and artifact validation but failed its complete matrix with
144 passed / 42 failed / 0 skipped, so it is not eligible for acceptance. Bounded corrections now have green focused evidence (engine 19/19,
player UI 14/14, WebApp 65/65, fullscreen behavior 6/6, targeted Details-return and browser-diagnostic reruns) and all capped P0/P1 reviews are
closed. These focused results do not replace Candidate 1's failed matrix. Candidate 2
`d9a05ce7e4594f42efccc8c02d2c9b0ec6003f60` passed production/Binaryen distribution and artifact validation, then failed its complete 21.7m matrix
with 183 passed / 3 failed / 0 skipped. All behavioral assertions passed; the failures were bounded terminal WebKit diagnostics: two exact
hard-reload coroutine teardown occurrences across viewports, one same-epoch player-exit blob-access plus I/O pair, and one diagnostic-seed
`web-probe.png` abort. Candidate 2 is not eligible for acceptance. Its correction is test-only and does not replace the failed matrix. Candidate 3
is not yet frozen; its production/Binaryen artifact, artifact validator, complete 186-test six-project matrix, Android/root gate, inspected visuals,
manual current Safari/macOS, and final authorized live journey remain required before WEB-04 acceptance. The target architecture remains
backend-agnostic.

WEB-03 and WEB-04 are milestone indexes, not executable mega-tickets. Their executable child tickets reserve disjoint paths and use at most three
concurrent feature owners while the root orchestrator remains active.

## Ticket Index

| Ticket                                           | Outcome                                                       | Depends on             | Parallel-safe                    |
|--------------------------------------------------|---------------------------------------------------------------|------------------------|----------------------------------|
| [KMP-00](KMP-00-android-baseline.md)             | Accepted green Android baseline after B-H                     | Current work committed | No                               |
| [KMP-00B](KMP-00B-auth-session-lifecycle.md)     | Logout and restored-session lifecycle on all platforms        | KMP-00A remediation    | No                               |
| [KMP-00C](KMP-00C-tablet-top-level-navigation.md) | Tablet Home/Search/Library navigation and surfaces           | KMP-00B                | Yes, feature-local ownership     |
| [KMP-00D](KMP-00D-tablet-details-player-parity.md) | Tablet trailer/player parity                                | KMP-00B                | Yes, feature-local ownership     |
| [KMP-00E](KMP-00E-tv-profile-deletion.md)        | TV profile deletion and confirmation focus                    | KMP-00B                | Yes, feature-local ownership     |
| [KMP-00F](KMP-00F-tv-details-player-parity.md)   | TV trailer and D-pad player parity                            | KMP-00B                | Yes, feature-local ownership     |
| [KMP-00G](KMP-00G-tv-return-focus-restoration.md) | Exact TV content return-focus restoration                   | KMP-00C through F      | No                               |
| [KMP-00H](KMP-00H-android-baseline-reacceptance.md) | Full rerun, Accepted report, integration branch            | KMP-00B through G      | One integration owner            |
| [KMP-01](KMP-01-hilt-to-koin.md)                 | Android graph migrated from Hilt to Koin                      | KMP-00                 | No                               |
| [KMP-02](KMP-02-kmp-foundation-and-core.md)      | KMP build conventions and core modules                        | KMP-01                 | No                               |
| [KMP-03](KMP-03-feature-logic-group-a.md)        | Login/profiles/details/home logic migrated                    | KMP-02                 | With KMP-04                      |
| [KMP-04](KMP-04-feature-logic-group-b.md)        | Search/library/player/persistence/playback contracts migrated | KMP-02                 | With KMP-03                      |
| [KMP-05](KMP-05-provider-migration.md)           | TMDB/ClientB provider implementations migrated                | KMP-03, KMP-04         | No                               |
| [KMP-06](KMP-06-shared-compose-and-resources.md) | Shared presentation, UI, and resources migrated               | KMP-05                 | No                               |
| [KMP-07](KMP-07-android-parity-gate.md)          | Accepted Android parity and Phase 1 release gate              | KMP-06                 | No                               |
| [WEB-01](WEB-01-wasm-runtime.md)                 | Wasm runtime and platform adapters                            | KMP-07                 | No                               |
| [WEB-02](WEB-02-login-and-profiles.md)           | Web login and profile flows                                   | WEB-01                 | No                               |
| [WEB-03](WEB-03-browse-surfaces.md)              | Non-executable browse milestone index                         | WEB-02                 | See WEB-03A through WEB-03F      |
| [WEB-03A](WEB-03A-contracts-and-shell.md)         | Browse contracts, module shells, navigation and chrome        | Accepted WEB-02 merge  | Integration owner only           |
| [WEB-03B](WEB-03B-home.md)                       | Web Home surface                                              | WEB-03A freeze         | Feature-local                    |
| [WEB-03C](WEB-03C-search.md)                     | Web Search surface and native input                           | WEB-03A freeze         | Feature-local                    |
| [WEB-03D](WEB-03D-library.md)                    | Web Library surface                                           | WEB-03A freeze         | Feature-local                    |
| [WEB-03E](WEB-03E-details.md)                    | Web Details surface                                           | WEB-03A freeze         | Feature-local                    |
| [WEB-03F](WEB-03F-integration-gate.md)           | Browse integration, candidate and final live gate             | WEB-03B through E      | Integration owner only           |
| [WEB-04](WEB-04-playback-and-release.md)         | Non-executable playback/release milestone index               | WEB-03F                | See WEB-04A through WEB-04D      |
| [WEB-04A](WEB-04A-contract-and-engine.md)         | Playback contract freeze and browser engine                   | WEB-03F                | Owns playback contract/engine    |
| [WEB-04B](WEB-04B-player-ui.md)                  | Web player UI against the frozen fake session                 | WEB-04A freeze         | Feature-local                    |
| [WEB-04C](WEB-04C-release-and-test.md)            | Release fixtures, tests and deployment contract               | WEB-04A freeze         | Test/docs only                   |
| [WEB-04D](WEB-04D-final-integration.md)           | Playback integration and release acceptance                   | WEB-04A through C      | Integration owner only           |

## Execution Protocol

1. KMP-00B executes first; KMP-00C through KMP-00F then execute in parallel with shared files reserved for the integration owner. KMP-00G and
   KMP-00H execute serially after integration. KMP-00H establishes the accepted green baseline and creates the
   long-lived `codex/kmp-migration` integration branch from its documentation-only acceptance commit.
2. Create one `codex/<ticket-id>-<slug>` branch/worktree per ticket from the latest accepted integration commit. Never implement migration tickets directly on the default branch.
3. Start from the merged commit of every prerequisite ticket.
4. Confirm `git status --short` is empty. Stop rather than overwrite unrelated/user changes.
5. Work only in the modules listed by the ticket. If a prerequisite contract is missing, report it instead of redesigning another ticket's scope.
6. Preserve public behavior unless the ticket explicitly authorizes an API change.
7. Run the ticket's targeted verification before broader verification.
8. Do not hide failures with exclusions, blanket suppressions, disabled tests, relaxed lint rules, or test tasks that execute zero tests.
9. Merge into `codex/kmp-migration` only after the ticket's acceptance gate passes. The last accepted integration commit is the rollback point; a blocking ticket remains unmerged and is revised rather than partially landing.
10. If KMP-07 finds a blocking regression, stop Phase 2. Keep the last green integration commit, document the regression, and fix the owning Phase 1 ticket before rerunning KMP-07.
11. Finish with a clean working tree after the ticket commit and provide the handoff information below.

## Web Orchestration Contract

- The root orchestrator owns dependency decisions, immutable base SHAs, branch/worktree creation, path reservations, integration, merge order,
  evidence classification, build queue, and status updates. It keeps one of four agent slots active; no more than three feature owners run at once.
- Every dispatch states one immutable 40-character base SHA, owned paths, forbidden paths, expected API, focused verification, and evidence required.
  A symbolic or unavailable base blocks implementation; agents never branch from an unreviewed candidate.
- Feature owners change only reserved paths and do not resolve cross-module conflicts. One integration owner exclusively controls `:webApp`, web
  navigation, root/settings/build files, and shared `:core:ui-web` contracts unless a ticket explicitly reserves a narrower test/docs subtree.
- Reviewers are read-only and return findings with file/line evidence. A reviewer does not edit, merge, run credentials, or turn a focused rerun into
  a complete-pass claim. Production code freezes before architecture, backend-boundary, lifecycle/leak, accessibility, and security review.
- The root consumes completion events promptly, verifies diffs and focused evidence, and merges only reviewed commits in the ticket's declared wave.
  WEB-03 merges B→C→D→E. WEB-04 establishes a contract-freeze checkpoint, then final integration merges A→B→C.
- Gradle, webpack/dev-server, Playwright-server, browser test, and Binaryen/distribution jobs are serialized through one build queue against shared
  caches. Parallelism is for reasoning, read-only review, and disjoint file edits—not concurrent build processes.
- Core, domain, playback API, and feature UI remain backend-agnostic. Provider DTOs, SDKs, responses, client-specific models, and direct TMDB calls
  stay outside these boundaries.

## Web Fast-Feedback Tiers

1. **Tier 0 — setup (target ≤5 minutes):** verify base and status, reserve paths, define the smallest acceptance journey, and name the first
   provider/platform boundary.
2. **Tier 1 — iteration (target ≤15 minutes):** run development Wasm, focused unit/Compose tests, and at most one Chromium 1280×720 journey. After
   two failed hypotheses or 15 minutes, stop and assign a specialist. Do not run Binaryen, the full browser matrix, or root `check`.
3. **Early live proof:** only when a smallest provider/network/persistence vertical slice cannot be proved by existing accepted evidence, run one
   redacted smoke and return to mocks. WEB-03/04 do not use live credentials during feature iteration.
4. **Tier 2 — review:** freeze production code and complete read-only architecture, boundary, lifecycle/leak, accessibility, and security reviews.
5. **Tier 3 — candidate:** the integration owner runs one production/Binaryen distribution, one complete three-browser/two-viewport matrix, and one
   combined Android/root regression gate. Any production change creates a new candidate and returns to focused verification.
6. **Test-only correction:** rerun only affected cases and report the original matrix plus focused rerun separately.
7. **Final live proof:** only after all non-live gates are ready, run the ticket's single redacted provider journey and mandatory cleanup. Never read,
   print, copy, commit, or screenshot credentials; use the ignored wrapper defined by WEB-02 and obtain action-time approval when Codex would transmit.

## Web Evidence Taxonomy

Every result is one of `planned`, `observed-pass`, `observed-fail`, `blocked`, or `not-run`. Record the exact command, commit, build kind
(`development` or `production/Binaryen`), browser and viewport, executed/passed/failed/skipped counts, focused versus complete scope, and artifact
path where applicable.

- **Focused evidence:** module compile/test or one targeted browser scenario. It proves only the named scope.
- **Review evidence:** read-only findings tied to exact files/lines and the reviewed commit. No findings is not a build/test result.
- **Candidate evidence:** serialized production distribution, complete browser/viewport matrix, and combined Android/root gate on one frozen commit.
- **Visual evidence:** named screenshots plus recorded human inspection of hierarchy, clipping, focus, and long text. File existence is not visual
  approval, and automated screenshot capture is not design acceptance.
- **Live evidence:** redacted provider journey, wrapper exit/result, and cleanup outcome. It never reveals secret, session, account, or credential data.
- **Correction evidence:** original failure plus focused rerun after a test-only change. It must not be reported as an unexecuted complete pass.

Use checkpoint updates in this exact form: **Observed:** verified fact. **Hypothesis:** current explanation. **Next falsifier:** cheapest decisive test.
**Stop/escalate:** explicit condition.

The repository currently has no `.github/workflows` CI. Until CI is explicitly introduced, every gate is local and its command output/test counts must be retained in the ticket handoff. KMP-00 records the physical/emulated device owners used for release verification.

## Shared Definition of Done

- Production and test sources are in the source set specified by the ticket.
- `commonMain` contains no `android.*`, `java.*`, `androidx.annotation.*`, `androidx.core.*`, Hilt/Dagger, Android resource IDs, provider DTOs, or platform engine implementations.
- New collections exposed in UI state are immutable and lazy layouts retain stable keys/content types.
- ViewModels continue to expose one immutable `StateFlow<UiState>` and effects remain explicit.
- New/changed screen-level or reusable composables retain backend-free previews.
- Every KMP module containing Kotlin test files in `commonTest` has an enabled Android host-test compilation until another executable target is added.
  `testAndroidHostTest` must execute those tests; a green zero-test task is a failure when tests are expected.
- Compile-only modules with no behavioral tests must be named explicitly in their ticket/baseline, must not create an empty `commonTest`, and must not
  receive filler tests solely to satisfy a task count. `:core:domain` is the initial compile-only exemption.
- Relevant common/host/device/browser tests pass, and each handoff reports executed test counts per module against the KMP-00 baseline.
- Root `check` passes and `verifyDesignTokens` reports production sources from `main`, `commonMain`, `androidMain`, and, after WEB-01, `wasmJsMain` rather than silently checking zero migrated files.
- Both TMDB and ClientB Android graphs remain resolvable until the web phase explicitly narrows a web-only acceptance test to TMDB.
- No credentials, generated production config, build outputs, or local paths are committed.

## Required Agent Handoff

Every ticket response must include:

- Changed files grouped by module.
- Public API/type changes.
- Exact verification commands and their results.
- Executed test counts per module and comparison with the baseline; explicitly report any zero-test task.
- Known limitations or deferred work, mapped to a later ticket.
- Confirmation that no unrelated files were modified.
- Final `git status --short` output, or an explicit explanation of remaining user-owned changes.
