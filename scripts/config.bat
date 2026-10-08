@echo off
rem Configure ADB path if not in PATH (auto-detect in Android SDK if available)
if not defined ADB_PATH (
  where adb >nul 2>&1
  if errorlevel 1 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
      set "ADB_PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    ) else (
      set "ADB_PATH=adb"
    )
  ) else (
    set "ADB_PATH=adb"
  )
)

rem Default touchpad server port (must match adb reverse and Android app)
if not defined TOUCHPAD_PORT set "TOUCHPAD_PORT=5000"

rem Optional: path to desktop server fat JAR (built by gradlew)
if not defined TOUCHPAD_JAR set "TOUCHPAD_JAR=%~dp0..\desktop-server\build\libs\desktop-server-1.0.0-all.jar"
rem Build without Gradle: powershell -ExecutionPolicy Bypass -File scripts\build-server-jar.ps1
if not defined GRADLE_HOME set "GRADLE_HOME=%~dp0..\.gradle-dist\gradle-8.10.2"
