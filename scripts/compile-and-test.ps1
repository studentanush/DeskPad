# Offline-friendly build + unit tests (no Gradle required).
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Junit = Join-Path $Root ".tools\junit-platform-console-standalone-1.10.2.jar"
if (-not (Test-Path $Junit)) {
    Write-Error "Missing JUnit jar at $Junit"
}

$OutMain = Join-Path $Root "build\classes\main"
$OutTest = Join-Path $Root "build\classes\test"
New-Item -ItemType Directory -Force -Path $OutMain, $OutTest | Out-Null

function Compile-Tree($srcDir, $dest, $extraCp) {
    $files = @(Get-ChildItem -Path $srcDir -Filter *.java -Recurse)
    if ($files.Count -eq 0) { return }
    $cp = $OutMain
    if ($extraCp) { $cp = "$cp;$extraCp" }
    $args = @("-encoding", "UTF-8", "-d", $dest, "-cp", $cp) + ($files | ForEach-Object { $_.FullName })
    & javac @args
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "Compiling protocol + server..."
Compile-Tree (Join-Path $Root "touchpad-protocol\src\main\java") $OutMain $null
Compile-Tree (Join-Path $Root "desktop-server\src\main\java") $OutMain $null

Write-Host "Compiling tests..."
Compile-Tree (Join-Path $Root "touchpad-protocol\src\test\java") $OutTest $Junit
Compile-Tree (Join-Path $Root "desktop-server\src\test\java") $OutTest $Junit

$gestureMain = Join-Path $Root "android-client\app\src\main\java\com\cnl\touchpad\android\TouchpadGestureHandler.java"
$gestureTest = Join-Path $Root "android-client\app\src\test\java\com\cnl\touchpad\android\TouchpadGestureHandlerTest.java"
$testCp = "$OutMain;$OutTest;$Junit"
& javac -encoding UTF-8 -d $OutTest -cp $testCp $gestureMain $gestureTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Running JUnit..."
$runCp = "$OutMain;$OutTest"
& java -jar $Junit --class-path $runCp --scan-class-path
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Packaging desktop server jar..."
$jarDir = Join-Path $Root "desktop-server\build\libs"
New-Item -ItemType Directory -Force -Path $jarDir | Out-Null
$jarPath = Join-Path $jarDir "desktop-server-1.0.0-all.jar"
& jar --create --file $jarPath --main-class com.cnl.touchpad.server.TouchpadServerMain -C $OutMain .
Write-Host "Server JAR built successfully: $jarPath"
exit 0
