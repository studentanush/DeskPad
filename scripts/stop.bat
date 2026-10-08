@echo off
setlocal
call "%~dp0config.bat"
echo Removing adb reverse for port %TOUCHPAD_PORT%...
"%ADB_PATH%" reverse --remove tcp:%TOUCHPAD_PORT% 2>nul
echo Done. Stop the Java server with Ctrl+C in its window.
endlocal
