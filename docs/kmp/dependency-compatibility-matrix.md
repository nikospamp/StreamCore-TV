# KMP dependency compatibility matrix

Recorded for KMP-02 on 2026-09-01. `verifyKmpDependencyCompatibility` resolves 14 direct locked common metadata artifacts with transitive resolution
disabled so the smoke checks each published root component independently. Normal Android transitive resolution is proven by both provider app
compiles. No `wasmJs` target is configured; WEB-01 owns the first Wasm resolution and link.

## Locked versions and common/Android status

| Area | Locked version | Catalog/root proof | Android proof |
|---|---:|---|---|
| Kotlin / serialization | 2.3.21 / 1.11.0 | KMP + serialization plugins; `:core:data` common compilation | `:core:data:compileAndroidMain` |
| AGP Android-KMP | 9.1.1 | `com.android.kotlin.multiplatform.library` | single `android` variant consumed by every app build type |
| Compose Multiplatform | 1.12.0 | plugin plus runtime/foundation/UI/resources metadata smoke | current Android Compose graph plus provider app compiles; first Compose KMP module is KMP-06 |
| `kotlinx-datetime` | 0.8.0 | common metadata smoke and `ContentModel` | `:core:data:compileAndroidMain` and host tests |
| DataStore storage | 1.2.1 | `datastore-core-okio` metadata smoke | existing Android DataStore 1.2.1 graph compiles for both providers |
| Coil Ktor3 | 3.4.0 | `coil-network-ktor3` metadata smoke | locked at 3.4.0; 3.6.0 is intentionally prohibited by the Kotlin 2.4.10 metadata mismatch |
| Ktor | 3.5.0 | core/content-negotiation/serialization metadata smoke | existing provider clients compile for both providers |
| Koin Compose/ViewModel | 4.2.2 | both common metadata roots resolved | KMP-01 Android Koin graph and both provider app compiles |
| Lifecycle runtime/ViewModel Compose | 2.10.0 | JetBrains multiplatform metadata roots resolved | existing AndroidX 2.10.0 graph compiles for both providers |
| Shaka Player | 5.2.3 | version catalog only; npm availability recorded below | not applicable before WEB-04 |

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
