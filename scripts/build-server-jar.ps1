# Build runnable desktop server JAR without Gradle.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$OutMain = Join-Path $Root "build\classes\main"
if (-not (Test-Path (Join-Path $OutMain "com\cnl\touchpad\server\TouchpadServerMain.class"))) {
    & (Join-Path $PSScriptRoot "compile-and-test.ps1")
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
$JarDir = Join-Path $Root "desktop-server\build\libs"
New-Item -ItemType Directory -Force -Path $JarDir | Out-Null
$Jar = Join-Path $JarDir "desktop-server-1.0.0-all.jar"

if (Test-Path $Jar) { Remove-Item $Jar -Force }
& jar cfe $Jar com.cnl.touchpad.server.TouchpadServerMain -C $OutMain .
Write-Host "Created $Jar"
