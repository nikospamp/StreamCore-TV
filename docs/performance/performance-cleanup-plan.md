# Performance work cleanup plan

## Goal

Reduce the current performance work from a broad diagnostic changeset into a
small, reviewable set of production improvements and durable benchmark tools.

The final codebase should retain:

- shipped Baseline and Startup Profiles;
- the Player orientation-before-pop fix;
- an explicit R8 rollout decision;
- a maintainable Macrobenchmark harness;
- only the tracing required by an active, named investigation.

Do not reset, restore, clean, or broadly restage the working tree. Existing
staging is intentional and must be split carefully.

## 1. Keep as production work

### Baseline Profile

- Keep `:baselineprofile`.
- Keep `app/src/main/generated/baselineProfiles/baseline-prof.txt`.
- Keep `app/src/main/generated/baselineProfiles/startup-prof.txt`.
- Keep `ProfileInstaller` and the Baseline Profile consumer plugin.
- Keep `ReportDrawnWhen` on the meaningful Profiles state.
- Keep the isolated `.benchmark` profile-generation target.
- Document that the profile was generated from the TMDB client journey; Client
  B still needs client-specific coverage if its hot paths differ materially.

### Player exit sequencing

- Keep `MobilePlayerWindowSession` and `MobilePlayerExitCoordinator`.
- Keep orientation restoration before `popBackStack()`.
- Keep explicit-Back PiP auto-enter suppression.
- Keep the configuration settlement gate and bounded timeout.
- Keep disposal fallback for orientation, system bars, and
  `FLAG_KEEP_SCREEN_ON`.
- Keep Player coordinator and ViewModel Back/settings tests.
- Do not change progress persistence ordering or Media3 application-looper
  release behavior.

### R8 candidate

- Keep `releaseR8` isolated from the current production `release` until release
  smoke coverage is complete.
- Keep mapping outputs as build artifacts, not source files.
- Decide separately whether to enable R8 directly on `release`.

## 2. Keep as durable benchmark infrastructure

- Keep the `:benchmark` module.
- Keep `:benchmark:ui-driver` and the shared deterministic selectors.
- Keep `NavigationBenchmark`, preflight, fixture, and target identity handling.
- Keep `None`, warm-up `Partial`, `Baseline`, and diagnostic `Ignore` as
  distinct compilation modes.
- Keep strict artifact/device/dataset/hash validation.
- Keep analyzer unit tests.
- Keep benchmark readiness semantics required to avoid timing arbitrary sleeps.
- Keep the rule that frozen and partial campaigns are never pooled.

Before retaining every host script, verify that it belongs to a repeatable
workflow. Remove one-off scripts that have no test, documented input contract,
or expected future caller.

## 3. Temporary attribution to remove after the next decision

Home attribution is diagnostic, not a production optimization.

Temporarily retain:

- `SC.Home.publish.*` markers;
- Home publication counters;
- `SC.Home.measure.*` / `SC.Home.place.*` boundaries;
- pager, indicator, row-type, shared-bound, and window-width counters.

Use only the already-captured local traces to identify one owner. Do not start a
new campaign without approval.

After selecting and implementing one Home optimization:

1. keep only the trace needed to verify that optimization;
2. run its focused host tests and approved device check;
3. remove all other Home attribution;
4. confirm production variants no longer create tracing SideEffects, collectors,
   strings, or modifier elements.

## 4. Remove low-value tracing

Remove trace calls that have already ruled out their owner or do not support an
active decision:

- `SC.TMDB.map.rows`;
- `SC.TMDB.map.details`;
- progress and library decode spans;
- Flow collector-count instrumentation;
- generic Home, Details, and Search load spans after readiness no longer needs
  them;
- image source/error tracing;
- platform width/selection tracing after Player structural validation;
- Search IME tracing unless Search investigation is explicitly resumed.

Keep a Player span only while it verifies a concrete invariant:

- persistence precedes navigation;
- orientation restore completes before Details composition;
- release occurs once on the application/Main looper;
- frame-cache bytes are zero after release.

Once these are covered by stable tests or an accepted trace, remove the
corresponding temporary span.

## 5. Reduce documentation

Keep durable documents:

- the controlled Samsung final report;
- the repeatable benchmark protocol;
- the Player optimization design and final verification result;
- the Home optimization design until the selected Home change is complete;
- this cleanup plan until cleanup is finished.

Archive outside the main source history or remove:

- chat handoff documents;
- progress journals;
- superseded partial findings;
- temporary device checkpoints;
- duplicated narrative reports.

Partial evidence must remain explicitly marked diagnostic and statistically
invalid. Do not convert it into a final result table.

## 6. Target commit boundaries

### Commit 1 — Baseline Profile

- producer/consumer Gradle configuration;
- generated profiles;
- ProfileInstaller;
- shared generation journey;
- `BaselineProfileMode.Require` support;
- Profiles `ReportDrawnWhen`.

### Commit 2 — Player optimization

- orientation-before-pop source change;
- PiP handling;
- coordinator/window-session tests;
- settings and duplicate-Back coverage.

### Commit 3 — R8 candidate

- `releaseR8` build type only;
- keep-rule fixes, if required;
- rollout/smoke documentation.

### Commit 4 — Benchmark infrastructure

- Macrobenchmark driver;
- host runner/analyzer tools;
- analyzer tests;
- durable readiness semantics.

### Commit 5 — Home diagnostic attribution

- temporary and independently removable;
- must not contain a Home behavior change;
- remove or replace with the eventual single Home optimization commit.

## 7. Verification after cleanup

Run host-side verification:

```powershell
.\gradlew.bat :feature:player:ui-common:testDebugUnitTest `
  :feature:player:ui-mobile:testDebugUnitTest `
  :feature:home:ui-common:testDebugUnitTest `
  :app:compileTmdbDebugKotlin `
  :app:compileClientBDebugKotlin `
  :baselineprofile:compileTmdbNonMinifiedProfileSources `
  :benchmark:compileTmdbBenchmarkSources --console=plain

.\gradlew.bat :app:assembleTmdbBenchmarkR8 `
  :app:assembleTmdbReleaseR8 `
  :app:assembleClientBReleaseR8 --console=plain

python -m unittest discover -s tools/performance -p 'test_*.py' -v
git diff --check
```

Verify artifact contents:

- `assets/dexopt/baseline.prof` exists;
- `assets/dexopt/baseline.profm` exists;
- TMDB and Client B R8 mappings exist;
- production release behavior is unchanged unless the R8 rollout commit is
  explicitly accepted.

Do not run another device campaign as part of cleanup.

## Definition of done

- Low-value tracing is removed from feature, core, client, and data code.
- Home contains no unapproved production optimization.
- Remaining benchmark hooks have a documented consumer.
- Baseline Profile, Player, R8, benchmark infrastructure, and Home attribution
  are split into reviewable commits.
- The working tree no longer mixes final product code, temporary diagnostics,
  intermediate reports, and superseded campaign tooling.
- All host verification passes.

## Implementation status — 2026-08-31

Completed:

- removed TMDB mapper probes;
- removed progress/library decode and Flow collector probes;
- removed image source/error instrumentation;
- removed platform selection/width probes;
- removed Search IME/discovery and generic Details/Home load probes;
- removed temporary Player persistence/orientation/release/cache probes after
  their ordering invariants were confirmed by tests and an accepted trace;
- reduced `:core:tracing` to readiness semantics, synchronous Home attribution,
  layout attribution, and counters;
- reduced tracing dependencies to modules with an active consumer;
- removed superseded checkpoints, partial reports, progress journals, and the
  chat handoff document;
- repaired durable documentation links;
- passed Player/Home unit tests, both client debug compilation, Baseline Profile
  and benchmark compilation, all three R8 builds, analyzer tests, profile
  packaging checks, and `git diff --check`.

Intentionally remaining:

- Home publication/layout attribution, until one Home owner is selected from
  already-captured traces;
- benchmark readiness semantics used by the durable Macrobenchmark driver;
- commit/index separation, which must be done explicitly without broad
  restaging or disturbing existing staging.
