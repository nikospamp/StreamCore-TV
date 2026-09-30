# StreamCore SDK

The SDK's target architecture is backend-agnostic. The current `0.1.0-alpha02` candidate supports Android and Kotlin/Wasm consumers through published Maven artifacts staged locally.

- [Quickstart](quickstart.md): a complete credential-free journey and the canonical coordinate-only sample.
- [Maintainer guide](maintainer-guide.md): module ownership, source folders, runtime flows, persistence, and where to make changes.
- [Integration reference](integration.md): services, lifecycle, profile/PIN activation, errors, storage and optional presentation.
- [Playback progress](playback-progress.md): player events versus direct progress updates, including save/remove/skip behavior.
- [Upgrade from alpha01](alpha02-migration.md): intentional namespace/API changes and preserved data.
- [Provider implementation](provider-template.md): backend SPI, capability rules and contract tests.
- [Verification](verification.md): checks actually executed and remaining acceptance limits.
- [Historical ownership baseline](ownership-and-baseline.md): pre-extraction behavior and legacy persistence attribution.

No remote registry, JavaScript/TypeScript package, Swift framework or stable 1.0 compatibility promise is implied by this candidate.
