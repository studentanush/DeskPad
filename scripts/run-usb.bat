@echo off
setlocal
cd /d "%~dp0"
call config.bat
call setup-adb.bat
if errorlevel 1 (
  echo.
  echo [ERROR] Could not configure USB connection.
  pause
  exit /b 1
)

echo.
echo Starting Desktop Touchpad Server for USB...
echo Now open USB Touchpad on your phone and tap CONNECT (Host: 127.0.0.1, Port: %TOUCHPAD_PORT%)
echo.
call start-server.bat
endlocal
