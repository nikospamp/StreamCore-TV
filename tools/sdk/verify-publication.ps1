param(
    [switch]$SkipPublish,
    [string]$NodeExecutable,
    [string]$AndroidSdkRoot,
    [string]$AndroidSerial,
    [string]$RepositoryPath = 'build/sdk-repository'
)
$ErrorActionPreference = 'Stop'
$taskRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$taskWrapper = Join-Path $taskRoot 'gradlew.bat'
$taskHeadlessModules = @(':sdk:model', ':sdk:api', ':sdk:runtime', ':sdk:testing', ':sdk:providers:tmdb', ':sdk:providers:clientB')
$taskUiModules = @(':sdk:ui', ':sdk:providers:tmdb:ui', ':sdk:providers:clientB:ui')
$taskModules = $taskHeadlessModules + $taskUiModules
$taskRepository = [System.IO.Path]::GetFullPath($RepositoryPath, $taskRoot)
$taskGradleProperties = @("-PsdkRepository=$taskRepository")
if ($NodeExecutable) { $taskGradleProperties += "-PstreamcoreNodeExecutable=$NodeExecutable" }
$taskPreviousAndroidHome = $env:ANDROID_HOME
$taskPreviousAndroidSerial = $env:ANDROID_SERIAL
if ($AndroidSdkRoot) { $env:ANDROID_HOME = $AndroidSdkRoot }
if ($AndroidSerial) { $env:ANDROID_SERIAL = $AndroidSerial }
Push-Location $taskRoot
try {
    if (-not $SkipPublish) {
        $taskPublish = @($taskModules | ForEach-Object { "${_}:publishAllPublicationsToSdkStagingRepository" })
        $taskGates = @($taskHeadlessModules | ForEach-Object { "${_}:verifySdkHeadlessDependencies" })
        $taskGates += @($taskUiModules | ForEach-Object { "${_}:verifySdkUiDependencies" })
        & $taskWrapper @taskPublish @taskGates @taskGradleProperties --console=plain --no-configuration-cache
        if ($LASTEXITCODE -ne 0) { throw 'SDK staging publication or dependency boundary check failed.' }
    }
    python tools/sdk/verify_sdk.py --repository $taskRepository
    if ($LASTEXITCODE -ne 0) { throw 'SDK source boundary or publication metadata check failed.' }
    & $taskWrapper -p samples/sdk-consumer verifyCoordinateOnlyConsumption :androidApp:assembleDebug :androidApp:testDebugUnitTest :headless:wasmJsNodeTest @taskGradleProperties --refresh-dependencies --console=plain --no-configuration-cache
    if ($LASTEXITCODE -ne 0) { throw 'Independent published-coordinate consumer failed.' }
    & $taskWrapper -p samples/sdk-consumer :uiAndroidApp:assembleDebug :uiAndroidApp:assembleDebugAndroidTest :uiWasm:wasmJsBrowserTest @taskGradleProperties --refresh-dependencies --console=plain --no-configuration-cache
    if ($LASTEXITCODE -ne 0) { throw 'Independent optional SDK UI resource consumer failed.' }
    if ($AndroidSerial) {
        & $taskWrapper -p samples/sdk-consumer :uiAndroidApp:connectedDebugAndroidTest @taskGradleProperties --console=plain --no-configuration-cache
        if ($LASTEXITCODE -ne 0) { throw 'Published Android SDK UI resource execution failed.' }
    } else {
        Write-Output 'Android UI resource test APK built; device execution not run. Pass -AndroidSerial to execute it.'
    }
} finally {
    Pop-Location
    if ($AndroidSdkRoot) { $env:ANDROID_HOME = $taskPreviousAndroidHome }
    if ($AndroidSerial) { $env:ANDROID_SERIAL = $taskPreviousAndroidSerial }
}
