param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [ValidateSet('benchmark', 'benchmarkR8')][string]$Variant = 'benchmark',
    [ValidateSet('Preflight', 'Fixture', 'Measure')][string]$Phase = 'Measure',
    [string]$ProfileName = 'Nikos',
    [string]$ContentTag,
    [ValidateRange(1, 100)][int]$Iterations = 10,
    [ValidateRange(0, 10)][int]$CleanupIterations = 3,
    [ValidateSet('', 'profilesToHome', 'homeToDetails', 'playerToDetails', 'initialSearch')][string]$Journey = '',
    [ValidateSet('', 'first', 'repeated')][string]$Entry = '',
    [ValidateSet('', 'None', 'Partial', 'Baseline', 'Ignore')][string]$Compilation = '',
    [string]$RunId = (Get-Date -Format 'yyyyMMdd-HHmmss'),
    [switch]$SkipInstall,
    [string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
)

$ErrorActionPreference = 'Stop'
$workspace = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
if ($RunId -notmatch '^[A-Za-z0-9_.-]+$') { throw 'RunId must be a simple directory name.' }
if ($ProfileName -notmatch '^[\p{L}\p{N}_-]+$') { throw 'Use a single-token benchmark profile name (letters, digits, underscore or hyphen).' }
if ($ContentTag -and $ContentTag -notmatch '^home:content:[A-Za-z0-9_.:-]+$') { throw 'Invalid contentTag.' }
if ($Phase -ne 'Preflight' -and [string]::IsNullOrWhiteSpace($ContentTag)) {
    throw 'Run Preflight first, then pin an exact contentTag from preflight.json for the whole comparison.'
}
$package = 'com.pampoukidis.streamcoretv.benchmark'
$driver = 'com.pampoukidis.streamcoretv.benchmark.driver'
$output = Join-Path $workspace "benchmark-results/$RunId/$Variant"
if (Test-Path -LiteralPath (Join-Path $output 'instrumentation.txt')) {
    throw 'This run already has evidence. Choose a new RunId; Macrobenchmark clears its output directory on startup.'
}
New-Item -ItemType Directory -Force -Path $output | Out-Null
$appApk = Join-Path $workspace "app/build/outputs/apk/tmdb/$Variant/app-tmdb-$Variant.apk"
$driverApk = Join-Path $workspace 'benchmark/build/outputs/apk/tmdb/benchmark/benchmark-tmdb-benchmark.apk'
foreach ($apk in @($appApk, $driverApk)) {
    if (-not (Test-Path -LiteralPath $apk)) { throw "Build the requested variant first: $apk" }
}

function Invoke-Device([string[]]$DeviceArguments) {
    $result = & $Adb -s $Serial @DeviceArguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw ($result -join "`n") }
    return ($result -join "`n")
}

$sdk = [int](Invoke-Device -DeviceArguments @('shell', 'getprop', 'ro.build.version.sdk'))
if ($Phase -eq 'Measure' -and $sdk -lt 34 -and $Compilation -ne 'Ignore') {
    throw 'Android <14 resets compilation by deleting target app data. Use explicit Ignore diagnostics or an Android 14+ device for the controlled matrix.'
}

$metadata = [ordered]@{
    runId = $RunId; variant = $Variant; phase = $Phase; serial = $Serial
    profileName = $ProfileName; contentTag = $ContentTag; iterations = $Iterations
    journey = $Journey; entry = $Entry; compilation = $Compilation
    commit = (& git -C $workspace rev-parse HEAD)
    appSha256 = (Get-FileHash -LiteralPath $appApk -Algorithm SHA256).Hash
    driverSha256 = (Get-FileHash -LiteralPath $driverApk -Algorithm SHA256).Hash
    startedAt = (Get-Date -Format o)
    model = Invoke-Device -DeviceArguments @('shell', 'getprop', 'ro.product.model')
    android = Invoke-Device -DeviceArguments @('shell', 'getprop', 'ro.build.version.release')
    sdk = $sdk
    deviceId = Invoke-Device -DeviceArguments @('shell', 'getprop', 'ro.serialno')
    compilationControlled = ($Compilation -ne 'Ignore')
    fingerprint = Invoke-Device -DeviceArguments @('shell', 'getprop', 'ro.build.fingerprint')
    thermalBefore = Invoke-Device -DeviceArguments @('shell', 'dumpsys', 'thermalservice')
    batteryBefore = Invoke-Device -DeviceArguments @('shell', 'dumpsys', 'battery')
    displayBefore = Invoke-Device -DeviceArguments @('shell', 'dumpsys', 'display')
    animatorScale = Invoke-Device -DeviceArguments @('shell', 'settings', 'get', 'global', 'animator_duration_scale')
    windowScale = Invoke-Device -DeviceArguments @('shell', 'settings', 'get', 'global', 'window_animation_scale')
    transitionScale = Invoke-Device -DeviceArguments @('shell', 'settings', 'get', 'global', 'transition_animation_scale')
    cachePolicy = 'Never clear app data. Disk cache retained. Process restarted for each sample; repeated entry primes once.'
}
$metadata | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $output 'run.json') -Encoding utf8

# Same separate package/signing key for both variants: preserve authentication and local state.
foreach ($installed in @(@($package, $metadata.appSha256, $appApk), @($driver, $metadata.driverSha256, $driverApk))) {
    $installedHash = $null
    try {
        $packagePath = (Invoke-Device -DeviceArguments @('shell', 'pm', 'path', '--user', '0', $installed[0])).Trim() -replace '^package:', ''
        if ($packagePath -notmatch '^/data/app/[A-Za-z0-9/_+.=~-]+\.apk$') { throw 'Unexpected installed APK path.' }
        $installedHash = ((Invoke-Device -DeviceArguments @('shell', 'sha256sum', $packagePath)) -split '\s+')[0]
    } catch {
        if ($SkipInstall) { throw }
    }
    if ($installedHash -eq $installed[1]) {
        Write-Output "Matching APK already installed: $($installed[0])"
    } elseif ($SkipInstall) {
        throw "Installed APK differs from requested build: $($installed[0])"
    } else {
        Invoke-Device -DeviceArguments @('install', '--user', '0', '-r', $installed[2]) | Write-Output
    }
}
$remoteOutput = "/sdcard/Android/media/$driver/results/$RunId/$Variant"
$testClass = switch ($Phase) {
    'Preflight' { 'com.pampoukidis.streamcoretv.benchmark.BenchmarkPreflight' }
    'Fixture' { 'com.pampoukidis.streamcoretv.benchmark.BenchmarkFixture' }
    'Measure' { 'com.pampoukidis.streamcoretv.benchmark.NavigationBenchmark' }
}
$testArguments = @('shell', 'am', 'instrument', '--user', '0', '-w', '-r', '-e', 'class', $testClass,
    '-e', 'profileName', $ProfileName, '-e', 'iterations', "$Iterations",
    '-e', 'cleanupIterations', "$CleanupIterations",
    '-e', 'additionalTestOutputDir', $remoteOutput, '-e', 'androidx.benchmark.fullTracing.enable', 'true')
foreach ($pair in @(@('contentTag', $ContentTag), @('journey', $Journey), @('entry', $Entry), @('compilation', $Compilation))) {
    if (-not [string]::IsNullOrWhiteSpace($pair[1])) { $testArguments += @('-e', $pair[0], $pair[1]) }
}
$testArguments += "$driver/androidx.test.runner.AndroidJUnitRunner"
try {
    & $Adb -s $Serial @testArguments 2>&1 | Tee-Object -FilePath (Join-Path $output 'instrumentation.txt')
    $instrumentationExit = $LASTEXITCODE
} finally {
    # Export evidence even on failure. Never delete a trace or clear the target's storage.
    & $Adb -s $Serial pull "$remoteOutput/." (Join-Path $output 'macrobenchmark') 2>&1 | Out-Null
    $metadata.thermalAfter = Invoke-Device -DeviceArguments @('shell', 'dumpsys', 'thermalservice')
    $metadata.batteryAfter = Invoke-Device -DeviceArguments @('shell', 'dumpsys', 'battery')
    $metadata.memoryAfter = Invoke-Device -DeviceArguments @('shell', 'dumpsys', 'meminfo', $package)
    $metadata.finishedAt = (Get-Date -Format o)
    $metadata | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $output 'run.json') -Encoding utf8
}
$log = Get-Content -LiteralPath (Join-Path $output 'instrumentation.txt') -Raw
if ($instrumentationExit -ne 0 -or $log -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=') {
    throw "Benchmark failed. Retained logs and traces at $output; do not treat it as a complete matrix."
}
if ($log -notmatch 'OK \([1-9][0-9]* tests?\)') { throw 'No successful JUnit result was reported.' }
if ($Phase -eq 'Measure') {
    $journeyCount = if ($Journey) { 1 } else { 4 }
    $entryCount = if ($Entry) { 1 } else { 2 }
    $modeCount = if ($Compilation) { 1 } else { 2 }
    $expectedTraces = $Iterations * $journeyCount * $entryCount * $modeCount
    $traces = @(Get-ChildItem -LiteralPath (Join-Path $output 'macrobenchmark') -Recurse -Filter '*.perfetto-trace')
    if ($traces.Count -lt $expectedTraces) { throw "Expected $expectedTraces traces, retrieved $($traces.Count)." }
}
Write-Output "Evidence: $output"
