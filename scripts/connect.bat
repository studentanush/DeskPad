@echo off
setlocal
call "%~dp0config.bat"
call "%~dp0setup-adb.bat"
if errorlevel 1 exit /b 1
echo.
echo Next steps:
echo   1. Keep scripts\start-server.bat running
echo   2. Open USB Touchpad on your phone and tap Connect (127.0.0.1:%TOUCHPAD_PORT%)
endlocal
