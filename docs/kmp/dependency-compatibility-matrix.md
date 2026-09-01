# KMP dependency compatibility matrix

Recorded for KMP-02 on 2026-09-01. `verifyKmpDependencyCompatibility` resolves every locked Maven coordinate twice from the plain convention fixture:
once with the official common metadata attributes and once with the official AGP 9.1.1 Android-KMP compile attributes. Both configurations are
non-transitive so each selected component/variant is attributable to one matrix row. Fourteen common selections produce 14 files; fourteen Android
selections produce 12 files because published redirects may be fileless. Component and variant counts—not file count—are the gate.

No `wasmJs` target is configured; WEB-01 owns the first Wasm resolution and link.

## Locked versions and common/Android status

| Locked parent coordinate | Common selected variant | Android selected variant |
|---|---|---|
| `androidx.datastore:datastore-core-okio:1.2.1` | `metadataApiElements` | `jvmApiElements-published` |
| `io.coil-kt.coil3:coil-network-ktor3:3.4.0` | `metadataApiElements` | `androidApiElements-published` |
| `org.jetbrains.compose.components:components-resources:1.12.0` | `metadataApiElements` | `releaseApiElements-published` |
| `org.jetbrains.compose.foundation:foundation:1.12.0` | `metadataApiElements` | `androidApiElements-published` |
| `org.jetbrains.compose.runtime:runtime:1.12.0` | `metadataApiElements` | `androidApiElements-published` |
| `org.jetbrains.compose.ui:ui:1.12.0` | `metadataApiElements` | `androidApiElements-published` |
| `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.10.0` | `metadataApiElements` | `releaseApiElements-published` |
| `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0` | `metadataApiElements` | `releaseApiElements-published` |
| `io.insert-koin:koin-compose:4.2.2` | `metadataApiElements` | `releaseApiElements-published` |
| `io.insert-koin:koin-compose-viewmodel:4.2.2` | `metadataApiElements` | `releaseApiElements-published` |
| `io.ktor:ktor-client-core:3.5.0` | `metadataApiElements` | `jvmApiElements-published` |
| `io.ktor:ktor-client-content-negotiation:3.5.0` | `metadataApiElements` | `jvmApiElements-published` |
| `io.ktor:ktor-serialization-kotlinx-json:3.5.0` | `metadataApiElements` | `jvmApiElements-published` |
| `org.jetbrains.kotlinx:kotlinx-datetime:0.8.0` | `metadataApiElements` | `jvmApiElements-published` |

Compose Multiplatform is locked to 1.12.0, Kotlin/serialization to 2.3.21/1.11.0, and AGP Android-KMP to 9.1.1. Coil remains pinned at 3.4.0;
3.6.0 is prohibited while the project remains on Kotlin 2.3.21. Shaka Player 5.2.3 is npm-only and recorded separately below.

## Published Wasm variants from Gradle module metadata

The official parent `.module` files publish the following `wasmJsApiElements-published`, `wasmJsRuntimeElements-published`, and
`wasmJsSourcesElements-published` redirects. Koin also publishes `wasmJsResourcesElements-published`.

| Parent coordinate inspected | Exact Wasm coordinate (`available-at`) |
|---|---|
| `androidx.datastore:datastore-core-okio:1.2.1` | `androidx.datastore:datastore-core-okio-wasm-js:1.2.1` |
| `io.ktor:ktor-client-core:3.5.0` | `io.ktor:ktor-client-core-wasm-js:3.5.0` |
| `io.ktor:ktor-client-js:3.5.0` (browser Fetch engine) | `io.ktor:ktor-client-js-wasm-js:3.5.0` |
| `io.coil-kt.coil3:coil-network-ktor3:3.4.0` | `io.coil-kt.coil3:coil-network-ktor3-wasm-js:3.4.0` |
| `io.insert-koin:koin-compose:4.2.2` | `io.insert-koin:koin-compose-wasm-js:4.2.2` |
| `io.insert-koin:koin-compose-viewmodel:4.2.2` | `io.insert-koin:koin-compose-viewmodel-wasm-js:4.2.2` |
| `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.10.0` | `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose-wasm-js:2.10.0` |
| `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0` | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose-wasm-js:2.10.0` |

## Shaka npm/ESM status

The npm registry publishes `shaka-player@5.2.3` with integrity
`sha512-qIstmSOlCqNsRicCOA9FmwIwPxjsZ9qjfqDv2C/N57ECmw5grLoaYP4HU3PMf28t2pgY3pyQDAl64CmoWZIeBg==` and entry point
`dist/shaka-player.compiled.js`. Its package metadata has no `module`, `type`, or `exports` field, so it does not advertise a native ESM entry.
WEB-04 must use the compiled global-compatible distribution (or an explicitly validated wrapper) rather than assuming direct ESM import support.
