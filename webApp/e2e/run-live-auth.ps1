[CmdletBinding()]
param(
    [Parameter()]
    [string] $CredentialsPath,

    [Parameter()]
    [string] $LocalPropertiesPath
)

$ErrorActionPreference = "Stop"
$liveKeys = @(
    "STREAMCORE_LIVE_TMDB_BASE_URL",
    "STREAMCORE_LIVE_TMDB_READ_ACCESS_TOKEN",
    "STREAMCORE_LIVE_TMDB_ACCOUNT_ID",
    "STREAMCORE_LIVE_TMDB_USERNAME",
    "STREAMCORE_LIVE_TMDB_PASSWORD"
)

function Read-LiteralProperties {
    param([string] $Path)

    $properties = @{}
    if ([string]::IsNullOrWhiteSpace($Path) -or -not [System.IO.File]::Exists($Path)) {
        return $properties
    }

    foreach ($line in [System.IO.File]::ReadAllLines($Path)) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#") -or $trimmed.StartsWith("!")) {
            continue
        }
        $separator = $line.IndexOf("=")
        if ($separator -lt 1) {
            continue
        }
        $key = $line.Substring(0, $separator).Trim()
        $value = $line.Substring($separator + 1)
        $properties[$key] = $value
    }
    return $properties
}

function Test-Configured {
    param([string] $Value)
    return -not [string]::IsNullOrWhiteSpace($Value)
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
    STREAMCORE_LIVE_TMDB_BASE_URL = if (Test-Configured $localProperties["tmdbBaseUrl"]) {
        $localProperties["tmdbBaseUrl"]
    } else {
        "https://api.themoviedb.org/"
    }
    STREAMCORE_LIVE_TMDB_READ_ACCESS_TOKEN = $localProperties["tmdbReadAccessToken"]
    STREAMCORE_LIVE_TMDB_ACCOUNT_ID = $localProperties["tmdbAccountId"]
    STREAMCORE_LIVE_TMDB_USERNAME = $credentialProperties["tmdbUsername"]
    STREAMCORE_LIVE_TMDB_PASSWORD = $credentialProperties["tmdbPassword"]
}

Write-Output "credentialsFileConfigured=$credentialsFileConfigured"
Write-Output "localPropertiesFileConfigured=$localPropertiesFileConfigured"
foreach ($key in $liveKeys) {
    Write-Output "$($key)Configured=$(Test-Configured $liveEnvironment[$key])"
}

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = "npm.cmd"
$startInfo.ArgumentList.Add("run")
$startInfo.ArgumentList.Add("test:live-auth")
$startInfo.WorkingDirectory = $PSScriptRoot
$startInfo.UseShellExecute = $false

foreach ($key in $liveKeys) {
    $startInfo.Environment.Remove($key) | Out-Null
    if (Test-Configured $liveEnvironment[$key]) {
        $startInfo.Environment[$key] = $liveEnvironment[$key]
    }
}

try {
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
