# Development tools

Shared scripts for environment checks, visual review, performance measurement and SDK verification. Keep these tools tracked so another developer can repeat the project's workflows. They support the backend-agnostic app and SDK without becoming part of the shipped application.

Run all commands below from the **repository root**.

## Folder map

| Folder | Responsibility |
| --- | --- |
| [dev](dev) | Environment preflight and shared PowerShell helpers for locating Java, Android SDK, devices and review configuration. |
| [review](review) | Android display capture and tests for the review helpers. Browser capture and board composition live alongside the [web review tools](../webApp/e2e). |
| [performance](performance) | Benchmark runners, navigation/trace analyzers and campaign evidence extraction. |
| [sdk](sdk) | SDK source/publication checks, independent consumer execution, and interactive documentation generators and tests. |

## Prerequisites

Use the [repository setup](../README.md#requirements) for Java, Android SDK, Node and Yarn. PowerShell helpers require PowerShell 7; the publication wrapper uses Windows `gradlew.bat`. Python is required for source checks, analyzers and documentation generation.

SDK documentation indexing also needs Git, a JDK with `java`/`javac`, and the project's Kotlin compiler dependencies in the Gradle cache. Browser checks use Node, the pinned Playwright installation under `webApp/e2e`, and installed Chrome. Device workflows need an explicitly selected connected device. See the linked guides for workflow-specific setup.

## Environment and visual review

Inspect Android tooling and connected devices:

```powershell
pwsh -NoProfile -File tools/dev/preflight.ps1 -Scope Android -CheckDevices
```

Preflight reports readiness without building or installing the app. Omit `-CheckDevices` for tooling-only inspection; add `-StartAdb` explicitly when an ADB server must be started.

Capture the currently displayed Android screen:

```powershell
pwsh -NoProfile -File tools/review/capture-android.ps1 -Serial '<serial>' -Role mobile -Output build/review/mobile.png
```

Navigate to the desired state first and choose a new output path for every capture. Use `tablet` or `tv` as the role when appropriate. Follow the [review workflow](../docs/tracked/agent-workflow.md) for installation, verified builds, browser capture and comparison boards.

## SDK verification and documentation

Check SDK source boundaries, or stage a candidate locally and verify its independent consumers:

```powershell
python tools/sdk/verify_sdk.py
pwsh -NoProfile -File tools/sdk/verify-publication.ps1 -RepositoryPath build/sdk-tools-check/maven
```

Use a fresh repository path for each candidate. The publication script checks metadata/dependency boundaries and builds/tests the [samples](../samples/README.md); it does not publish to a remote registry. Android UI-resource tests execute on-device only when `-AndroidSerial` is supplied. See the [SDK publication guide](../docs/tracked/sdk/integration.md#maintainer-publication-and-verification) for reuse, device and live-backend options.

Generate both interactive SDK pages before opening them or running their browser checks:

```powershell
python tools/sdk/build-guide.py
python tools/sdk/build-explorer.py
python tools/sdk/build-explorer.py --check
```

Inputs are maintained under `docs/tracked/sdk/guide-content` and `docs/tracked/sdk/explorer-content`. HTML goes to `build/sdk-docs/`. The [SDK documentation guide](../docs/tracked/sdk/README.md) lists the browser checks and explains source indexing; `--check` verifies an existing explorer output rather than creating it.

## Performance measurement

Follow the [measurement protocol](../docs/tracked/performance/mobile-navigation.md) before using `run-navigation.ps1` or `run-campaign.ps1`. These workflows install benchmark APKs and run device instrumentation; the navigation runner's `Preflight` phase is not the inspection-only development preflight above. Keep the selected device, content, build identity and run directory consistent.

To inspect analysis options for existing results:

```powershell
python tools/performance/analyze_navigation.py --help
python tools/performance/extract_campaign_evidence.py --help
```

The protocol documents trace-processor setup, complete measurement matrices and current analyzer limitations.

## Test the helpers

```powershell
pwsh -NoProfile -File tools/review/tests/review-tools.tests.ps1
python -m unittest discover -s tools/performance -p "test_*.py"
python -m unittest discover -s tools/sdk -p "test_*.py"
```

Browser integration checks are separate; use the review and SDK guides above. Passing helper tests does not establish application/device acceptance.

## Local output

Keep generated output ignored: review captures/provenance under `build/review/`, SDK HTML under `build/sdk-docs/`, documentation check reports/caches under `build/sdk-guide/` and `build/sdk-explorer/`, and benchmark data under `benchmark-results/`. Downloaded trace tooling under `tools/performance/.tools/` and Python bytecode caches also stay local.

Track script source, tests, authored documentation inputs and required configuration. Keep credentials in the documented ignored locations and out of commands, reports and committed files.
