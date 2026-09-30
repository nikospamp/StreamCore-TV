# Local setup verification — 2026-09-22

This records setup checks on the current Windows PC. `HANDOFF.md` remains the application handoff;
`kmp/ORCHESTRATION-HANDOFF.md` remains historical. The target architecture is backend-agnostic.

## Production preview

- No active project development server was found before the build.
- The sequential production build passed with `--warning-mode all`:
  `:app:verifyTmdbRuntimeConfig :webApp:generateWebDevelopmentConfig :webApp:wasmJsBrowserDistribution`
  with `-PrequireTmdbRuntimeConfig=true`.
- `npm --prefix webApp/e2e run validate:release` passed: one JavaScript bundle, two Wasm files,
  67 Compose assets, one placeholder configuration example, and zero real configuration files.
- The local production server was started at `http://localhost:8080/`, bound to `127.0.0.1`.
  The index and runtime configuration endpoint returned HTTP 200. Configuration uses `Cache-Control: no-store`.
- Runtime configuration stays in `webApp/build/generated/webDevelopmentConfig/config.json`, outside
  `webApp/build/dist/wasmJs/productionExecutable`. No configuration values were printed.
- Ignored local evidence: `build/setup-verification/production-build.log`,
  `gradle-after-fix.log`, `preview.pid`, and `preview.stdout.log` / `preview.stderr.log`.
  The preview is a local process; use the handoff commands to restart it after a reboot.

## Playwright

`npm ci` and `npx playwright install chromium firefox webkit` completed in `webApp/e2e`.
`npx playwright install --list` confirmed the binaries for the pinned Playwright 1.62.1 installation.

| Engine | Installation / launch verification |
| --- | --- |
| Chromium 151.0.7922.34, revision 1234 | Installed; headless launch and close passed. |
| Firefox 153.0, revision 1538 | Installed; headless launch is blocked by Windows SideBySide activation. |
| WebKit 26.5, revision 2336 | Installed; headless launch and close passed. |

Firefox failed identically after `npx playwright install --force firefox`. Windows Application event 33
reports an unresolved `mozglue` assembly. The downloaded `mozglue.dll` exists and contains the assembly
manifest as `RT_MANIFEST` resource 2, with no resource 1. This is inconsistent with the resource ID 1
required for this private-assembly lookup in Microsoft's
[assembly manifest documentation](https://learn.microsoft.com/en-us/windows/win32/sbscs/assembly-manifests).
The official binaries were left unmodified. A corrected compatible Playwright Firefox build is needed;
verify its launch before enabling Firefox tests. No browser version substitution, binary patch, system
security change, application journey, or live credential test was performed. Deferred suites remain deferred.

## Git and daemon JDK

The pre-existing daemon diff retained Java 21, added `toolchainVendor=JETBRAINS`, and regenerated download
URLs for every platform. Both JetBrains and Temurin 21 are installed. The user confirmed it was an IDE
rewrite; `gradle/gradle-daemon-jvm.properties` was restored to the checked-in vendor-neutral configuration.

`/build-logic/.kotlin/` was appended to `.git/info/exclude`, preserving its existing contents. This is a
local exclusion, not a shared ignore-rule change. No changes were committed.

## Gradle warning audit

The production build emitted one Gradle 10 deprecation: use of an auto-provisioned Temurin JDK 11 without
toolchain download repositories. The included `build-logic` build requests JDK 11 and had no resolver in
its own settings. Added `org.gradle.toolchains.foojay-resolver-convention` **1.0.0**, matching the root
build, to `build-logic/settings.gradle.kts`. Included builds need their own settings configuration.
See [Gradle toolchain repositories](https://docs.gradle.org/9.3.1/userguide/toolchains.html#sub:download_repositories).

After this fix and the daemon restoration, reran only `:app:verifyTmdbRuntimeConfig` with
`-PrequireTmdbRuntimeConfig=true --warning-mode all --no-configuration-cache`. Build-logic compilation and
the preflight passed with no deprecation warning. Production application inputs were unchanged; webpack
and Binaryen were not rerun. No unresolved third-party Gradle deprecation was observed in the production log.

Other warnings are separate from Gradle deprecations and remain for their owning follow-up work:

- Compose Multiplatform's compatibility check reports Coil 3.4.0 requesting Skiko 0.9.22.2 while Compose
  resolves 0.150.1. Review Coil/Compose compatibility in the already-deferred dependency migration.
- Kotlin Gradle plugin 2.3.21's Binaryen setup attempts to add a project repository under
  `PREFER_SETTINGS`; the matching settings-level distribution repository already exists and resolution passed.
  Reassess repository registration when migrating the Kotlin plugin; do not remove the required settings repository.
- Kotlin-managed npm tooling reports competing `ws` versions (selecting 8.18.3), an unmet `tslib@2` peer
  dependency under `webpack-dev-server > webpack-dev-middleware > memfs`, and intentionally ignored scripts.
- Webpack 5.101.3 reports a dynamic dependency expression and asset/entrypoint size advisories for the
  generated Wasm/JavaScript bundles. These require a separate packaging/performance review.
- Kotlin reports a redundant exhaustive `when` fallback in `WebHomeScreen.kt`; application UI was not changed.

Gradle, AGP, Kotlin, Compose, and Playwright versions were not upgraded.
