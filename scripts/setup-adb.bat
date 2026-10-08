@echo off
setlocal
call "%~dp0config.bat"

if exist "%ADB_PATH%" (
  for %%I in ("%ADB_PATH%") do set "PATH=%%~dpI;%PATH%"
)

where adb >nul 2>&1
if errorlevel 1 (
  echo ERROR: ADB not found. Install Android Platform Tools and set ADB_PATH in scripts\config.bat
  exit /b 1
)

echo Checking devices...
adb devices
adb devices | findstr /r /c:"[0-9a-zA-Z].*device$" >nul
if errorlevel 1 (
  echo ERROR: No authorized device found.
  echo Ensure USB debugging is turned ON and tap 'Allow' on your phone.
  exit /b 1
)

echo.
echo Setting up adb reverse (phone 127.0.0.1:%TOUCHPAD_PORT% -^> PC 127.0.0.1:%TOUCHPAD_PORT%)...
adb reverse tcp:%TOUCHPAD_PORT% tcp:%TOUCHPAD_PORT%
if errorlevel 1 (
  echo ERROR: adb reverse failed.
  exit /b 1
)

echo.
echo ===============================================================
echo  SUCCESS: USB Reverse Connection Active!
echo  Phone 127.0.0.1:%TOUCHPAD_PORT% is linked to PC 127.0.0.1:%TOUCHPAD_PORT%
echo  Connect in the Android app: Host: 127.0.0.1  Port: %TOUCHPAD_PORT%
echo ===============================================================
endlocal
