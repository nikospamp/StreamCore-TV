# Developer documentation

Start with the [repository README](../../README.md) for setup and running the application, then the [module graph](../../MODULE_DEPENDENCY_GRAPH.md) and [project conventions](../../AGENTS.md). The target architecture is backend-agnostic; SDK contracts own domain behavior, while application modules own rendering and platform interaction.

Read the guide for the work you are taking over:

| Task | Documentation |
|---|---|
| Understand product and UI decisions | [Product](../../PRODUCT.md), [design system](../../DESIGN.md), [shared control contracts](shared-control-styling.md) |
| Integrate or maintain the SDK | [SDK reading map](sdk/README.md), [quickstart](sdk/quickstart.md), [maintainer guide](sdk/maintainer-guide.md) |
| Add a provider or change public SDK behavior | [Integration reference](sdk/integration.md), [provider template](sdk/provider-template.md), [playback progress](sdk/playback-progress.md) |
| Verify SDK changes/publication | [Verification workflow and limits](sdk/verification.md) |
| Change shared modules or dependencies | [KMP conventions](kmp/build-conventions.md), [dependency compatibility](kmp/dependency-compatibility-matrix.md) |
| Test or deploy the browser app | [Browser testing](kmp/web-testing.md), [web release contract](kmp/web-release.md) |
| Launch clients or capture a visual review | [Local review workflow](agent-workflow.md) |
| Investigate Android performance | [Benchmark workflow](performance/mobile-navigation.md) |
| Choose follow-up work | [Maintenance register](maintenance.md) |

Source declarations are authoritative for module names, dependency versions, and executable tasks. Update the relevant operating guide when those contracts change. Keep unresolved decisions in the maintenance register, with a clear completion condition.

Optional SDK interactive pages are generated locally from tracked source inputs; instructions are in the SDK reading map. `docs/tracked/sdk/guide-content` and `docs/tracked/sdk/explorer-content` are authored tooling source. Their generated HTML outputs are ignored and are not checked in. Screenshots, historical audits/tickets, build logs, and machine-specific handoffs are local working material, not onboarding prerequisites. Record verification for the exact revision and environment tested; do not treat an older report as a pass for new code.
