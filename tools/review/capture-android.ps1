#requires -Version 7.0
<#
.SYNOPSIS
Capture the current Android display using the selected SDK. Navigation/login are separate.
.EXAMPLE
pwsh -NoProfile -File tools/review/capture-android.ps1 -Serial emulator-5554 -Role mobile
.EXAMPLE
pwsh -NoProfile -File tools/review/capture-android.ps1 -Serial emulator-5556 -Role tv -Launch -StartAdb
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^[A-Za-z0-9_.:\-]+$')][string]$Serial,
    [Parameter(Mandatory)][ValidateSet('mobile', 'tablet', 'tv')][string]$Role,
    [string]$Output,
    [string]$SdkRoot,
    [string]$LocalPropertiesPath,
    [switch]$StartAdb,
    [ValidateRange(1024, 65535)][int]$AdbServerPort = 5037,
    [ValidateRange(1, 300)][int]$TimeoutSeconds = 60,
    [ValidateRange(0, 10000)][int]$SettleMilliseconds = 750,
    [switch]$Launch,
    [string]$InstallApk,
    [ValidatePattern('^[A-Za-z][A-Za-z0-9_.]+$')][string]$Package = 'com.pampoukidis.streamcoretv',
    [ValidatePattern('^[A-Za-z.][A-Za-z0-9_.$]+$')][string]$Activity = '.MainActivity'
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '../dev/review-common.ps1')
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$phase = 'environment'
$remoteTemporary = $null
$localTemporary = $null
$environment = $null
$prefix = @('-H', '127.0.0.1', '-P', "$AdbServerPort", '-s', $Serial)
$result = [ordered]@{ schemaVersion = 1; success = $false; client = $Role; serial = $Serial; checkout = $root; semanticScreenVerified = $false; installedArtifactProvenance = 'unknown'; warnings = @() }

function Invoke-Device {
    param([string[]]$DeviceArguments, [int]$DeadlineSeconds = 15)
    $command = Invoke-ReviewProcess $environment.adb ($prefix + $DeviceArguments) $DeadlineSeconds
    if ($command.status -ne 'ready') {
        $kind = if (($command.stderr + $command.stdout) -match 'unauthorized') { 'device-unauthorized' } else { $command.status }
        throw "${kind}: ADB command failed during $phase. $($command.stderr.Substring(0, [Math]::Min(300, $command.stderr.Length)))"
    }
    return $command.stdout
}

try {
    $environment = Get-ReviewAndroidEnvironment $root $SdkRoot $LocalPropertiesPath
    if (-not $environment.adb) { throw 'missing: No SDK is configured. Set -SdkRoot or sdk.dir.' }
    $result.adb = $environment.adb
    if (-not $Output) { $Output = "build/review/$Role.png" }
    $outputPath = Resolve-ReviewPath $Output $root
    if ([IO.Path]::GetExtension($outputPath) -ne '.png') { throw 'Output must have a .png extension.' }
    $metadataPath = [IO.Path]::ChangeExtension($outputPath, '.json')
    if ([IO.File]::Exists($outputPath) -or [IO.File]::Exists($metadataPath)) { throw 'Output already exists. Choose a new -Output to preserve earlier evidence.' }

    $phase = 'artifact-provenance'
    if ($InstallApk) {
        $apkPath = Resolve-ReviewPath $InstallApk $root
        $node = Get-Command node -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $node) { throw 'missing: Node is required to verify APK provenance before installation.' }
        $provenanceArguments = @((Join-Path $root 'webApp/e2e/review-build.mjs'), 'status', '--target', 'android', '--artifact', $apkPath, '--local-properties', $environment.localProperties.path)
        $provenance = Invoke-ReviewProcess $node.Source $provenanceArguments 30 $root
        if ($provenance.status -ne 'ready') { throw 'APK provenance check failed. Build with review-build.mjs before installation.' }
        $artifact = $provenance.stdout | ConvertFrom-Json
        if ($artifact.status -ne 'fresh') { throw 'APK provenance is stale or unknown. Build with review-build.mjs before installation.' }
        $result.apk = $apkPath
    }

    $connection = Get-ReviewAdbConnection $environment.adb $AdbServerPort -StartAdb:$StartAdb
    if ($connection.status -ne 'ready') { throw "$($connection.status): $($connection.message)" }

    $phase = 'device-readiness'
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $ready = $false
    do {
        $remaining = [Math]::Max(1, [Math]::Min(5, [int][Math]::Ceiling(($deadline - [DateTime]::UtcNow).TotalSeconds)))
        $state = Invoke-ReviewProcess $environment.adb ($prefix + @('get-state')) $remaining
        if (($state.stderr + $state.stdout) -match 'unauthorized') { throw 'device-unauthorized: Accept the device debugging prompt, then retry.' }
        if ($state.status -eq 'access-denied') { throw 'access-denied: Unable to communicate with the device.' }
        if ($state.status -eq 'ready' -and $state.stdout -eq 'device') {
            if ([DateTime]::UtcNow -ge $deadline) { break }
            $remaining = [Math]::Max(1, [Math]::Min(5, [int][Math]::Ceiling(($deadline - [DateTime]::UtcNow).TotalSeconds)))
            $boot = Invoke-ReviewProcess $environment.adb ($prefix + @('shell', 'getprop', 'sys.boot_completed')) $remaining
            if ([DateTime]::UtcNow -ge $deadline) { break }
            $remaining = [Math]::Max(1, [Math]::Min(5, [int][Math]::Ceiling(($deadline - [DateTime]::UtcNow).TotalSeconds)))
            $service = Invoke-ReviewProcess $environment.adb ($prefix + @('shell', 'service', 'check', 'package')) $remaining
            $ready = $boot.status -eq 'ready' -and $boot.stdout -eq '1' -and $service.status -eq 'ready' -and $service.stdout -match ': found'
            if ($ready) { break }
        }
        if ([DateTime]::UtcNow -lt $deadline) { Start-Sleep -Milliseconds 1000 }
    } while ([DateTime]::UtcNow -lt $deadline)
    if (-not $ready) { throw 'timeout: Device boot and package service did not become ready before the deadline.' }

    if ($InstallApk) {
        $phase = 'install'
        $installed = Invoke-Device @('install', '-r', $apkPath) $TimeoutSeconds
        if ($installed -notmatch '(?m)^Success\s*$') { throw 'Installation did not report Success.' }
        $result.installedArtifactProvenance = 'fresh'
    }
    if ($Launch) {
        $phase = 'launch'
        $launched = Invoke-Device @('shell', 'am', 'start', '-W', '-n', "$Package/$Activity") $TimeoutSeconds
        if ($launched -match '(?im)^Error:|^Exception|Error type') { throw 'Activity launch failed; verify the package and activity.' }
    }
    if ($SettleMilliseconds -gt 0) { Start-Sleep -Milliseconds $SettleMilliseconds }

    $phase = 'capture'
    $outputDirectory = Split-Path $outputPath -Parent
    [void][IO.Directory]::CreateDirectory($outputDirectory)
    $captureId = [Guid]::NewGuid().ToString('N')
    $remoteTemporary = "/sdcard/streamcore-review-$captureId.png"
    $localTemporary = Join-Path $outputDirectory ".streamcore-review-$captureId.png"
    [void](Invoke-Device @('shell', 'screencap', '-p', $remoteTemporary))
    [void](Invoke-Device @('pull', $remoteTemporary, $localTemporary))
    $stream = [IO.File]::OpenRead($localTemporary)
    try {
        $header = [byte[]]::new(24)
        if ($stream.Read($header, 0, 24) -ne 24 -or [Convert]::ToHexString($header[0..7]) -ne '89504E470D0A1A0A') { throw 'Device capture did not produce a valid PNG header.' }
        $width = [Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($header, 16))
        $height = [Net.IPAddress]::NetworkToHostOrder([BitConverter]::ToInt32($header, 20))
        if ($width -le 0 -or $height -le 0) { throw 'Device capture has invalid dimensions.' }
    } finally { $stream.Dispose() }
    [IO.File]::Move($localTemporary, $outputPath)
    $localTemporary = $null
    $result.success = $true
    $result.screenshot = $outputPath
    $result.width = $width
    $result.height = $height
    $result.capturedAt = [DateTime]::UtcNow.ToString('o')
    $result.warnings += 'Captured the current display. Inspect the screenshot to confirm the requested screen, loading completion, and focus state.'
} catch {
    $result.phase = $phase
    $result.error = @{ kind = (Get-ReviewFailureKind $_.Exception.Message); message = $_.Exception.Message }
} finally {
    if ($remoteTemporary -and $environment) {
        # The only remote file removed is this invocation's GUID-named screenshot.
        $cleanup = Invoke-ReviewProcess $environment.adb ($prefix + @('shell', 'rm', '-f', $remoteTemporary)) 5
        if ($cleanup.status -ne 'ready') { $result.warnings += "Could not remove temporary device capture $remoteTemporary." }
    }
    if ($localTemporary -and [IO.File]::Exists($localTemporary)) {
        try { [IO.File]::Delete($localTemporary) }
        catch { $result.warnings += "Could not remove temporary local capture $localTemporary." }
    }
}
$json = $result | ConvertTo-Json -Depth 6 -Compress
if ($result.success) {
    try {
        $metadataStream = [IO.File]::Open($metadataPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
        try { $bytes = [Text.Encoding]::UTF8.GetBytes($json); $metadataStream.Write($bytes, 0, $bytes.Length) }
        finally { $metadataStream.Dispose() }
    } catch {
        $result.success = $false
        $result.phase = 'metadata'
        $result.error = @{ kind = (Get-ReviewFailureKind $_.Exception.Message); message = 'Screenshot was retained, but required metadata could not be written. Choose a new output path before retrying.' }
        $json = $result | ConvertTo-Json -Depth 6 -Compress
    }
}
$json
if (-not $result.success) { exit 1 }
