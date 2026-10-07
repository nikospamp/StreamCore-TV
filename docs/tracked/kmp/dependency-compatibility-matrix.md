# Shared dependency compatibility

The backend-agnostic architecture requires compatible shared dependencies without leaking platform/provider implementations into consumers. The authoritative versions are in [the catalog](../../../gradle/libs.versions.toml); the compatibility gate's exact coordinate list is `lockedKmpCompatibilityCoordinateNames` in [the root build](../../../build.gradle.kts).

## Gate and scope

```powershell
.\gradlew.bat verifyKmpDependencyCompatibility
```

The task resolves the locked coordinates from the plain convention fixture twice: once with common metadata attributes and once with Android-KMP compile attributes. Both configurations are non-transitive. The current list covers DataStore, Coil networking, Compose resources/foundation/runtime/UI, Lifecycle Compose, Koin Compose, Ktor core/content negotiation/serialization, and Kotlinx Datetime.

Passing this gate proves those selected common/Android variants resolve. It does not prove every transitive dependency, the Wasm closure, browser execution, or published SDK consumer compatibility. Run the affected target compilation/tests and [SDK publication checks](../sdk/verification.md) when those surfaces change.

When adding or upgrading a shared dependency:

1. Update its catalog entry and the locked gate coordinates together.
2. Confirm usable common metadata and Android variants; keep Android-only libraries in `androidMain` or an Android-only module.
3. Compile/test each declared consuming target, including Wasm when applicable.
4. Recheck published SDK boundaries/consumers if the dependency enters an SDK artifact. Keep rendering dependencies out of headless artifacts and Koin out of all SDK publications.

## Current constraints

- Kotlin is pinned to 2.3.21, AGP to 9.1.1, and Compose Multiplatform to 1.12.0. Material 3 has its own catalog version; do not assume it follows the Compose version.
- Coil remains pinned to 3.4.0. Do not move to 3.6.0 while retaining Kotlin 2.3.21 without resolving the compatibility constraint and updating the consuming checks.
- DataStore common/Android dependencies use 1.2.1. Browser storage explicitly uses `datastore-core-okio:1.3.0-alpha08` for `WebLocalStorage` / `WebSessionStorage`; do not broaden that target-specific choice to Android incidentally.
- The browser application uses Shaka Player 5.2.3 from npm. `playback/web/src/wasmJsMain/resources/shaka-playback-adapter.mjs` imports `shaka-player/dist/shaka-player.compiled.js` through the typed module bridge. The release validator requires this local adapter and rejects remote/global-script fallbacks.
- Both headless SDK providers declare Wasm targets. The browser application's composition root currently selects TMDB; provider target support and app wiring are separate.

Use current dependency resolution and test output when evaluating a change. A metadata declaration alone does not establish runtime behavior on a device or browser.
