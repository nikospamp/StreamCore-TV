# StreamCore SDK

The SDK's target architecture is backend-agnostic. The current `0.1.0-alpha02` candidate supports Android and Kotlin/Wasm consumers through published Maven artifacts staged locally.

- [Quickstart](quickstart.md): a complete credential-free journey and the canonical coordinate-only sample.
- [Maintainer guide](maintainer-guide.md): module ownership, source folders, runtime flows, persistence, and where to make changes.
- [Integration reference](integration.md): services, lifecycle, profile/PIN activation, errors, storage and optional presentation.
- [Playback progress](playback-progress.md): player events versus direct progress updates, including save/remove/skip behavior.
- [Upgrade from alpha01](alpha02-migration.md): intentional namespace/API changes and current persistence defaults.
- [Provider implementation](provider-template.md): backend SPI, capability rules and contract tests.
- [Verification](verification.md): checks actually executed and remaining acceptance limits.
- [Historical ownership baseline](ownership-and-baseline.md): pre-extraction behavior, with superseded storage assumptions identified.

No remote registry, JavaScript/TypeScript package, Swift framework or stable 1.0 compatibility promise is implied by this candidate.

## Interactive usage graph

Open [SDK Usage Explorer](streamcore-sdk-explorer.html) directly in a browser. The single-file page works offline and maps calls, argument origins, bindings and lifetimes across the backend-agnostic SDK. It opens on Android/TMDB construction; choose a function or value to highlight its origins and usages. The inspector links every relationship to the source snapshot. ClientB browser paths describe the SDK entry point, not current browser app wiring.

**Full tree** is the default view. Every indexed function and constructor exposes its complete signature inputs, types and defaults, including inputs with no previously authored mapping. Expand a parameter, local or property to follow **Where it comes from** and **Where it goes**. The tree also includes field reads, assignments, argument bindings, calls and returns. Branches load lazily; **Expand all reachable** visits the complete connected component once, retaining shared/cyclic links as references rather than stopping at a fixed depth.

Use **Tree of selection** or **Open full tree** to make an inspected symbol the tree root. Selecting a name otherwise preserves the expanded tree and updates the inspector. For example, open `createAndroidSdkStorage`, then its `config: StreamCoreConfiguration` input: its common-configuration origins and persistence/backend/storage-namespace uses are available in both directions. This behavior is driven by source indexing across the SDK, not a storage-specific map.

**Graph** remains an overview with distinct type shapes. Automatic tracing shows calls for functions and value flow for parameters/properties/variables; explicit filters are also available. It renders reviewed overview nodes and the selected neighborhood while Full tree exposes all indexed branches. Ordinary selection preserves positions and camera; **Center selection** and **Arrange path** move them explicitly. Pin a function and select a value to show one directed connection plus source-backed creator/consumer lists.

Regenerate after reviewing source changes:

```text
python tools/sdk/build-explorer.py
python tools/sdk/build-explorer.py --check
python -m unittest discover -s tools/sdk -p "test_*.py"
node tools/sdk/check-tree.mjs
node tools/sdk/check-explorer.mjs
```

The source index uses the project's cached Kotlin compiler PSI parser through a tooling-only Java exporter. Regeneration requires a JDK (`java`/`javac`) and the Kotlin compiler dependencies already present in the Gradle cache. It does not run Gradle, build the Android/browser apps, fetch dependencies, or execute Kotlin source. The complete data is gzip-compressed inside the HTML and decoded locally by the browser; viewing it requires a current browser with `DecompressionStream` support and makes no network requests.

Syntax indexing covers production Kotlin in the SDK and current application/feature/playback sources. Tests, coordinate-only samples and build tooling are not treated as current application callers. The resolver uses lexical scopes, imports, declared receiver types and argument shapes; it preserves reviewed provider/DI/deferred bindings from `explorer-content/construction.json` and `services.json`. This is conservative source analysis, not a Kotlin compiler binding context or runtime execution trace. Ambiguous and external calls are explicit boundaries. A terminal node with unresolved links is never claimed to prove the original source. Field projections retain their own origins; selecting `.common` does not absorb connection credentials from a sibling field.

`explorer-content/reviewed-sources.json` records normalized UTF-8 SHA-256 hashes for mapped files. Regeneration fails if a mapped file changes, an anchor becomes missing/ambiguous, a relationship crosses an incompatible provider/platform context, or a public service operation lacks a coverage entry. Review the changed source and all affected mappings before replacing that file's hash with the hash printed by the builder. Do not bulk-accept hashes to bypass a failed review. New syntax-derived declarations refresh automatically; curated bindings still require review. After changing graph/UI inputs, regenerate the HTML and run `--check`; unchanged inputs produce identical output. The existing guide can be regenerated independently with `python tools/sdk/build-guide.py`.

The source inventory includes safe repository source and all SDK production files; ignored runtime configuration, credentials and build output are excluded. References to configuration values show source symbols such as `BuildConfig.TMDB_READ_ACCESS_TOKEN`, never the local runtime values.
