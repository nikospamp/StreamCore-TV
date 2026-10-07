# Maintenance register

Reviewed 2026-10-07. This is the current handover backlog, not a verification result. The target architecture is backend-agnostic:
public SDK contracts own domain behavior; application modules own presentation and platform interaction. Update or close an item with its evidence
when the work is completed. Setup and review procedures are in [the workflow](agent-workflow.md).

## Open decisions and verification

| Area | Current issue | Completion criteria |
| --- | --- | --- |
| ClientB distribution | Authentication persists a demonstration identity without validating the password; the catalogue is demo data and a release flavor exists. | Decide and document whether distribution is a demo or a real service; enforce that choice in packaging or implement/test a real provider. |
| Android backup | The manifest enables backup, while backup/extraction XML still contains template comments. | Define and verify cloud-backup/device-transfer treatment of the SDK's current namespaced session files. |
| Navigation payloads | Android Details and Player routes serialize `StreamCoreContent` snapshots. | Check saved-state size and process restoration; decide whether IDs plus a bounded transient preview are preferable, preserving source-row/artwork identity and return focus. |
| CI gate | Existing architecture/build gates, SDK tests and module tests lack a checked-in CI workflow that aggregates the required release coverage. | Define one documented CI entry point with tests, variant coverage and release validation; report failures and executed scope explicitly. |
| R8 rollout | `releaseR8` and `benchmarkR8` enable shrinking; ordinary `release` remains unminified. | Complete both-client smoke coverage, serialization/navigation/DI checks, mappings and packaging before changing the ordinary release policy. |
| UI acceptance | Manual web review and cross-platform rendering/input regression coverage remain incomplete. | Review changed surfaces against [DESIGN](../../DESIGN.md) and the [control checklist](shared-control-styling.md#regression-checklist); record browser/device, state and observed result. |
| Review-tool portability | Four-client acceptance has not been established on every development environment. | Complete [environment verification](agent-workflow.md#verify-a-development-environment) on the environment being handed over; report unavailable clients without claiming a complete pass. |
| Home performance | Publication/layout instrumentation exists, but the owning layout work and accepted optimization remain unresolved. | Attribute a specific descendant/publication, test one change, preserve profile isolation and carousel/transition behavior, and measure matched before/after artifacts using [the protocol](performance/mobile-navigation.md). |
| Profile/Player benefit | Baseline Profile packaging and orientation-before-pop code exist; historical baseline measurements do not establish their current benefit. | Support and validate `Baseline` identity in the analyzer before an integrated profile report, then run matched comparisons before claiming improvement; preserve Player progress, PiP, adaptive sizing and release-once behavior. |
| Dependency and packaging warnings | Coil/Compose compatibility, Kotlin/Binaryen repository registration, npm peer/version and webpack size warnings need a scoped reassessment. | Reproduce with current pinned versions and resolve or document the relevant compatibility/packaging outcome; do not infer a required upgrade from an old warning. |

Splitting the universal Android app into mobile/TV applications is a product/architecture decision, not an approved cleanup task.

## Browser verification timing

Queued test-infrastructure work: reduce repeated player auto-hide waits without changing production behavior or reducing coverage.

- Own `webApp/e2e/` test helpers and tests. Production playback, provider/API contracts, dependencies, credentials, browser binaries and build
  configuration are outside scope. A production diagnostic-fixture change requires a separately scoped change and a new artifact.
- Establish current scenario/project discovery before editing and preserve every scenario, assertion, input path, browser/viewport and cleanup check.
  Do not remove, combine away, skip, rename away or conditionally exclude tests to make the suite faster.
- Use a test-owned diagnostic clock or deterministic hidden-controls fixture. Advancing time must exercise the existing production state transition;
  it must not assign production state or bypass Player actions. Prove explicit readiness before pointer/keyboard control reveal.
- Retain exactly one release smoke that observes the real production auto-hide duration. Preserve leak/disposal checks and failure on unknown diagnostics;
  do not relax global timeouts or diagnostic classifiers to hide failures.
- Start with helper checks and the affected Chromium scenario. Then check the affected scenario in Chromium, Firefox and WebKit at one viewport;
  include another viewport when geometry changes. Freeze the shared-harness change and run the complete current matrix once before acceptance.
- Record before/after duration, discovery and executed counts, failures/skips/retries, artifact identity and cleanup outcome. Complete acceptance needs
  unchanged coverage and no new failures or skipped cases. A focused correction is reported beside the original run, never as an unexecuted full pass.
  Serialize build/server/browser work and follow [web testing](kmp/web-testing.md).

## Optional component maintenance

These consistency improvements are not prerequisites for deleting documentation or a mandate to split production code:

- Reconcile direct glyph rendering with `StreamCoreVectorIcon`, retaining named wrappers, sizing, tint and RTL. Replacing the Canvas profile fallback
  with the person vector changes artwork and needs visual review.
- Consider separate files for public navigation-item composables when those components are maintained. Move the fixture-only tablet destination
  test-tag helper with its test without removing production readiness hooks.
- Review mobile/TV scrim reuse through the current shared card-artwork contract; preserve alpha, geometry, input and transitions.
  Document internal-helper preview coverage and use the reusable-component preview convention for the TV content card.

Existing SDK/module tests and component files remain intentional source. No whole production component was identified for removal by this cleanup.
