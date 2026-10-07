# SDK consumer samples

These samples verify that an external application can consume the backend-agnostic StreamCore SDK through published Maven artifacts. They also provide working integration examples for another developer.

The [sdk-consumer project](sdk-consumer/settings.gradle.kts) is an independent Gradle build with its own settings and plugins. It resolves artifacts from a local staging repository, rather than using SDK project dependencies or composite-build substitution. This catches missing transitive dependencies, inaccessible APIs and missing packaged resources that an in-repository build can hide. The main application build does not include these sample modules.

## Modules

| Module | Purpose |
| --- | --- |
| [androidApp](sdk-consumer/androidApp) | Android consumer and host tests for public SDK operations, saved state and protected-profile/PIN journeys. |
| [headless](sdk-consumer/headless) | Kotlin/Wasm consumer running the SDK reference journeys on Node. |
| [uiAndroidApp](sdk-consumer/uiAndroidApp) | Android consumer checking that optional SDK UI resources are packaged and can be loaded from Maven artifacts. |
| [uiWasm](sdk-consumer/uiWasm) | Browser tests loading those optional UI resources from published dependencies. |

The default reference journeys use simulated ClientB behavior with in-memory storage and need no TMDB credentials. They do not establish persistence across restarts, live-backend behavior or production playback acceptance.

## Run publication and consumer verification

Run the following from the **repository root** on Windows. The wrapper uses `gradlew.bat` and requires PowerShell, Python, the project's JDK/Android SDK setup, Node 22+, Yarn and Chrome. Node and Yarn must already be installed; automatic downloads are disabled in the sample. See [repository requirements](../README.md#requirements); set `CHROME_BIN` if Chrome is not discovered automatically.

```powershell
pwsh -NoProfile -File tools/sdk/verify-publication.ps1 -RepositoryPath build/sdk-samples/maven
```

Choose a fresh `-RepositoryPath` for each candidate. The script stages all eight SDK artifacts locally, checks dependency boundaries and publication metadata, then builds/tests the independent Android, Node and browser consumers. It does not publish to a remote registry. Android UI instrumentation is compiled but only executed when a device is specified.

To reuse the **same already-staged candidate** and also execute its Android UI-resource test on a connected device:

```powershell
pwsh -NoProfile -File tools/sdk/verify-publication.ps1 `
    -RepositoryPath build/sdk-samples/maven `
    -SkipPublish `
    -AndroidSerial '<device-serial>'
```

`-SkipPublish` skips artifact publication, not consumer verification. Use `-NodeExecutable <path>` or `-AndroidSdkRoot <path>` when the default environment is unsuitable.

## Run the Android reference journey

After building `androidApp`, the separate runner installs its APK and executes the simulated ClientB journey on the selected device:

```powershell
node tools/sdk/run-android-consumer.mjs --serial '<device-serial>'
```

This is separate from the UI-resource instrumentation selected by `-AndroidSerial`. A live TMDB journey is an explicit opt-in with additional configuration; follow the [publication and verification guide](../docs/tracked/sdk/integration.md#maintainer-publication-and-verification) for that workflow.

## Maintain the samples

Keep SDK dependencies expressed as Maven coordinates. The `verifyCoordinateOnlyConsumption` task rejects SDK project dependencies and composite substitution. Keep the sample's SDK versions aligned with the candidate being staged; `-PsdkRepository=<absolute path>` selects its Maven repository when running the consumer build directly.

Keep sources, build configuration and the Yarn lockfile tracked. Generated `build`, `.gradle` and `.kotlin` directories remain local and ignored.

For API usage, start with the [SDK quickstart](../docs/tracked/sdk/quickstart.md). For executed checks and their limitations, use the [verification record](../docs/tracked/sdk/verification.md).
