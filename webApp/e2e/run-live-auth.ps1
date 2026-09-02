[CmdletBinding()]
param(
    [Parameter()]
    [string] $CredentialsPath,

    [Parameter()]
    [string] $LocalPropertiesPath,

    [Parameter()]
    [switch] $PreflightOnly,

    [Parameter()]
    [switch] $ListOnly
)

$ErrorActionPreference = "Stop"
$liveKeys = @(
    "STREAMCORE_LIVE_TMDB_BASE_URL",
    "STREAMCORE_LIVE_TMDB_READ_ACCESS_TOKEN",
    "STREAMCORE_LIVE_TMDB_ACCOUNT_ID",
    "STREAMCORE_LIVE_TMDB_USERNAME",
    "STREAMCORE_LIVE_TMDB_PASSWORD"
)

function Test-Configured {
    param([AllowNull()][string] $Value)
    return -not [string]::IsNullOrWhiteSpace($Value)
}

function Read-LiteralProperties {
    param([AllowNull()][string] $Path)

    $properties = [System.Collections.Generic.Dictionary[string, string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )
    if (-not (Test-Configured $Path) -or -not [System.IO.File]::Exists($Path)) {
        return $properties
    }

    foreach ($line in [System.IO.File]::ReadAllLines($Path)) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#") -or $trimmed.StartsWith("!")) {
            continue
        }
        $equalsIndex = $line.IndexOf("=")
        $colonIndex = $line.IndexOf(":")
        $separatorIndexes = @($equalsIndex, $colonIndex) | Where-Object { $_ -ge 1 }
        if ($separatorIndexes.Count -eq 0) {
            continue
        }
        $separator = ($separatorIndexes | Measure-Object -Minimum).Minimum
        $key = $line.Substring(0, $separator).Trim()
        $value = $line.Substring($separator + 1).Trim()
        if ($properties.ContainsKey($key)) {
            throw "Duplicate property key is not allowed."
        }
        if (-not (Test-Configured $value)) {
            throw "Recognized property values must not be blank."
        }
        $properties.Add($key, $value)
    }
    return $properties
}

function Resolve-RecognizedValue {
    param(
        [System.Collections.Generic.Dictionary[string, string]] $Properties,
        [string[]] $Aliases
    )

    $matches = @($Aliases | Where-Object { $Properties.ContainsKey($_) })
    if ($matches.Count -gt 1) {
        throw "Duplicate aliases for one required property are not allowed."
    }
    if ($matches.Count -eq 0) {
        return $null
    }
    $value = $Properties[$matches[0]]
    if (-not (Test-Configured $value)) {
        throw "Recognized property values must not be blank."
    }
    return $value
}

$resolvedCredentialsPath = if (Test-Configured $CredentialsPath) {
    $CredentialsPath
} else {
    [System.Environment]::GetEnvironmentVariable("STREAMCORE_TMDB_CREDENTIALS_FILE")
}
$resolvedLocalPropertiesPath = if (Test-Configured $LocalPropertiesPath) {
    $LocalPropertiesPath
} else {
    [System.Environment]::GetEnvironmentVariable("STREAMCORE_LOCAL_PROPERTIES")
}

$credentialProperties = Read-LiteralProperties $resolvedCredentialsPath
$localProperties = Read-LiteralProperties $resolvedLocalPropertiesPath
$credentialsFileConfigured = (Test-Configured $resolvedCredentialsPath) -and
    [System.IO.File]::Exists($resolvedCredentialsPath)
$localPropertiesFileConfigured = (Test-Configured $resolvedLocalPropertiesPath) -and
    [System.IO.File]::Exists($resolvedLocalPropertiesPath)
$liveEnvironment = @{
    STREAMCORE_LIVE_TMDB_BASE_URL = Resolve-RecognizedValue $localProperties @("tmdbBaseUrl")
    STREAMCORE_LIVE_TMDB_READ_ACCESS_TOKEN = Resolve-RecognizedValue $localProperties @("tmdbReadAccessToken")
    STREAMCORE_LIVE_TMDB_ACCOUNT_ID = Resolve-RecognizedValue $localProperties @("tmdbAccountId")
    STREAMCORE_LIVE_TMDB_USERNAME = Resolve-RecognizedValue $credentialProperties @(
        "username", "identifier", "email", "tmdbUsername", "tmdbIdentifier", "tmdbEmail"
    )
    STREAMCORE_LIVE_TMDB_PASSWORD = Resolve-RecognizedValue $credentialProperties @("password", "tmdbPassword")
}
if (-not (Test-Configured $liveEnvironment["STREAMCORE_LIVE_TMDB_BASE_URL"])) {
    $liveEnvironment["STREAMCORE_LIVE_TMDB_BASE_URL"] = "https://api.themoviedb.org/"
}

Write-Output "credentialsFileConfigured=$credentialsFileConfigured"
Write-Output "localPropertiesFileConfigured=$localPropertiesFileConfigured"
foreach ($key in $liveKeys) {
    Write-Output "$($key)Configured=$(Test-Configured $liveEnvironment[$key])"
}

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$nodeCommand = Get-Command node.exe -ErrorAction Stop
$playwrightCli = Join-Path $PSScriptRoot "node_modules/@playwright/test/cli.js"
if (-not [System.IO.File]::Exists($playwrightCli)) {
    throw "Local Playwright CLI is missing. Run npm ci first."
}
$startInfo.FileName = $nodeCommand.Source
$startInfo.ArgumentList.Add($playwrightCli)
$startInfo.ArgumentList.Add("test")
$startInfo.ArgumentList.Add("--config")
$startInfo.ArgumentList.Add("playwright.live.config.ts")
if ($ListOnly) {
    $startInfo.ArgumentList.Add("--list")
}
$startInfo.WorkingDirectory = $PSScriptRoot
$startInfo.UseShellExecute = $false

try {
foreach ($key in $liveKeys) {
    $startInfo.Environment.Remove($key) | Out-Null
    if (Test-Configured $liveEnvironment[$key]) {
        $startInfo.Environment[$key] = $liveEnvironment[$key]
    }
}
$childEnvironmentMatched = $true
foreach ($key in $liveKeys) {
    $expected = if (Test-Configured $liveEnvironment[$key]) { $liveEnvironment[$key] } else { $null }
    $actual = if ($startInfo.Environment.ContainsKey($key)) { $startInfo.Environment[$key] } else { $null }
    if ($actual -cne $expected) {
        $childEnvironmentMatched = $false
    }
}
Write-Output "childEnvironmentMatched=$childEnvironmentMatched"
if (-not $childEnvironmentMatched) {
    throw "Child environment validation failed."
}

    if ($PreflightOnly) {
        exit 0
    }
    $process = [System.Diagnostics.Process]::Start($startInfo)
    $process.WaitForExit()
    exit $process.ExitCode
} finally {
    foreach ($key in $liveKeys) {
        $startInfo.Environment.Remove($key) | Out-Null
        if ($liveEnvironment.ContainsKey($key)) {
            $liveEnvironment[$key] = ""
        }
    }
    $credentialProperties.Clear()
    $localProperties.Clear()
    $liveEnvironment.Clear()
}
