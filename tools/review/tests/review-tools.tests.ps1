#requires -Version 7.0
# Dependency-free checks. No SDK, device, Gradle, credentials, or browser required.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '../../dev/review-common.ps1')
$testRoot = Join-Path ([IO.Path]::GetTempPath()) "streamcore-review-tests-$([Guid]::NewGuid().ToString('N'))"
[void][IO.Directory]::CreateDirectory($testRoot)
$assertions = 0
function Assert-Equal($Actual, $Expected, [string]$Message) {
    if ($Actual -cne $Expected) { throw "${Message}: expected '$Expected', received '$Actual'." }
    $script:assertions++
}
$savedAndroidHome = $env:ANDROID_HOME
$savedAndroidSdk = $env:ANDROID_SDK_ROOT
$savedLinkedProperties = $env:STREAMCORE_LOCAL_PROPERTIES
try {
    foreach ($origin in @('http://127.0.0.1:8080', 'http://localhost:8080/', 'http://[::1]:8080')) {
        Assert-Equal (Resolve-ReviewBaseUri $origin).Scheme 'http' "Valid review origin $origin"
    }
    foreach ($invalidOrigin in @('https://localhost:8080', 'http://localhost:8080/home', 'http://localhost:8080?query=1', 'http://localhost:8080#fragment', 'http://user:pass@localhost:8080', 'http://example.com', '/relative')) {
        $rejected = $false
        try { [void](Resolve-ReviewBaseUri $invalidOrigin) } catch { $rejected = $true }
        Assert-Equal $rejected $true 'Invalid review origin rejected'
    }
    Assert-Equal (Get-ReviewJavaAvailability $null $null) 'missing' 'No configured Java'
    Assert-Equal (Get-ReviewJavaAvailability @{ status = 'access-denied' } $null) 'access-denied' 'Unusable PATH Java without JAVA_HOME blocks readiness'
    Assert-Equal (Get-ReviewJavaAvailability @{ status = 'failed' } @{ status = 'ready' }) 'ready' 'Usable JAVA_HOME is recognized'
    Assert-Equal (Get-ReviewJavaAvailability @{ status = 'ready' } $null) 'ready' 'Usable PATH Java is recognized'
    $fakeBrowser = Join-Path $testRoot 'chrome.exe'
    [IO.File]::WriteAllText($fakeBrowser, 'presence fixture; never executed')
    Assert-Equal (Get-ReviewExecutableAvailability @($fakeBrowser)).status 'ready' 'Selected installed browser does not require managed Chromium'
    Assert-Equal (Get-ReviewExecutableAvailability @((Join-Path $testRoot 'missing-chromium.exe'))).status 'missing' 'Selected missing browser is reported'
    Assert-Equal (Get-ReviewExecutableAvailability @($fakeBrowser)).launch 'not-tested' 'Executable presence never claims launch verification'

    $readyBoot = @{ status = 'ready'; stdout = '1' }
    $readyPackage = @{ status = 'ready'; stdout = 'Service package: found' }
    $readyDevice = Get-ReviewDeviceReadiness 'device' $readyBoot $readyPackage
    Assert-Equal $readyDevice.status 'ready' 'Device boot and package service are ready'
    Assert-Equal $readyDevice.bootComplete $true 'Device boot completion is explicit'
    Assert-Equal $readyDevice.packageService $true 'Device package readiness is explicit'
    Assert-Equal $readyDevice.errors.Count 0 'Ready device has no readiness errors'
    Assert-Equal (Get-ReviewDeviceReadiness 'offline').status 'offline' 'Offline transport is not a ready device'
    Assert-Equal (Get-ReviewDeviceReadiness 'unauthorized').status 'unauthorized' 'Unauthorized transport is not a ready device'
    Assert-Equal (Get-ReviewDeviceReadiness 'recovery').status 'probe-failed' 'Recovery transport is not a usable Android session'
    Assert-Equal (Get-ReviewDeviceReadiness 'device' @{ status = 'ready'; stdout = '' } $readyPackage).status 'booting' 'Missing boot completion is not ready'
    Assert-Equal (Get-ReviewDeviceReadiness 'device' $readyBoot @{ status = 'ready'; stdout = 'Service package: not found' }).status 'booting' 'Missing package service is not ready'
    $failedProbe = Get-ReviewDeviceReadiness 'device' @{ status = 'timeout'; stdout = ''; stderr = 'synthetic-secret-value' } $readyPackage
    Assert-Equal $failedProbe.status 'probe-failed' 'Failed probe is distinct from ongoing boot'
    Assert-Equal $failedProbe.errors[0].code 'boot-probe-timeout' 'Failed probe reports its specific classification'
    Assert-Equal (($failedProbe | ConvertTo-Json -Depth 5) -match 'synthetic-secret-value') $false 'Probe errors never emit raw output'
    Assert-Equal (Get-ReviewDeviceReadiness 'device' $null $null).status 'probe-failed' 'Absent probes cannot establish readiness'
    $mixedDevices = @(
        @{ serial = 'ready-device'; status = 'ready' },
        @{ serial = 'offline-device'; status = 'offline' },
        @{ serial = 'unauthorized-device'; status = 'unauthorized' }
    )
    $mixedSummary = Get-ReviewDeviceCheckSummary 'ready' 'ready' $mixedDevices
    Assert-Equal $mixedSummary.status 'ready' 'Unusable unrelated devices do not block a ready device'
    Assert-Equal $mixedSummary.readyDeviceCount 1 'Only usable devices are counted'
    Assert-Equal (Get-ReviewDeviceCheckSummary 'ready' 'ready' @()).status 'no-ready-devices' 'Successful empty inventory is not readiness'
    Assert-Equal (Get-ReviewDeviceCheckSummary 'ready' 'ready' @(@{ serial = 'offline-device'; status = 'offline' })).status 'no-ready-devices' 'Only offline devices fail readiness'
    Assert-Equal (Get-ReviewDeviceCheckSummary 'ready' 'ready' $mixedDevices 'ready-device').status 'ready' 'Explicit ready serial succeeds'
    Assert-Equal (Get-ReviewDeviceCheckSummary 'ready' 'ready' $mixedDevices 'offline-device').status 'selected-not-ready' 'Unrelated ready device cannot mask selected offline device'
    Assert-Equal (Get-ReviewDeviceCheckSummary 'ready' 'ready' $mixedDevices 'unknown-device').status 'selected-missing' 'Missing selected serial fails readiness'
    Assert-Equal (Get-ReviewDeviceCheckSummary 'ready' 'ready' $mixedDevices 'READY-DEVICE').status 'selected-missing' 'Device serial selection is case sensitive'
    Assert-Equal (Get-ReviewDeviceCheckSummary 'ready' 'timeout' $mixedDevices).status 'inventory-failed' 'Server connectivity cannot mask inventory failure'
    Assert-Equal (Get-ReviewDeviceCheckSummary 'ready' 'timeout' $mixedDevices).readyDeviceCount 0 'Failed inventory cannot retain a previous ready count'
    Assert-Equal (Get-ReviewDeviceCheckSummary 'conflict' 'ready' $mixedDevices).status 'adb-server-unavailable' 'Server protocol conflict fails readiness'
    Assert-Equal (Get-ReviewDeviceCheckSummary 'not-running' 'not-checked').readyDeviceCount 0 'Absent server has no confirmed ready devices'

    $propertiesPath = Join-Path $testRoot 'local.properties'
    $fixture = @'
# Synthetic fixtures only. No actual credentials are read by this test.
sdk.dir=C\:\\Android\ SDK\\sdk
tmdbReadAccessToken=synthetic-test-value
tmdbAccountId=
'@
    [IO.File]::WriteAllText($propertiesPath, $fixture)
    $properties = Get-ReviewLocalProperties $propertiesPath
    Assert-Equal $properties.sdkDir 'C:\Android SDK\sdk' 'Java property path escaping'
    Assert-Equal $properties.tmdbReadAccessTokenPresent $true 'Configuration presence'
    Assert-Equal $properties.tmdbAccountIdPresent $false 'Blank configuration'
    Assert-Equal (($properties | ConvertTo-Json) -match 'synthetic-test-value') $false 'Configuration contents must not be emitted'
    Assert-Equal (ConvertFrom-ReviewPropertyEscape 'C\:\\N\u0069kos\\Sdk') 'C:\Nikos\Sdk' 'Unicode escapes'
    Assert-Equal (Get-ReviewLocalProperties (Join-Path $testRoot 'missing.properties')).status 'missing' 'Missing configuration classification'
    [IO.File]::WriteAllText($propertiesPath, "sdk.dir=C\:\\Android\\\`n  sdk`ntmdbAccountId : fixture`n")
    Assert-Equal (Get-ReviewLocalProperties $propertiesPath).sdkDir 'C:\Android\sdk' 'Continued Java properties'

    $env:ANDROID_HOME = Join-Path $testRoot 'env-sdk'
    $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
    $env:STREAMCORE_LOCAL_PROPERTIES = $propertiesPath
    $explicitSdk = Join-Path $testRoot 'explicit sdk'
    $environment = Get-ReviewAndroidEnvironment $testRoot $explicitSdk
    Assert-Equal $environment.sdkRoot $explicitSdk 'Explicit SDK precedence'
    Assert-Equal $environment.source 'explicit' 'SDK source'
    Assert-Equal $environment.conflict $true 'SDK conflict reporting'
    Assert-Equal $environment.localProperties.path $propertiesPath 'Linked configuration precedence'
    Assert-Equal (Get-ReviewAndroidEnvironment $testRoot).source 'sdk.dir' 'Property SDK precedence over environment'

    $platformMetadata = Join-Path $testRoot 'source.properties'
    foreach ($api in @('37', '37.0', '37.2')) {
        [IO.File]::WriteAllText($platformMetadata, "AndroidVersion.ApiLevel=$api`n")
        Assert-Equal (Get-ReviewPlatformApiLevel $testRoot) 37 "SDK API $api metadata"
    }
    [IO.File]::WriteAllText($platformMetadata, "AndroidVersion.ApiLevel=36.1`n")
    Assert-Equal (Get-ReviewPlatformApiLevel $testRoot) 36 'Other SDK API must not match37'
    [IO.File]::Delete($platformMetadata)
    [IO.File]::WriteAllText((Join-Path $testRoot 'package.xml'), '<localPackage><type-details><api-level>37</api-level></type-details></localPackage>')
    Assert-Equal (Get-ReviewPlatformApiLevel $testRoot) 37 'SDK package XML fallback'

    $pwsh = (Get-Command pwsh -CommandType Application | Select-Object -First 1).Source
    $echoScript = Join-Path $testRoot 'echo arguments.ps1'
    [IO.File]::WriteAllText($echoScript, 'param([string]$Value) [Console]::Write($Value)')
    $literal = 'space path; $value & "quoted"'
    $echo = Invoke-ReviewProcess $pwsh @('-NoProfile', '-File', $echoScript, '-Value', $literal)
    Assert-Equal $echo.status 'ready' 'Process execution'
    Assert-Equal $echo.stdout $literal 'Arguments passed without shell interpolation'
    $timeout = Invoke-ReviewProcess $pwsh @('-NoProfile', '-Command', 'Start-Sleep -Seconds 20') 1
    Assert-Equal $timeout.status 'timeout' 'Bounded process timeout'
    $failure = Invoke-ReviewProcess $pwsh @('-NoProfile', '-Command', '[Console]::Error.WriteLine("Access is denied"); exit 1')
    Assert-Equal $failure.status 'access-denied' 'Permission failure classification'

    $preflightScript = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../dev/preflight.ps1'))
    foreach ($invalidSelection in @(
        @('-Scope', 'Android', '-Serial', 'fixture-device'),
        @('-Scope', 'Web', '-CheckDevices', '-Serial', 'fixture-device')
    )) {
        $selectionResult = Invoke-ReviewProcess $pwsh (@('-NoProfile', '-File', $preflightScript) + $invalidSelection)
        Assert-Equal $selectionResult.exitCode 1 'Invalid selection fails before environment inspection'
        Assert-Equal ($selectionResult.stdout | ConvertFrom-Json).issues[0].message '-Serial requires -CheckDevices and an Android scope.' 'Serial cannot silently skip device readiness'
    }

    $captureScript = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../capture-android.ps1'))
    $existingCapture = Join-Path $testRoot 'existing.png'
    [IO.File]::WriteAllText($existingCapture, 'preserved')
    $captureArguments = @('-NoProfile', '-File', $captureScript, '-Serial', 'fixture-not-a-device', '-Role', 'mobile', '-SdkRoot', $explicitSdk, '-Output', $existingCapture)
    $existingResult = Invoke-ReviewProcess $pwsh $captureArguments
    Assert-Equal $existingResult.exitCode 1 'Existing screenshot is rejected'
    Assert-Equal ($existingResult.stdout | ConvertFrom-Json).phase 'environment' 'Output collision fails before device work'
    Assert-Equal ([IO.File]::ReadAllText($existingCapture)) 'preserved' 'Existing screenshot remains unchanged'
    [IO.File]::Delete($existingCapture)
    $existingMetadata = [IO.Path]::ChangeExtension($existingCapture, '.json')
    [IO.File]::WriteAllText($existingMetadata, 'preserved metadata')
    $metadataResult = Invoke-ReviewProcess $pwsh $captureArguments
    Assert-Equal $metadataResult.exitCode 1 'Existing metadata is rejected'
    Assert-Equal ([IO.File]::ReadAllText($existingMetadata)) 'preserved metadata' 'Existing metadata remains unchanged'

    # A tiny owned TCP server proves ADB inspection does not need adb start-server.
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $port = $listener.LocalEndpoint.Port
    $server = [PowerShell]::Create()
    [void]$server.AddScript({
        param($Listener)
        $client = $Listener.AcceptTcpClient()
        try {
            $stream = $client.GetStream()
            $header = [byte[]]::new(4)
            $offset = 0
            while ($offset -lt 4) { $offset += $stream.Read($header, $offset, 4 - $offset) }
            $length = [Convert]::ToInt32([Text.Encoding]::ASCII.GetString($header), 16)
            $buffer = [byte[]]::new($length)
            $offset = 0
            while ($offset -lt $length) { $offset += $stream.Read($buffer, $offset, $length - $offset) }
            $response = [Text.Encoding]::ASCII.GetBytes('OKAY00040029')
            $stream.Write($response, 0, $response.Length)
        } finally { $client.Dispose() }
    }).AddArgument($listener)
    $pending = $server.BeginInvoke()
    try {
        $query = Invoke-ReviewAdbHostQuery 'host:version' $port
        Assert-Equal $query.status 'ready' 'ADB host protocol status'
        Assert-Equal $query.value '0029' 'ADB host protocol payload'
        [void]$server.EndInvoke($pending)
    } finally { $listener.Stop(); $server.Dispose() }
    Assert-Equal (Invoke-ReviewAdbHostQuery 'host:version' $port).status 'not-running' 'Absent ADB server never autostarts'

    foreach ($file in Get-ChildItem (Join-Path $PSScriptRoot '../../dev/*.ps1'), (Join-Path $PSScriptRoot '../*.ps1')) {
        $tokens = $null
        $parseErrors = $null
        [void][Management.Automation.Language.Parser]::ParseFile($file.FullName, [ref]$tokens, [ref]$parseErrors)
        Assert-Equal $parseErrors.Count 0 "Syntax: $($file.Name)"
    }
    [pscustomobject]@{ success = $true; assertions = $assertions } | ConvertTo-Json -Compress
} finally {
    $env:ANDROID_HOME = $savedAndroidHome
    $env:ANDROID_SDK_ROOT = $savedAndroidSdk
    $env:STREAMCORE_LOCAL_PROPERTIES = $savedLinkedProperties
    # Only files created by this test are removed; no recursive directory deletion.
    foreach ($file in @('local.properties', 'echo arguments.ps1', 'existing.png', 'existing.json', 'source.properties', 'package.xml', 'chrome.exe')) {
        $path = Join-Path $testRoot $file
        if ([IO.File]::Exists($path)) { [IO.File]::Delete($path) }
    }
    [IO.Directory]::Delete($testRoot)
}
