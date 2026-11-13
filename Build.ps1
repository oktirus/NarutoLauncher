param([string]$AndroidSdk = "$env:LOCALAPPDATA/Android/Sdk")
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath "$AndroidSdk/platforms/android-34/android.jar")) { throw 'Android SDK platform 34 is required.' }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'JDK 17 is required.' }
$env:ANDROID_HOME = $AndroidSdk
$stageRoot = Join-Path $env:LOCALAPPDATA ('KonohaBuild/' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
$buildRoot = Join-Path $stageRoot 'android'
New-Item -ItemType Directory -Path $buildRoot -Force | Out-Null
Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'android') -Force | Where-Object Name -NotIn @('.gradle','local.properties') | ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination $buildRoot -Recurse }
& python -X utf8 (Join-Path $PSScriptRoot 'tools/bundle_android.py') (Join-Path $buildRoot 'app/src/main/assets/site')
if ($LASTEXITCODE -ne 0) { throw 'Asset bundle failed.' }
Write-Output "Build staging: $buildRoot"
Push-Location $buildRoot
try {
    & ./gradlew.bat --no-daemon lintDebug assembleDebug *> (Join-Path $PSScriptRoot 'revisions/v26/verification/android-build.log')
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed. See revisions/v26/verification/android-build.log.' }
    $apk = Join-Path $buildRoot 'app/build/outputs/apk/debug/app-debug.apk'
    if (-not (Test-Path -LiteralPath $apk)) { throw 'Build did not produce an APK.' }
    $output = Join-Path $PSScriptRoot 'releases/Konoha-1.0.0-v26-debug.apk'
    Copy-Item -LiteralPath $apk -Destination $output
    foreach ($extension in @('html','xml')) {
        Copy-Item -LiteralPath (Join-Path $buildRoot "app/build/reports/lint-results-debug.$extension") -Destination (Join-Path $PSScriptRoot "revisions/v26/verification/android-lint.$extension")
    }
    [ordered]@{file=[IO.Path]::GetFileName($output);bytes=(Get-Item -LiteralPath $output).Length;sha256=(Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash;package='ua.konoha.tv';versionCode=26;versionName='1.0.0-v26';signing='local Android debug key; live-test build';debuggable=$true;build_staging=$buildRoot} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'releases/build-manifest.json') -Encoding utf8
    Write-Output "Built: $output"
} finally { Pop-Location }
