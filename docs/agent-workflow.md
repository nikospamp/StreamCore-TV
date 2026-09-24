# Portable setup and client review

Use the applicable helpers for environment setup, launch/capture, and comparison boards. For source-only work,
inspect the relevant code and run targeted checks without starting devices or browsers. The target
architecture remains backend-agnostic; this tooling does not change application modules or UI behavior.

The certified build path below is specifically for authenticated `tmdbDebug` and production web acceptance
reviews. It is optional developer tooling, not a new gate on normal development. Continue using Android Studio,
the Gradle wrapper, `clientB`, benchmark variants, and `:webApp:wasmJsBrowserDevelopmentRun` as appropriate.
Those paths do not require a provenance record or a production rebuild. Android display capture and board
composition work independently of the certified build wrapper; use the existing install/run procedure for
other variants and identify the actual variant in the review evidence.

## Ownership and completion

1. Define the requested screens/clients, deliverable, and observable acceptance cases.
2. One coordinator owns shared setup, builds, and servers. Establish readiness before delegating clients.
3. Give each worker its device serial or URL, artifact, starting state, output path, checks, and stop condition.
   Workers report blockers, dependency changes, and completion. Do not repeat unchanged status requests or
   restart a worker awaiting permission. Use bounded 10–60 second tool waits; scripts handle short internal waits.
4. Run checks appropriate to the change, including cheap required gates and owning-module compilation before
   integration. Coordinate shared Gradle/webpack/Binaryen staging with IDE builds. A task-specific test deferral
   applies only within its stated scope; these helpers do not defer application tests.
5. Inspect every original capture for the requested screen/content, completed loading, corners, and relevant
   focus/disabled states. A successful screenshot command does not establish visual correctness.
6. Compose the originals deterministically. Conclude with accepted behavior, changed files/commit, completed
   checks, remaining issues, and next action. Start a new task at the next independent accepted-work boundary.

## Shared files, local state

Both Windows PCs use the same Git version of these scripts. They require PowerShell 7 and Node 20 or newer.
Playwright is already pinned in `webApp/e2e/package-lock.json`; install dependencies with
`npm --prefix webApp/e2e ci` once when missing or the lockfile changes. Capture defaults to installed Chrome;
`--channel chromium` uses the project Playwright browser (`npm --prefix webApp/e2e exec playwright install chromium`
when explicitly needed). No private Codex runtime path is required.

All script-relative paths derive from their own location. CLI file paths resolve from the repository;
board entry paths resolve from the manifest. The commands below assume a repository-root terminal only for brevity.
Keep credentials, generated configuration, profiles, locks, logs, manifests, and captures in ignored locations.
Default evidence is `build/review/`. Use a new output name for each iteration; capture/board tools refuse to
overwrite evidence. Browser sessions and device serials never transfer through Git.

Use `-SdkRoot` for an intentional SDK override. Otherwise PowerShell tools resolve `sdk.dir` from the selected
local properties, then `ANDROID_HOME`, `ANDROID_SDK_ROOT`, and PATH ADB. The selected properties file is
`-LocalPropertiesPath`, `STREAMCORE_LOCAL_PROPERTIES`, or repository `local.properties`, in that order.
These controls select tooling inputs; they do not rewrite environment variables or Gradle's configuration precedence.
Gradle project properties can override values, and the repository local file precedes the linked fallback.

## Check once, repair only the reported capability

```powershell
pwsh -NoProfile -File tools/dev/preflight.ps1 -Scope Android -CheckDevices -Serial '<serial>'
pwsh -NoProfile -File tools/dev/preflight.ps1 -Scope Web -BrowserChannel chrome -BaseUrl http://127.0.0.1:8080
```

Preflight emits one JSON result and exits nonzero for blocking issues. It does not build, install, launch apps,
start browsers, or stop processes. Device queries use an existing compatible ADB server. Add `-StartAdb` together
with `-CheckDevices` only when starting the selected SDK's server is intended. An incompatible occupied ADB port
is reported rather than restarted. API 37 is identified through package metadata, including suffixed SDK directories.
Configured Java, PATH Java, and checked-in daemon criteria are reported separately.
Device inventory exposes `readyDeviceCount` and each device's readiness independently of ADB server connectivity.
Use `-Serial` to require a particular device; otherwise any ready device is sufficient and unrelated offline devices
do not block it. `-CheckDevices` fails when no usable device is available. Omit that flag for tool-only inspection.
For review-server development distributions, `-AllowUnverifiedArtifacts` retains an explicit freshness warning while
checking server identity and browser availability; it does not require a production rebuild.

Permission denial is a permission issue; do not respond by reinstalling SDKs/browsers. Resolve the specific host
approval or access issue, then rerun the failed check. No global security policy or model settings are changed.

## Build only when needed; record provenance

Existing artifacts without a recorded successful build have `unknown` freshness. Status is an inspection command
and exits zero even for stale/unknown results: consumers must inspect the JSON `status`.

```powershell
node webApp/e2e/review-build.mjs status --target all
node webApp/e2e/review-build.mjs build --target android
node webApp/e2e/review-build.mjs build --target web
```

`build --target all` runs the two targets sequentially in one Gradle invocation. Android uses `tmdbDebug` (one APK
for mobile/tablet/TV); web uses production distribution plus the existing release validator. Every build includes
`:app:verifyTmdbRuntimeConfig -PrequireTmdbRuntimeConfig=true`. Use `--local-properties <absolute path>` for a linked
checkout; set `STREAMCORE_LOCAL_PROPERTIES` to the same path for later status/server calls, or pass the same option
to status. No credential values are placed on the command line or in JSON results.

Build logs and successful provenance records live under `build/review/`. Records bind the current checkout,
target-specific production source contents (including relevant dirty/new files and build configuration), artifact hashes, eligible local/user Gradle configuration,
and web runtime configuration. Changed inputs during a build prevent certification. Custom Gradle init scripts
are outside the supported certification boundary and report an explicit blocker. Provenance verifies recorded
inputs, not application correctness. It cannot certify arbitrary external concurrent builds.

The build lock coordinates these wrappers only. Stop development serving before production web builds and keep
one build owner across scripts, the IDE, and agents. On a crash/timeout, inspect the owning process and log before
manually removing a stale `build/review/build.lock`. Never remove another active worker's lock.

## Serve the verified web artifact

```powershell
$env:STREAMCORE_WEB_PORT = '8080'
$env:STREAMCORE_WEB_DISTRIBUTION = 'production'
$env:STREAMCORE_WEB_RUNTIME_CONFIG = (Resolve-Path 'webApp/build/generated/webDevelopmentConfig/config.json').Path
node webApp/e2e/server.mjs
```

The server binds loopback. `/__streamcore_review` returns lightweight checkout, PID, distribution, artifact-path,
and runtime-config-path metadata. It never claims artifact freshness or hashes the repository while serving requests.
Preflight/capture clients verify provenance separately after checking server identity.
HTTP 200 alone is insufficient. An older already-running server must be inspected and restarted by its owner to
load new server code. Do not kill an unidentified port occupant. Stop only a process started by this task; use the
owning terminal/session. If using PowerShell `Start-Process` for a background server, use `-WindowStyle Hidden`,
retain the returned process, and redirect logs into `build/review/`. Runtime configuration remains outside the
deployable distribution.

## Capture Android

Navigate/sign in using the established device interaction tools, then capture the selected display:

```powershell
pwsh -NoProfile -File tools/review/capture-android.ps1 -Serial '<serial>' -Role mobile -Output build/review/mobile.png
```

Use roles `mobile`, `tablet`, or `tv` with explicit serials from preflight. The helper waits for device connection,
completed boot, and package service. It uses a unique remote screenshot file and pulls the PNG without shell binary
redirection. It removes only its own temporary capture. Login and screen navigation remain explicit agent/manual
steps; the helper does not guess coordinates, enter credentials, or claim semantic screen verification.

Optional `-InstallApk app/build/outputs/apk/tmdb/debug/app-tmdb-debug.apk` requires fresh recorded provenance.
Optional `-Launch` starts `com.pampoukidis.streamcoretv/.MainActivity`; override `-Package`/`-Activity` intentionally.
Launching does not navigate to a requested screen. Without installation, the installed artifact's provenance remains
unknown. `-TimeoutSeconds`, `-SettleMilliseconds`, SDK/properties overrides, and `-StartAdb` are explicit controls.

## Capture web

```powershell
node webApp/e2e/review-web.mjs --screen profiles --output build/review/web-profiles.png
node webApp/e2e/review-web.mjs --screen home --profile '<profile name>' --output build/review/web-home.png
node webApp/e2e/review-web.mjs --screen details --profile '<profile name>' --asset '<visible exact title>' --output build/review/web-details.png
```

The runner checks server identity/freshness, locks its local browser profile, restores authentication only when
required, and uses the existing app route/semantic readiness signals. Multiple profiles require `--profile`;
Details requires `--asset`. A missing/offscreen selection fails rather than silently capturing unrelated content.
Credentials default to ignored `docs/credentials/tmdb.txt` with username/password fields, or `--credentials-file`.
They are loaded only for login and are not logged. Browser navigation and credential entry stay on the verified
loopback origin. No browser state is exported.

Defaults: Chrome, 1920×1080, `build/review/browser` profile, 60-second per-step timeout, 1500ms final canvas settlement.
Use `--headless`, `--viewport`, `--base-url`, `--channel`, `--user-data-dir`, `--timeout-ms`, or `--settle-ms` as needed.
Settlement remains a bounded allowance; inspect the screenshot for artwork completion. Normal runs close their
browser and release their lock before returning. `--keep-open` emits completion and intentionally remains attached
until browser close/Ctrl+C. Do not run another review against that profile. After a crash, confirm its browser is
closed before manually removing its adjacent `.review-lock` directory.

For this review runner, `--allow-unverified-artifacts` permits captures of same-checkout development distributions
or existing stale/unknown builds without a certified production rebuild. Its result
records that limitation; do not present such captures as current-source verification. It never bypasses checkout
identity. Capture failure returns phase/error category without dumping page text or credential-bearing exceptions.
The ordinary Gradle development server and manual browser workflow remain available; they need not implement
the review-server identity endpoint or use this runner.

## Compose originals

Create `build/review/manifest.json` (include only the files actually captured):

```json
{
  "heading": "Profiles review",
  "entries": [
    { "label": "Mobile", "path": "mobile.png" },
    { "label": "Tablet", "path": "tablet.png" },
    { "label": "TV", "path": "tv.png" },
    { "label": "Web", "path": "web-profiles.png" }
  ]
}
```

```powershell
node webApp/e2e/review-board.mjs --manifest build/review/manifest.json --output build/review/profiles-board.png
```

The offline renderer uses original PNGs without redrawing/cropping, labels outside captures, and explicit display
scaling. Originals remain unchanged. The sidecar records their SHA256, dimensions, and placement/scaling.
`--columns`, `--cell-width`, and `--channel` control presentation; bounded image/board sizes prevent excessive
memory use. Font/browser differences can affect labels between PCs; screenshot source hashes remain authoritative.

## Validation on both PCs

```powershell
npm --prefix webApp/e2e run test:review-tools
pwsh -NoProfile -File tools/review/tests/review-tools.tests.ps1
$env:STREAMCORE_REVIEW_BROWSER_TEST = '1'
npm --prefix webApp/e2e run test:review-tools
Remove-Item Env:STREAMCORE_REVIEW_BROWSER_TEST
```

These are tooling checks, separate from deferred application regression suites. On each PC, also run preflight
from another working directory using an absolute script path, one real Android capture, and one real web capture.
Inspect both originals and the resulting board. Record the checkout, tool versions, pass/fail, and blockers without
secrets. PC 1 success alone does not establish PC 2 runtime compatibility.

Measure model responses, unchanged polls, setup failures, elapsed time and corrective iterations per comparable
accepted deliverable. Keep root, worker, and any nested CLI usage separate. No model/reasoning/compaction defaults
are changed by this workflow.
