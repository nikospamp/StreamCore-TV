#requires -Version 7.0
<#
.SYNOPSIS
Inspect the local review environment and emit one JSON result. No builds or installs.
.EXAMPLE
pwsh -NoProfile -File tools/dev/preflight.ps1 -Scope Android -CheckDevices
.EXAMPLE
pwsh -NoProfile -File tools/dev/preflight.ps1 -Scope All -CheckDevices -StartAdb
#>
[CmdletBinding()]
param(
    [ValidateSet('Android', 'Web', 'All')][string]$Scope = 'All',
    [string]$SdkRoot,
    [string]$LocalPropertiesPath,
    [switch]$CheckDevices,
    [ValidatePattern('^[A-Za-z0-9_.:\-]+$')][string]$Serial,
    [switch]$StartAdb,
    [ValidateRange(1024, 65535)][int]$AdbServerPort = 5037,
    [ValidateSet('chrome', 'chromium', 'msedge')][string]$BrowserChannel = 'chrome',
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [switch]$AllowUnverifiedArtifacts
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'review-common.ps1')
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$issues = [Collections.Generic.List[object]]::new()
$result = [ordered]@{ schemaVersion = 1; success = $false; checkout = $root; scope = $Scope; powershell = "$($PSVersionTable.PSVersion)" }
function Add-Issue([string]$Code, [string]$Message, [string]$Severity = 'error') {
    $issues.Add([pscustomobject]@{ code = $Code; severity = $Severity; message = $Message })
}
try {
    if ($StartAdb -and -not $CheckDevices) { throw '-StartAdb requires -CheckDevices.' }
    if ($Serial -and (-not $CheckDevices -or $Scope -eq 'Web')) { throw '-Serial requires -CheckDevices and an Android scope.' }
    $git = Get-Command git -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($git) {
        $head = Invoke-ReviewProcess $git.Source @('-C', $root, 'rev-parse', 'HEAD')
        $branch = Invoke-ReviewProcess $git.Source @('-C', $root, 'branch', '--show-current')
        $changes = Invoke-ReviewProcess $git.Source @('-C', $root, 'status', '--porcelain')
        $result.git = @{ commit = $head.stdout; branch = $branch.stdout; dirty = -not [string]::IsNullOrEmpty($changes.stdout); status = $changes.status }
    }
    $environment = Get-ReviewAndroidEnvironment $root $SdkRoot $LocalPropertiesPath
    $result.localProperties = $environment.localProperties
    if ($environment.localProperties.status -eq 'access-denied') { Add-Issue 'local-properties-access-denied' 'The selected local properties file is not readable.' }
    if (-not $environment.localProperties.tmdbReadAccessTokenPresent -or -not $environment.localProperties.tmdbAccountIdPresent) { Add-Issue 'runtime-config-not-confirmed' 'TMDB runtime settings were not both found in the selected file. The authenticated build gate remains authoritative for Gradle overrides.' 'warning' }

    if ($Scope -in @('Android', 'All')) {
        $javaPathCommand = Get-Command java -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        $javaPath = if ($javaPathCommand) { $javaPathCommand.Source } else { $null }
        $javaHomePath = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin/$(if ($IsWindows) { 'java.exe' } else { 'java' })" } else { $null }
        $java = [ordered]@{ path = $javaPath; javaHome = $env:JAVA_HOME; daemonVersion = $null; pathVersion = $null; javaHomeVersion = $null }
        foreach ($entry in @(@{ path = $javaPath; key = 'pathVersion' }, @{ path = $javaHomePath; key = 'javaHomeVersion' })) {
            if ($entry.path) {
                $probe = Invoke-ReviewProcess $entry.path @('-version')
                $versionLine = (($probe.stdout + "`n" + $probe.stderr).Trim() -split '\r?\n')[0]
                $java[$entry.key] = @{ status = $probe.status; version = $versionLine }
            }
        }
        $daemonPath = Join-Path $root 'gradle/gradle-daemon-jvm.properties'
        $daemonMatch = [regex]::Match([IO.File]::ReadAllText($daemonPath), '(?m)^toolchainVersion=(\d+)')
        if ($daemonMatch.Success) { $java.daemonVersion = [int]$daemonMatch.Groups[1].Value }
        $java.status = Get-ReviewJavaAvailability $java.pathVersion $java.javaHomeVersion
        if ($java.status -ne 'ready') { Add-Issue "java-$($java.status)" 'Neither PATH Java nor JAVA_HOME provides a usable Java executable.' }
        if ($javaHomePath -and $java.javaHomeVersion.status -ne 'ready') { Add-Issue "java-home-$($java.javaHomeVersion.status)" 'JAVA_HOME does not provide a usable Java executable.' }
        if ($javaPath -and $javaHomePath -and $javaPath -ne $javaHomePath) { Add-Issue 'java-selection-differs' 'PATH Java and JAVA_HOME differ. Gradle daemon selection is governed separately by its checked-in JVM criteria.' 'warning' }
        $result.java = $java

        $android = [ordered]@{
            sdkRoot = $environment.sdkRoot; source = $environment.source; candidates = $environment.candidates
            adb = $environment.adb; adbVersion = $null; platform37 = @(); devices = @(); selectedSerial = $Serial
            adbServer = 'not-checked'; deviceInventory = 'not-checked'
            readyDeviceCount = $(if ($CheckDevices) { 0 } else { $null })
            deviceCheck = $(if ($CheckDevices) { 'unavailable' } else { 'skipped' })
        }
        if ($environment.conflict) { Add-Issue 'sdk-selection-differs' 'SDK candidates differ. Commands consistently use the selected SDK; environment variables were not changed.' 'warning' }
        if (-not $environment.sdkRoot) { Add-Issue 'sdk-missing' 'Set -SdkRoot, sdk.dir, ANDROID_HOME, or ANDROID_SDK_ROOT.' }
        else {
            $adbProbe = Invoke-ReviewProcess $environment.adb @('version')
            $android.adbVersion = @{ status = $adbProbe.status; version = ($adbProbe.stdout -split '\r?\n')[0] }
            if ($adbProbe.status -ne 'ready') { Add-Issue "adb-$($adbProbe.status)" 'Selected SDK ADB is not usable.' }
            try {
                $platforms = Get-ChildItem -LiteralPath (Join-Path $environment.sdkRoot 'platforms') -Directory -ErrorAction Stop
                foreach ($platform in $platforms) {
                    $api = Get-ReviewPlatformApiLevel $platform.FullName
                    if ($api -eq 37 -and (Test-Path -LiteralPath (Join-Path $platform.FullName 'android.jar'))) { $android.platform37 += $platform.FullName }
                }
                if (-not $android.platform37.Count) { Add-Issue 'platform-37-missing' 'No installed platform with API 37 metadata and android.jar was found.' }
            } catch { Add-Issue "sdk-platforms-$(Get-ReviewFailureKind $_.Exception.Message)" 'Cannot inspect selected SDK platform metadata.' }
            if ($CheckDevices -and $adbProbe.status -eq 'ready') {
                $connection = Get-ReviewAdbConnection $environment.adb $AdbServerPort -StartAdb:$StartAdb
                $android.adbServer = $connection.status
                if ($connection.status -eq 'ready') {
                    $inventory = Invoke-ReviewAdbHostQuery 'host:devices-l' $AdbServerPort
                    $android.deviceInventory = $inventory.status
                    if ($inventory.status -eq 'ready') {
                        foreach ($line in ($inventory.value -split '\r?\n')) {
                            if ($line -notmatch '^(\S+)\s+(\S+)(.*)$') { continue }
                            $device = [ordered]@{ serial = $Matches[1]; state = $Matches[2]; role = $null; type = $(if ($Matches[1] -like 'emulator-*') { 'emulator' } else { 'unknown' }) }
                            $boot = $null
                            $service = $null
                            if ($device.state -eq 'device') {
                                $prefix = @('-H', '127.0.0.1', '-P', "$AdbServerPort", '-s', $device.serial)
                                $boot = Invoke-ReviewProcess $environment.adb ($prefix + @('shell', 'getprop', 'sys.boot_completed')) 5
                                $service = Invoke-ReviewProcess $environment.adb ($prefix + @('shell', 'service', 'check', 'package')) 5
                            }
                            $readiness = Get-ReviewDeviceReadiness $device.state $boot $service
                            $device.status = $readiness.status
                            $device.bootComplete = $readiness.bootComplete
                            $device.packageService = $readiness.packageService
                            $device.errors = $readiness.errors
                            $android.devices += $device
                        }
                    }
                }
                $summary = Get-ReviewDeviceCheckSummary $android.adbServer $android.deviceInventory $android.devices $Serial
                $android.deviceCheck = $summary.status
                $android.readyDeviceCount = $summary.readyDeviceCount
                if ($summary.status -ne 'ready') { Add-Issue $summary.code $summary.message }
            }
        }
        $defaultApk = Join-Path $root 'app/build/outputs/apk/tmdb/debug/app-tmdb-debug.apk'
        $android.artifact = @{ path = $defaultApk; status = 'unknown'; variant = 'tmdbDebug' }
        $artifactNode = Get-Command node -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($artifactNode) {
            $artifactProbe = Invoke-ReviewProcess $artifactNode.Source @((Join-Path $root 'webApp/e2e/review-build.mjs'), 'status', '--target', 'android', '--artifact', $defaultApk, '--local-properties', $environment.localProperties.path) 30 $root
            if ($artifactProbe.status -eq 'ready') {
                $artifactStatus = $artifactProbe.stdout | ConvertFrom-Json
                $android.artifact.status = $artifactStatus.status
            } else { Add-Issue 'apk-provenance-unavailable' 'Could not verify default tmdbDebug APK provenance.' 'warning' }
        }
        if ($android.artifact.status -ne 'fresh') { Add-Issue 'apk-artifact-unverified' 'Default tmdbDebug APK freshness is not verified. Build with review-build.mjs; query other variants explicitly.' 'warning' }
        $result.android = $android
    }

    if ($Scope -in @('Web', 'All')) {
        $uri = Resolve-ReviewBaseUri $BaseUrl
        $node = Get-Command node -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        $npm = Get-Command npm.cmd, npm -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        $web = [ordered]@{ node = $null; npm = $(if ($npm) { $npm.Source } else { $null }); playwright = 'missing'; browser = $null; server = $null }
        $managedChromiumPath = $null
        if ($node) {
            $probe = Invoke-ReviewProcess $node.Source @('--version')
            $web.node = @{ path = $node.Source; version = $probe.stdout; status = $probe.status }
            if ($probe.status -ne 'ready' -or $probe.stdout -notmatch '^v(\d+)' -or [int]$Matches[1] -lt 20) { Add-Issue 'node-unusable' 'Node 20 or later is required.' }
        } else { Add-Issue 'node-missing' 'Node 20 or later is required.' }
        if (-not $npm) { Add-Issue 'npm-missing' 'npm is not available on PATH.' }
        $playwrightPackage = Join-Path $root 'webApp/e2e/node_modules/@playwright/test/package.json'
        if (Test-Path -LiteralPath $playwrightPackage) {
            $installedVersion = ([IO.File]::ReadAllText($playwrightPackage) | ConvertFrom-Json).version
            $expectedVersion = ([IO.File]::ReadAllText((Join-Path $root 'webApp/e2e/package.json')) | ConvertFrom-Json).devDependencies.'@playwright/test'
            $web.playwright = @{ installed = $installedVersion; expected = $expectedVersion; browserLaunch = 'not-tested' }
            if ($installedVersion -ne $expectedVersion) { Add-Issue 'playwright-version-mismatch' 'Run npm ci in webApp/e2e to restore the pinned dependency.' }
            if ($node) {
                $browserProbe = Invoke-ReviewProcess $node.Source @('-e', 'const {chromium}=require(process.argv[1]);const fs=require("node:fs");const path=chromium.executablePath();console.log(JSON.stringify({path,installed:fs.existsSync(path)}));', (Join-Path $root 'webApp/e2e/node_modules/playwright'))
                if ($browserProbe.status -eq 'ready') {
                    $web.playwright.chromium = $browserProbe.stdout | ConvertFrom-Json
                    $managedChromiumPath = $web.playwright.chromium.path
                } elseif ($BrowserChannel -eq 'chromium') { Add-Issue 'chromium-inspection-failed' 'Unable to inspect the pinned Playwright Chromium binary. No browser was launched.' }
            }
        } else { Add-Issue 'playwright-missing' 'Run npm ci in webApp/e2e. No dependencies were installed.' }
        $browserCandidates = @()
        if ($BrowserChannel -eq 'chromium') { $browserCandidates = @($managedChromiumPath) }
        else {
            $browserCommand = Get-Command $BrowserChannel -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($browserCommand) { $browserCandidates += $browserCommand.Source }
            $browserRelativePath = if ($BrowserChannel -eq 'chrome') { 'Google/Chrome/Application/chrome.exe' } else { 'Microsoft/Edge/Application/msedge.exe' }
            foreach ($installationRoot in @($env:ProgramFiles, ${env:ProgramFiles(x86)}, $env:LOCALAPPDATA)) {
                if ($installationRoot) { $browserCandidates += Join-Path $installationRoot $browserRelativePath }
            }
        }
        $browserAvailability = Get-ReviewExecutableAvailability $browserCandidates
        $web.browser = @{ channel = $BrowserChannel; status = $browserAvailability.status; path = $browserAvailability.path; launch = $browserAvailability.launch }
        if ($browserAvailability.status -ne 'ready') { Add-Issue 'browser-missing' "The selected $BrowserChannel executable was not found. Select an installed channel with -BrowserChannel; no browser was installed or launched." }
        try {
            $identity = Invoke-RestMethod -Uri "$($uri.GetLeftPart([UriPartial]::Authority))/__streamcore_review" -TimeoutSec 5 -MaximumRedirection 0
            if ($identity.service -ne 'streamcore-review' -or $identity.schemaVersion -ne 2) { throw 'Unexpected server identity.' }
            if ([IO.Path]::GetFullPath($identity.checkout) -ne $root) { throw 'Server belongs to another checkout.' }
            if ($identity.distribution -notin @('productionExecutable', 'developmentExecutable')) { throw 'Unexpected distribution.' }
            $expectedArtifact = [IO.Path]::GetFullPath((Join-Path $root "webApp/build/dist/wasmJs/$($identity.distribution)"))
            if (-not [IO.Path]::IsPathFullyQualified($identity.artifactPath) -or [IO.Path]::GetFullPath($identity.artifactPath) -ne $expectedArtifact) { throw 'Unexpected artifact path.' }
            $artifactState = 'unknown'
            if ($identity.runtimeConfigPath -and -not [IO.Path]::IsPathFullyQualified($identity.runtimeConfigPath)) { throw 'Runtime configuration path must be absolute.' }
            if ($identity.distribution -eq 'productionExecutable' -and $identity.runtimeConfigPath -and $node) {
                # Verify in this client process. The HTTP server only returns lightweight identity metadata.
                $statusProbe = Invoke-ReviewProcess $node.Source @((Join-Path $root 'webApp/e2e/review-build.mjs'), 'status', '--target', 'web', '--artifact', $identity.artifactPath, '--runtime-config', $identity.runtimeConfigPath, '--local-properties', $environment.localProperties.path) 30 $root
                if ($statusProbe.status -eq 'ready') { $artifactState = ($statusProbe.stdout | ConvertFrom-Json).status }
            }
            $web.server = @{ status = $artifactState; url = $BaseUrl; checkout = $identity.checkout; distribution = $identity.distribution; unverifiedArtifactsAllowed = [bool]$AllowUnverifiedArtifacts }
            if ($artifactState -ne 'fresh') {
                $severity = if ($AllowUnverifiedArtifacts) { 'warning' } else { 'error' }
                Add-Issue 'web-artifact-unverified' 'Artifact freshness is unknown or stale. Use the normal development workflow or explicitly allow unverified review artifacts.' $severity
            }
        } catch {
            $web.server = @{ status = 'unverified'; url = $BaseUrl }
            Add-Issue 'web-server-unverified' 'No matching review-server identity was verified. Start the project review server; HTTP 200 alone is insufficient.'
        }
        $result.web = $web
    }
} catch { Add-Issue (Get-ReviewFailureKind $_.Exception.Message) $_.Exception.Message }
$result.issues = @($issues.ToArray())
$result.success = @($issues | Where-Object severity -eq 'error').Count -eq 0
$result | ConvertTo-Json -Depth 10 -Compress
if (-not $result.success) { exit 1 }
