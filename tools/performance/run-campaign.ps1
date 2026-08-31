param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [Parameter(Mandatory = $true)][string]$ContentTag,
    [string]$ProfileName = 'Nikos',
    [string]$RunPrefix = (Get-Date -Format 'yyyyMMdd-HHmmss'),
    [string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
)

$ErrorActionPreference = 'Stop'
if ($RunPrefix -notmatch '^[A-Za-z0-9_.-]+$') { throw 'RunPrefix must be a simple directory name.' }
$sdk = & $Adb -s $Serial shell getprop ro.build.version.sdk
if ($LASTEXITCODE -ne 0) { throw 'Cannot identify connected device.' }
if ([int]$sdk -lt 34) { throw 'Controlled campaign requires Android 14+ to preserve target authentication/cache during compilation reset.' }
$workspace = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$campaignDirectory = Join-Path $workspace "benchmark-results/$RunPrefix"
New-Item -ItemType Directory -Force -Path $campaignDirectory | Out-Null
$journalPath = Join-Path $campaignDirectory 'campaign.json'
$apkHashes = [ordered]@{}
foreach ($variant in @('benchmark', 'benchmarkR8')) {
    $apk = Join-Path $workspace "app/build/outputs/apk/tmdb/$variant/app-tmdb-$variant.apk"
    $apkHashes[$variant] = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
}
$driverApk = Join-Path $workspace 'benchmark/build/outputs/apk/tmdb/benchmark/benchmark-tmdb-benchmark.apk'
$driverHash = (Get-FileHash -LiteralPath $driverApk -Algorithm SHA256).Hash
if (Test-Path -LiteralPath $journalPath) {
    $journal = Get-Content -LiteralPath $journalPath -Raw | ConvertFrom-Json -AsHashtable
    if ($journal.serial -ne $Serial) {
        throw 'Device connection changed. Start a distinct campaign; never mix phones.'
    }
    if ($journal.driverHash -ne $driverHash -or $journal.contentTag -ne $ContentTag -or $journal.profileName -ne $ProfileName) {
        throw 'Driver or dataset changed. Start a new campaign instead of mixing results.'
    }
    foreach ($variant in @('benchmark', 'benchmarkR8')) {
        if ($journal.apkHashes[$variant] -ne $apkHashes[$variant]) { throw 'Target APK changed. Start a new campaign.' }
    }
} else {
    $journal = [ordered]@{
        prefix = $RunPrefix; serial = $Serial; profileName = $ProfileName; contentTag = $ContentTag
        apkHashes = $apkHashes; driverHash = $driverHash; batches = @(); startedAt = (Get-Date -Format o)
    }
    & git -C $workspace diff --binary HEAD "--output=$campaignDirectory/source.patch" -- . ':(exclude)**/__pycache__/**'
}
$journeys = @('profilesToHome', 'homeToDetails', 'playerToDetails', 'initialSearch')
for ($journeyIndex = 0; $journeyIndex -lt $journeys.Count; $journeyIndex++) {
    $journey = $journeys[$journeyIndex]
    # Alternate A/B order by journey to reduce a single systematic run-order bias.
    $variants = if ($journeyIndex % 2 -eq 0) { @('benchmark', 'benchmarkR8') } else { @('benchmarkR8', 'benchmark') }
    foreach ($variant in $variants) {
        $completed = @($journal.batches | Where-Object { $_.journey -eq $journey -and $_.variant -eq $variant -and $_.status -eq 'complete' })
        if ($completed.Count -gt 0) { continue }
        $attempt = @($journal.batches | Where-Object { $_.journey -eq $journey -and $_.variant -eq $variant }).Count
        $batchRunId = "$RunPrefix-$journey-a$attempt"
        $fixtureRunId = "$RunPrefix-$journey-fixture-a$attempt"
        $batch = [ordered]@{ journey=$journey; variant=$variant; runId=$batchRunId; status='running'; startedAt=(Get-Date -Format o) }
        $journal.batches += $batch
        $journal | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $journalPath -Encoding utf8
        try {
            Write-Output "Preparing $journey / $variant"
            & "$PSScriptRoot/run-navigation.ps1" -Serial $Serial -ContentTag $ContentTag -ProfileName $ProfileName -Variant $variant -Phase Fixture -CleanupIterations 0 -RunId $fixtureRunId -Adb $Adb > (Join-Path $campaignDirectory "$journey-$variant-fixture-$attempt.log") 2>&1
            Write-Output "Measuring $journey / ${variant}: 40 iterations"
            & "$PSScriptRoot/run-navigation.ps1" -Serial $Serial -ContentTag $ContentTag -ProfileName $ProfileName -Variant $variant -Journey $journey -RunId $batchRunId -Adb $Adb -SkipInstall > (Join-Path $campaignDirectory "$journey-$variant-$attempt.log") 2>&1
            $batch.status = 'complete'
            $batch.output = Join-Path $workspace "benchmark-results/$batchRunId/$variant"
        } catch {
            $batch.status = 'failed'
            $batch.error = $_.Exception.Message
            throw
        } finally {
            $batch.finishedAt = Get-Date -Format o
            $journal | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $journalPath -Encoding utf8
        }
    }
}
$resultDirectories = @(
    $journal.batches | Where-Object status -eq 'complete' | ForEach-Object {
        if ($_.outputs) { $_.outputs } else { $_.output }
    }
)
& python "$PSScriptRoot/analyze_navigation.py" @resultDirectories --output (Join-Path $campaignDirectory 'summary') --require-complete
if ($LASTEXITCODE -ne 0) { throw 'Campaign finished but coverage/provenance validation failed.' }
$journal.finishedAt = Get-Date -Format o
$journal | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $journalPath -Encoding utf8
Write-Output "Complete campaign: $campaignDirectory"
