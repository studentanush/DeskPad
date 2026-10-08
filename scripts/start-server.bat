@echo off
setlocal
call "%~dp0config.bat"

if exist "%TOUCHPAD_JAR%" (
  echo Starting touchpad server from JAR...
  java -Dtouchpad.port=%TOUCHPAD_PORT% -jar "%TOUCHPAD_JAR%"
) else (
  echo JAR not found. Building and running via Gradle...
  pushd "%~dp0.."
  call gradlew.bat :desktop-server:run --args="" -Ptouchpad.port=%TOUCHPAD_PORT%
  popd
)
endlocal
