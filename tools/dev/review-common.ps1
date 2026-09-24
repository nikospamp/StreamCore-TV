# Shared by the Windows review commands. Dot-source only; no setup runs on import.
#requires -Version 7.0
Set-StrictMode -Version Latest

function Resolve-ReviewPath {
    param([string]$Path, [string]$Base)
    return [IO.Path]::GetFullPath($Path, $Base)
}

function Resolve-ReviewBaseUri {
    param([string]$BaseUrl)
    $uri = $null
    if (-not [Uri]::TryCreate($BaseUrl, [UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -ne 'http' -or
        $uri.Host -notin @('127.0.0.1', 'localhost', '[::1]') -or $uri.AbsolutePath -ne '/' -or
        $uri.UserInfo -or $uri.Query -or $uri.Fragment) {
        throw 'BaseUrl must be an HTTP loopback origin without credentials, path, query, or fragment.'
    }
    return $uri
}

function Get-ReviewJavaAvailability {
    param($PathProbe, $HomeProbe)
    if (($PathProbe -and $PathProbe.status -eq 'ready') -or ($HomeProbe -and $HomeProbe.status -eq 'ready')) { return 'ready' }
    if ($HomeProbe) { return $HomeProbe.status }
    if ($PathProbe) { return $PathProbe.status }
    return 'missing'
}

function Get-ReviewExecutableAvailability {
    param([string[]]$Candidates)
    foreach ($candidate in $Candidates) {
        if ($candidate -and [IO.File]::Exists($candidate)) { return [pscustomobject]@{ status = 'ready'; path = $candidate; launch = 'not-tested' } }
    }
    return [pscustomobject]@{ status = 'missing'; path = $null; launch = 'not-tested' }
}

function Get-ReviewFailureKind {
    param([string]$Message)
    if ($Message -match '(?i)access.+denied|permission denied|unauthorizedaccess|operation not permitted') { return 'access-denied' }
    if ($Message -match '(?i)timed? ?out|timeout') { return 'timeout' }
    if ($Message -match '(?i)cannot find|could not find|not found|no such file|does not exist') { return 'missing' }
    if ($Message -match '^(conflict|not-running|device-unauthorized):') { return $Matches[1] }
    return 'failed'
}

function Invoke-ReviewProcess {
    param([string]$FilePath, [string[]]$Arguments = @(), [int]$TimeoutSeconds = 15, [string]$WorkingDirectory)
    $process = [Diagnostics.Process]::new()
    try {
        $start = [Diagnostics.ProcessStartInfo]::new($FilePath)
        $start.UseShellExecute = $false
        $start.CreateNoWindow = $true
        $start.RedirectStandardOutput = $true
        $start.RedirectStandardError = $true
        if ($WorkingDirectory) { $start.WorkingDirectory = $WorkingDirectory }
        foreach ($argument in $Arguments) { $start.ArgumentList.Add($argument) }
        $process.StartInfo = $start
        [void]$process.Start()
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            # Only our client process is owned; never terminate an existing ADB server.
            $process.Kill()
            return [pscustomobject]@{ status = 'timeout'; exitCode = $null; stdout = ''; stderr = 'Process exceeded its deadline.' }
        }
        if (-not $stdout.Wait(1000) -or -not $stderr.Wait(1000)) {
            return [pscustomobject]@{ status = 'timeout'; exitCode = $process.ExitCode; stdout = ''; stderr = 'Process output did not close.' }
        }
        $status = if ($process.ExitCode -eq 0) { 'ready' } else { Get-ReviewFailureKind $stderr.Result }
        return [pscustomobject]@{ status = $status; exitCode = $process.ExitCode; stdout = $stdout.Result.Trim(); stderr = $stderr.Result.Trim() }
    } catch {
        return [pscustomobject]@{ status = (Get-ReviewFailureKind $_.Exception.Message); exitCode = $null; stdout = ''; stderr = $_.Exception.Message }
    } finally {
        $process.Dispose()
    }
}

function ConvertFrom-ReviewPropertyEscape {
    param([string]$Value)
    return [regex]::Replace($Value, '\\(u[0-9a-fA-F]{4}|.)', {
        param($match)
        $escaped = $match.Groups[1].Value
        switch -CaseSensitive ($escaped) {
            't' { return "`t" }; 'n' { return "`n" }; 'r' { return "`r" }; 'f' { return "`f" }
            default {
                if ($escaped -cmatch '^u[0-9a-fA-F]{4}$') { return [string][char][Convert]::ToInt32($escaped.Substring(1), 16) }
                return $escaped
            }
        }
    })
}

function Get-ReviewLocalProperties {
    param([string]$Path)
    $result = [ordered]@{ path = $Path; status = 'missing'; sdkDir = $null; tmdbReadAccessTokenPresent = $false; tmdbAccountIdPresent = $false }
    try {
        $pending = ''
        foreach ($physical in [IO.File]::ReadAllLines($Path)) {
            $line = $pending + $physical.TrimStart()
            if (([regex]::Match($line, '\\+$').Length % 2) -eq 1) { $pending = $line.Substring(0, $line.Length - 1); continue }
            $pending = ''
            if ($line -match '^\s*[#!]' -or [string]::IsNullOrWhiteSpace($line)) { continue }
            $match = [regex]::Match($line, '^(?<key>(?:\\.|[^\s:=\\])+)(?:\s*[:=]\s*|\s+)(?<value>.*)$')
            if (-not $match.Success) { continue }
            $key = ConvertFrom-ReviewPropertyEscape $match.Groups['key'].Value
            switch -CaseSensitive ($key) {
                'sdk.dir' { $result.sdkDir = ConvertFrom-ReviewPropertyEscape $match.Groups['value'].Value }
                'tmdbReadAccessToken' { $result.tmdbReadAccessTokenPresent = -not [string]::IsNullOrWhiteSpace($match.Groups['value'].Value) }
                'tmdbAccountId' { $result.tmdbAccountIdPresent = -not [string]::IsNullOrWhiteSpace($match.Groups['value'].Value) }
            }
        }
        $result.status = 'ready'
    } catch { $result.status = Get-ReviewFailureKind $_.Exception.Message }
    return [pscustomobject]$result
}

function Get-ReviewAndroidEnvironment {
    param([string]$Root, [string]$SdkRoot, [string]$LocalPropertiesPath)
    $propertiesPath = if ($LocalPropertiesPath) { $LocalPropertiesPath } elseif ($env:STREAMCORE_LOCAL_PROPERTIES) { $env:STREAMCORE_LOCAL_PROPERTIES } else { 'local.properties' }
    $properties = Get-ReviewLocalProperties (Resolve-ReviewPath $propertiesPath $Root)
    $candidates = [Collections.Generic.List[object]]::new()
    $pathAdb = Get-Command adb -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    foreach ($candidate in @(
        @{ source = 'explicit'; path = $SdkRoot },
        @{ source = 'sdk.dir'; path = $properties.sdkDir },
        @{ source = 'ANDROID_HOME'; path = $env:ANDROID_HOME },
        @{ source = 'ANDROID_SDK_ROOT'; path = $env:ANDROID_SDK_ROOT },
        @{ source = 'PATH adb'; path = $(if ($pathAdb) { Split-Path (Split-Path $pathAdb.Source -Parent) -Parent }) }
    )) {
        if ($candidate.path) { $candidates.Add([pscustomobject]@{ source = $candidate.source; path = (Resolve-ReviewPath $candidate.path $Root) }) }
    }
    $selected = if ($candidates.Count) { $candidates[0] } else { $null }
    $adbName = if ($IsWindows) { 'adb.exe' } else { 'adb' }
    $adb = if ($selected) { Join-Path $selected.path "platform-tools/$adbName" } else { $null }
    return [pscustomobject]@{
        localProperties = $properties
        sdkRoot = $(if ($selected) { $selected.path } else { $null })
        source = $(if ($selected) { $selected.source } else { $null })
        adb = $adb
        candidates = @($candidates.ToArray())
        conflict = (@($candidates | ForEach-Object path | Select-Object -Unique).Count -gt 1)
    }
}

function Get-ReviewPlatformApiLevel {
    param([string]$PlatformDirectory)
    $properties = Join-Path $PlatformDirectory 'source.properties'
    if (Test-Path -LiteralPath $properties) {
        $match = [regex]::Match([IO.File]::ReadAllText($properties), '(?m)^AndroidVersion\.ApiLevel\s*=\s*(\d+)(?:\.\d+)?\s*$')
        if ($match.Success) { return [int]$match.Groups[1].Value }
    }
    $packageXml = Join-Path $PlatformDirectory 'package.xml'
    if (Test-Path -LiteralPath $packageXml) {
        $match = [regex]::Match([IO.File]::ReadAllText($packageXml), '<(?:\w+:)?api-level>\s*(\d+)(?:\.\d+)?\s*</')
        if ($match.Success) { return [int]$match.Groups[1].Value }
    }
    return $null
}

function Get-ReviewDeviceReadiness {
    param([string]$State, $BootProbe, $PackageProbe)
    $result = [ordered]@{ status = 'probe-failed'; bootComplete = $null; packageService = $null; errors = @() }
    if ($State -eq 'offline') {
        $result.status = 'offline'
        $result.errors += @{ code = 'device-offline'; message = 'ADB lists this device as offline.' }
    } elseif ($State -eq 'unauthorized') {
        $result.status = 'unauthorized'
        $result.errors += @{ code = 'device-unauthorized'; message = 'Accept the device debugging prompt, then retry.' }
    } elseif ($State -ne 'device') {
        $result.errors += @{ code = 'device-state-unusable'; message = 'ADB does not report a usable Android device transport.' }
    } else {
        foreach ($probe in @(@{ name = 'boot'; value = $BootProbe }, @{ name = 'package-service'; value = $PackageProbe })) {
            if (-not $probe.value -or $probe.value.status -ne 'ready') {
                # Only classifications are returned. Raw command output may contain device-specific data.
                $kind = if ($probe.value -and $probe.value.status -in @('timeout', 'access-denied', 'missing', 'failed')) { $probe.value.status } else { 'failed' }
                $result.errors += @{ code = "$($probe.name)-probe-$kind"; message = "The $($probe.name) readiness probe did not complete successfully." }
            }
        }
        if ($result.errors.Count -eq 0) {
            $result.bootComplete = $BootProbe.stdout -eq '1'
            $result.packageService = $PackageProbe.stdout -match ': found'
            if ($result.bootComplete -and $result.packageService) { $result.status = 'ready' }
            else {
                $result.status = 'booting'
                $result.errors += @{ code = 'device-not-ready'; message = 'Android boot completion and the package service are not both ready.' }
            }
        }
    }
    return [pscustomobject]$result
}

function Get-ReviewDeviceCheckSummary {
    param([string]$ServerStatus, [string]$InventoryStatus, [object[]]$Devices = @(), [string]$Serial)
    $result = [ordered]@{ status = 'ready'; readyDeviceCount = 0; code = $null; message = $null }
    if ($ServerStatus -ne 'ready') {
        $result.status = 'adb-server-unavailable'
        $result.code = "adb-server-$ServerStatus"
        $result.message = 'No compatible ADB server was verified. Device readiness is unknown.'
    } elseif ($InventoryStatus -ne 'ready') {
        $result.status = 'inventory-failed'
        $result.code = "devices-$InventoryStatus"
        $result.message = 'Unable to read the ADB device inventory. Device readiness is unknown.'
    } else {
        $result.readyDeviceCount = @($Devices | Where-Object status -eq 'ready').Count
        $selected = @($Devices | Where-Object serial -CEQ $Serial)
        if ($Serial -and $selected.Count -eq 0) {
            $result.status = 'selected-missing'
            $result.code = 'selected-device-missing'
            $result.message = 'The selected serial is not present in the ADB inventory.'
        } elseif ($Serial -and $selected[0].status -ne 'ready') {
            $result.status = 'selected-not-ready'
            $result.code = 'selected-device-not-ready'
            $result.message = 'The selected device is not ready. See its structured readiness errors.'
        } elseif ($result.readyDeviceCount -eq 0) {
            $result.status = 'no-ready-devices'
            $result.code = 'devices-not-ready'
            $result.message = 'No usable devices were found. No emulator was launched. See the device readiness results.'
        }
    }
    return [pscustomobject]$result
}

function Read-ReviewSocketBytes {
    param([IO.Stream]$Stream, [int]$Count)
    $buffer = [byte[]]::new($Count)
    $offset = 0
    while ($offset -lt $Count) {
        $read = $Stream.Read($buffer, $offset, $Count - $offset)
        if ($read -eq 0) { throw 'ADB server closed its response.' }
        $offset += $read
    }
    return ,$buffer
}

function Invoke-ReviewAdbHostQuery {
    param([string]$Query, [int]$Port = 5037)
    $client = [Net.Sockets.TcpClient]::new()
    try {
        $connect = $client.ConnectAsync('127.0.0.1', $Port)
        if (-not $connect.Wait(5000)) { return [pscustomobject]@{ status = 'timeout'; value = $null } }
        $stream = $client.GetStream()
        $stream.ReadTimeout = 2000
        $stream.WriteTimeout = 2000
        $bytes = [Text.Encoding]::UTF8.GetBytes($Query)
        $header = [Text.Encoding]::ASCII.GetBytes($bytes.Length.ToString('x4'))
        $stream.Write($header, 0, 4)
        $stream.Write($bytes, 0, $bytes.Length)
        $response = [Text.Encoding]::ASCII.GetString((Read-ReviewSocketBytes $stream 4))
        if ($response -notin @('OKAY', 'FAIL')) { throw 'Port is occupied by an unrecognized service.' }
        $length = [Convert]::ToInt32([Text.Encoding]::ASCII.GetString((Read-ReviewSocketBytes $stream 4)), 16)
        if ($length -gt 65535) { throw 'ADB response is too large.' }
        $value = [Text.Encoding]::UTF8.GetString((Read-ReviewSocketBytes $stream $length))
        return [pscustomobject]@{ status = $(if ($response -eq 'OKAY') { 'ready' } else { 'failed' }); value = $value }
    } catch {
        $cause = $_.Exception.GetBaseException()
        $kind = if ($cause -is [Net.Sockets.SocketException] -and $cause.SocketErrorCode -eq [Net.Sockets.SocketError]::ConnectionRefused) { 'not-running' } else { Get-ReviewFailureKind $_.Exception.Message }
        return [pscustomobject]@{ status = $kind; value = $null }
    } finally { $client.Dispose() }
}

function Get-ReviewAdbConnection {
    param([string]$Adb, [int]$Port = 5037, [switch]$StartAdb)
    $version = Invoke-ReviewProcess $Adb @('version')
    if ($version.status -ne 'ready') { return [pscustomobject]@{ status = $version.status; message = $version.stderr } }
    $match = [regex]::Match($version.stdout, 'Android Debug Bridge version \d+\.\d+\.(\d+)')
    if (-not $match.Success) { return [pscustomobject]@{ status = 'failed'; message = 'Cannot determine the selected ADB protocol version.' } }
    $hostVersion = Invoke-ReviewAdbHostQuery 'host:version' $Port
    if ($hostVersion.status -eq 'not-running' -and $StartAdb) {
        $started = Invoke-ReviewProcess $Adb @('-L', "tcp:localhost:$Port", 'start-server')
        if ($started.status -ne 'ready') { return [pscustomobject]@{ status = $started.status; message = 'Unable to start the selected ADB server.' } }
        $hostVersion = Invoke-ReviewAdbHostQuery 'host:version' $Port
    }
    if ($hostVersion.status -ne 'ready') { return [pscustomobject]@{ status = $hostVersion.status; message = 'No usable ADB server. Start it explicitly with -StartAdb, or inspect the occupied port.' } }
    if ([Convert]::ToInt32($hostVersion.value, 16) -ne [int]$match.Groups[1].Value) {
        return [pscustomobject]@{ status = 'conflict'; message = 'Selected ADB and the running server use different protocols. No server was stopped or replaced.' }
    }
    return [pscustomobject]@{ status = 'ready'; message = 'Existing server is compatible.' }
}
